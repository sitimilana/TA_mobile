package com.example.absensi

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.*


class PresensiActivity : AppCompatActivity() {

    private lateinit var viewFinder: PreviewView
    private lateinit var btnCapture: View
    private lateinit var tvTitlePresensi: TextView
    private lateinit var tvWelcomeName: TextView
    private lateinit var tvRole: TextView

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private lateinit var ivPreview: ImageView
    private lateinit var layoutConfirm: LinearLayout
    private lateinit var btnRetake: Button
    private lateinit var btnSubmit: Button
    private lateinit var pbLoading: ProgressBar
    private lateinit var tvLocationStatus: TextView
    private lateinit var captureOverlay: View
    private var capturedPhotoFile: File? = null

    private var currentLat: Double = 0.0
    private var currentLon: Double = 0.0
    private var jenisAbsen: String = "masuk"

    private var officeLat: Double = 0.0
    private var officeLon: Double = 0.0
    private var maxRadius: Double = 0.0
    private var isOfficeConfigReady = false
    private var refreshConfigJob: Job? = null

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
        NavigationUtils.setupBottomNav(this)
        NavigationUtils.setupHeaderWithUserData(this)

        // 1. Inisialisasi View
        viewFinder = findViewById(R.id.viewFinder)
        btnCapture = findViewById(R.id.btnCapture)
        tvTitlePresensi = findViewById(R.id.tvTitlePresensi)
        tvWelcomeName = findViewById(R.id.tvWelcomeName)
        tvRole = findViewById(R.id.tvRole)

        ivPreview = findViewById(R.id.ivPreview)
        layoutConfirm = findViewById(R.id.layoutConfirm)
        btnRetake = findViewById(R.id.btnRetake)
        btnSubmit = findViewById(R.id.btnSubmit)
        pbLoading = findViewById(R.id.pbLoading)
        tvLocationStatus = findViewById(R.id.tvLocationStatus)
        captureOverlay = findViewById(R.id.captureOverlay)

        btnRetake.setOnClickListener { hidePreview() }
        btnSubmit.setOnClickListener {
            capturedPhotoFile?.let {
                btnSubmit.isEnabled = false
                btnRetake.isEnabled = false
                pbLoading.visibility = View.VISIBLE
                uploadKeLaravel(it)
            }
        }

        // 2. Ambil Data User dari SharedPreferences
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        jenisAbsen = intent.getStringExtra("JENIS_ABSEN") ?: "masuk"
        tvTitlePresensi.text = "Presensi ${jenisAbsen.uppercase()}"

        cameraExecutor = Executors.newSingleThreadExecutor()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        setCaptureEnabled(false)
        fetchOfficeConfig()
        
        // Jangan mulai refresh background di onCreate untuk menghindari race condition
        // Refresh akan dimulai di onResume

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
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", "") ?: ""
        val bearerToken = "Bearer $token"

