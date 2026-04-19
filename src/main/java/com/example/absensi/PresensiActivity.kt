package com.example.absensi

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.ConfigResponse
import com.example.absensi.network.LoginResponse
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PresensiActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var btnCapture: View
    private lateinit var tvTitlePresensi: TextView
    private lateinit var tvWelcomeName: TextView
    private lateinit var tvRole: TextView

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var currentLat: Double = 0.0
    private var currentLon: Double = 0.0
    private var jenisAbsen: String = "masuk"

    // KOORDINAT KANTOR (Pastikan ini sesuai lokasi Anda saat ini jika memakai HP asli)
    private var officeLat = -7.7509239
    private var officeLon = 111.9946412
    private var maxRadius = 100.0 // Saya naikkan ke 100 meter agar lebih toleran saat testing
    private var isOfficeConfigReady = false

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.CAMERA] == true && permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                startCamera()
                getLocation()
            } else {
                Toast.makeText(this, "Izin Kamera & Lokasi wajib diberikan!", Toast.LENGTH_LONG).show()
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_presensi)

        // 1. Inisialisasi View
        viewFinder = findViewById(R.id.viewFinder)
        btnCapture = findViewById(R.id.btnCapture)
        tvTitlePresensi = findViewById(R.id.tvTitlePresensi)
        tvWelcomeName = findViewById(R.id.tvWelcomeName)
        tvRole = findViewById(R.id.tvRole)

        // 2. Ambil Data User (Sama seperti Dashboard)
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val namaLengkap = sharedPref.getString("NAMA_LENGKAP", "Karyawan")
        tvWelcomeName.text = "Selamat Datang, $namaLengkap"
        tvRole.text = "Karyawan"

        jenisAbsen = intent.getStringExtra("JENIS_ABSEN") ?: "masuk"
        tvTitlePresensi.text = "Presensi ${jenisAbsen.uppercase()}"

        cameraExecutor = Executors.newSingleThreadExecutor()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setCaptureEnabled(false)
        fetchOfficeConfig()

        if (allPermissionsGranted()) {
            startCamera()
            getLocation()
        } else {
            requestPermissionLauncher.launch(arrayOf(
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }

        btnCapture.setOnClickListener {
            if (!isOfficeConfigReady) {
                Toast.makeText(this, "Konfigurasi lokasi kantor belum tersedia.", Toast.LENGTH_SHORT).show()
            } else if (currentLat == 0.0) {
                Toast.makeText(this, "Mencari lokasi GPS... Tunggu sampai 'Lokasi Terkunci' muncul.", Toast.LENGTH_SHORT).show()
                getLocation()
            } else if (checkLocationStatus(currentLat, currentLon)) {
                takePhoto()
            }
        }
    }

    private fun fetchOfficeConfig() {
        ApiConfig.getApiService().getConfigPresensi()
            .enqueue(object : Callback<ConfigResponse> {
                override fun onResponse(call: Call<ConfigResponse>, response: Response<ConfigResponse>) {
                    val config = response.body()?.data
                    val latFromConfig = config?.officeLat
                    val lonFromConfig = config?.officeLon
                    val radiusFromConfig = config?.maxRadius

                    if (!response.isSuccessful) {
                        disableCaptureForConfigFailure("Server konfigurasi mengembalikan error (${response.code()}).")
                        return
                    }

                    if (response.body()?.success != true) {
                        disableCaptureForConfigFailure(response.body()?.message ?: "Konfigurasi lokasi kantor ditolak server.")
                        return
                    }

                    if (latFromConfig == null || lonFromConfig == null || radiusFromConfig == null) {
                        disableCaptureForConfigFailure("Data konfigurasi lokasi kantor tidak lengkap.")
                        return
                    }

                    if (!isValidOfficeConfig(latFromConfig, lonFromConfig, radiusFromConfig)) {
                        disableCaptureForConfigFailure("Data konfigurasi lokasi kantor tidak valid.")
                        return
                    }

                    officeLat = latFromConfig
                    officeLon = lonFromConfig
                    maxRadius = radiusFromConfig
                    isOfficeConfigReady = true
                    setCaptureEnabled(true)
                }

                override fun onFailure(call: Call<ConfigResponse>, t: Throwable) {
                    disableCaptureForConfigFailure("Gagal terhubung ke server konfigurasi.")
                }
            })
    }

    private fun disableCaptureForConfigFailure(message: String) {
        isOfficeConfigReady = false
        setCaptureEnabled(false)
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun setCaptureEnabled(isEnabled: Boolean) {
        btnCapture.isEnabled = isEnabled
        btnCapture.alpha = if (isEnabled) 1f else 0.5f
    }

    private fun isValidOfficeConfig(lat: Double, lon: Double, radius: Double): Boolean {
        return lat.isFinite() && lon.isFinite() && radius.isFinite() &&
            lat in -90.0..90.0 && lon in -180.0..180.0 && radius > 0
    }

    private fun checkLocationStatus(userLat: Double, userLon: Double): Boolean {
        val results = FloatArray(1)
        Location.distanceBetween(userLat, userLon, officeLat, officeLon, results)
        val distanceInMeters = results[0]

        // LOG UNTUK DEBUGGING (Cek di Logcat dengan filter "ABSENSI_DEBUG")
        Log.d("ABSENSI_DEBUG", "Lokasi User: $userLat, $userLon")
        Log.d("ABSENSI_DEBUG", "Jarak ke Kantor: $distanceInMeters meter")

        return if (distanceInMeters <= maxRadius) {
            true
        } else {
            Toast.makeText(this, "Gagal! Jarak Anda ${distanceInMeters.toInt()}m dari kantor (Maks ${maxRadius.toInt()}m)", Toast.LENGTH_LONG).show()
            false
        }
    }

    // Fungsi lainnya (startCamera, getLocation, takePhoto, uploadKeLaravel) tetap sama seperti sebelumnya...
    // [Gunakan kode fungsi pendukung dari jawaban saya sebelumnya untuk menghemat tempat]

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                currentLat = location.latitude
                currentLon = location.longitude
                Toast.makeText(this, "Lokasi Terkunci!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(viewFinder.surfaceProvider)
            }
            imageCapture = ImageCapture.Builder().build()
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return
        val photoFile = File(cacheDir, SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        btnCapture.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).withEndAction {
            btnCapture.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100)
        }

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                uploadKeLaravel(photoFile)
            }
            override fun onError(exc: ImageCaptureException) {
                Toast.makeText(this@PresensiActivity, "Gagal ambil foto", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun uploadKeLaravel(fotoFile: File) {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val idUserString = sharedPref.getString("ID_USER", "")
        if (idUserString.isNullOrEmpty()) {
            Toast.makeText(this, "Sesi login habis, silakan login ulang", Toast.LENGTH_LONG).show()
            return
        }

        val idUser = idUserString.toRequestBody("text/plain".toMediaTypeOrNull())
        val jenis = jenisAbsen.toRequestBody("text/plain".toMediaTypeOrNull())
        val lat = currentLat.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val lon = currentLon.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        val requestImageFile = fotoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val fotoMultipart = MultipartBody.Part.createFormData("foto", fotoFile.name, requestImageFile)

        ApiConfig.getApiService().submitAbsensi(idUser, jenis, lat, lon, fotoMultipart)
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@PresensiActivity, "Absen Berhasil!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@PresensiActivity, response.body()?.message ?: "Gagal", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@PresensiActivity, "Koneksi Error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