        ApiConfig.getApiService().getConfigPresensi(bearerToken)
            .enqueue(object : Callback<ConfigResponse> {
                override fun onResponse(call: Call<ConfigResponse>, response: Response<ConfigResponse>) {
                    val config = response.body()?.data
                    val latFromConfig = config?.officeLat
                    val lonFromConfig = config?.officeLon
                    val radiusFromConfig = config?.maxRadius

                    Log.d("CONFIG_DEBUG", "CONFIG RAW: ${response.body()}")
                    Log.d("CONFIG_DEBUG", "LAT: $latFromConfig")
                    Log.d("CONFIG_DEBUG", "LON: $lonFromConfig")
                    Log.d("CONFIG_DEBUG", "RADIUS: $radiusFromConfig")
                    Log.d("CONFIG_DEBUG", "Response Code: ${response.code()}, Body: ${response.body()}")

                    if (!response.isSuccessful) {
                        disableCaptureForConfigFailure("Server konfigurasi mengembalikan error (${response.code()}).")
                        return
                    }

                    if (response.body()?.success != true) {
                        disableCaptureForConfigFailure(response.body()?.message ?: "Konfigurasi lokasi kantor ditolak server.")
                        return
                    }

                    if (latFromConfig == null || lonFromConfig == null || radiusFromConfig == null) {
                        Log.e("CONFIG_DEBUG", "Data null - LAT: $latFromConfig, LON: $lonFromConfig, RADIUS: $radiusFromConfig")
                        disableCaptureForConfigFailure("Data konfigurasi lokasi kantor tidak lengkap.")
                        return
                    }

                    if (!isValidOfficeConfig(latFromConfig, lonFromConfig, radiusFromConfig)) {
                        Log.e("CONFIG_DEBUG", "Data tidak valid - LAT: $latFromConfig, LON: $lonFromConfig, RADIUS: $radiusFromConfig")
                        disableCaptureForConfigFailure("Data konfigurasi lokasi kantor tidak valid.")
                        return
                    }

                    // Hanya update jika data mengalami perubahan
                    val isConfigChanged = (officeLat != latFromConfig) || (officeLon != lonFromConfig) || (maxRadius != radiusFromConfig)
                    
                    officeLat = latFromConfig
                    officeLon = lonFromConfig
                    maxRadius = radiusFromConfig
                    isOfficeConfigReady = true
                    
                    Log.d("CONFIG_DEBUG", "Config berhasil diupdate - LAT: $officeLat, LON: $officeLon, RADIUS: $maxRadius")
                    if (isConfigChanged) {
                        Log.d("CONFIG_DEBUG", "Konfigurasi lokasi kantor telah diperbarui")
                    }
                    
                    setCaptureEnabled(true)
                }

                override fun onFailure(call: Call<ConfigResponse>, t: Throwable) {
                    Log.e("CONFIG_DEBUG", "Gagal fetch config: ${t.message}")
                    disableCaptureForConfigFailure("Gagal terhubung ke server konfigurasi: ${t.message}")
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
        if (!isOfficeConfigReady) {
            Toast.makeText(this, "Konfigurasi lokasi kantor belum siap. Silakan coba lagi.", Toast.LENGTH_SHORT).show()
            return false
        }

        if (maxRadius <= 0) {
            Log.e("ABSENSI_DEBUG", "ERROR: maxRadius tidak valid: $maxRadius")
            Toast.makeText(this, "Konfigurasi radius tidak valid. Hubungi administrator.", Toast.LENGTH_LONG).show()
            return false
        }

        val results = FloatArray(1)
        Location.distanceBetween(userLat, userLon, officeLat, officeLon, results)
        val distanceInMeters = results[0]

        Log.d("ABSENSI_DEBUG", "Lokasi User: $userLat, $userLon")
        Log.d("ABSENSI_DEBUG", "Lokasi Kantor: $officeLat, $officeLon")
        Log.d("ABSENSI_DEBUG", "Jarak ke Kantor (Meter): $distanceInMeters")
        Log.d("ABSENSI_DEBUG", "Batas Radius (Meter): $maxRadius")
        
        return if (distanceInMeters <= maxRadius) {
            true
        } else {
            val jarakFormat = String.format("%.0f", distanceInMeters)
            val batasFormat = String.format("%.0f", maxRadius)
            Toast.makeText(
                this,
                "Gagal! Jarak Anda ${jarakFormat}m dari kantor (Maks ${batasFormat}m)",
                Toast.LENGTH_LONG
            ).show()

            false
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {
        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                currentLat = location.latitude
                currentLon = location.longitude
                tvLocationStatus.text = "Lokasi GPS Terkunci: ${String.format("%.4f", currentLat)}, ${String.format("%.4f", currentLon)}"
                tvLocationStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
                Log.d("LOCATION_DEBUG", "GPS Location: $currentLat, $currentLon")
            } else {
                tvLocationStatus.text = "Gagal mengunci lokasi GPS"
                tvLocationStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
                Log.w("LOCATION_DEBUG", "Lokasi GPS tidak tersedia")
            }
        }.addOnFailureListener { exception ->
            Log.e("LOCATION_DEBUG", "Error getting location: ${exception.message}")
            tvLocationStatus.text = "Error: ${exception.message}"
            tvLocationStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
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

        // Prevent double clicking
        btnCapture.isEnabled = false

        val photoFile = File(cacheDir, SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        btnCapture.animate().scaleX(0.8f).scaleY(0.8f).setDuration(100).withEndAction {
            btnCapture.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100)
        }

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                capturedPhotoFile = photoFile
                val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
                ivPreview.setImageBitmap(bitmap)
                showPreview()
                btnCapture.isEnabled = true
            }
            override fun onError(exc: ImageCaptureException) {
                btnCapture.isEnabled = true
                Toast.makeText(this@PresensiActivity, "Gagal ambil foto: " + exc.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showPreview() {
        captureOverlay.visibility = View.GONE
        ivPreview.visibility = View.VISIBLE
        layoutConfirm.visibility = View.VISIBLE
        layoutConfirm.alpha = 0f
        layoutConfirm.animate().alpha(1f).setDuration(300).start()
    }

    private fun hidePreview() {
        ivPreview.visibility = View.GONE
        layoutConfirm.visibility = View.GONE
        captureOverlay.visibility = View.VISIBLE
        capturedPhotoFile?.delete()
        capturedPhotoFile = null
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

        Toast.makeText(this, "Mengirim data absensi...", Toast.LENGTH_SHORT).show()

        ApiConfig.getApiService().submitAbsensi(idUser, jenis, lat, lon, fotoMultipart)
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    pbLoading.visibility = View.GONE
                    btnSubmit.isEnabled = true
                    btnRetake.isEnabled = true

                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@PresensiActivity, response.body()?.message ?: "Absen Berhasil!", Toast.LENGTH_LONG).show()
                        finish()
                    } else {
                        // Membaca pesan error dari Laravel jika ditolak (misal belum jam 15:00 untuk pulang)
                        try {
                            val errorString = response.errorBody()?.string()
                            if (errorString != null) {
                                val jsonObject = org.json.JSONObject(errorString)
                                val pesanError = jsonObject.getString("message")
                                Toast.makeText(this@PresensiActivity, pesanError, Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this@PresensiActivity, "Gagal absen. Terjadi kesalahan server.", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@PresensiActivity, "Gagal memproses respon server: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    pbLoading.visibility = View.GONE
                    btnSubmit.isEnabled = true
                    btnRetake.isEnabled = true
                    Toast.makeText(this@PresensiActivity, "Koneksi lambat atau terputus: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        refreshConfigJob?.cancel()
    }

    override fun onResume() {
        super.onResume()
        // Refresh config ketika activity resume untuk memastikan data selalu terbaru
        if (allPermissionsGranted()) {
            getLocation()
        }
        startPeriodicConfigRefresh()
    }

    override fun onPause() {
        super.onPause()
        // Stop refresh background ketika activity pause
        refreshConfigJob?.cancel()
    }

    private fun startPeriodicConfigRefresh() {
        // Cancel job sebelumnya jika ada
        refreshConfigJob?.cancel()
        
        // Mulai refresh otomatis setiap 5 menit (300 detik)
        refreshConfigJob = lifecycleScope.launch {
            while (isActive) {
                delay(5 * 60 * 1000) // 5 minutes = 300,000 ms
                fetchOfficeConfig()
                Log.d("CONFIG_DEBUG", "Periodic config refresh initiated")
            }
        }
    }
}