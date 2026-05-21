package com.example.absensi

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageButton
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
import org.json.JSONObject
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
    private var isHariLibur = false
    private var pesanLiburStr = ""
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

        // DITAMBAHKAN: Tambah Logout Button Listener via Header
        val btnLogout: ImageButton = findViewById(R.id.btnLogout)
        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

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
            // Urutan Pengecekan: Libur -> Config -> GPS -> Jarak
            if (isHariLibur) {
                // Jika libur, munculkan dialog libur, BUKAN error jarak
                showErrorDialog("Akses Ditolak", pesanLiburStr)
            } else if (!isOfficeConfigReady) {
                Toast.makeText(this, "Konfigurasi lokasi kantor belum tersedia.", Toast.LENGTH_SHORT).show()
            } else if (currentLat == 0.0) {
                Toast.makeText(this, "Mencari lokasi GPS... Tunggu sampai 'Lokasi Terkunci' muncul.", Toast.LENGTH_SHORT).show()
                getLocation()
            } else if (checkLocationStatus(currentLat, currentLon)) {
                // Lolos semua, silakan ambil foto
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
                    // === DEBUG LOGGING LENGKAP ===
                    Log.d("CONFIG_DEBUG", "========== FETCH CONFIG RESPONSE ==========")
                    Log.d("CONFIG_DEBUG", "HTTP Status Code: ${response.code()}")
                    Log.d("CONFIG_DEBUG", "Is Successful: ${response.isSuccessful}")
                    Log.d("CONFIG_DEBUG", "Response Body: ${response.body()}")
                    
                    val config = response.body()?.data
                    Log.d("CONFIG_DEBUG", "Config Data: $config")
                    Log.d("CONFIG_DEBUG", "Lat: ${config?.officeLat}, Lon: ${config?.officeLon}, Radius: ${config?.maxRadius}")
                    Log.d("CONFIG_DEBUG", "Type of maxRadius: ${config?.maxRadius?.javaClass?.simpleName}")
                    Log.d("CONFIG_DEBUG", "=========================================")

                    // 1. CEK HTTP STATUS DULU
                    if (!response.isSuccessful) {
                        Log.e("CONFIG_DEBUG", "HTTP ERROR: ${response.code()}")
                        val errorBody = try {
                            response.errorBody()?.string()
                        } catch (e: Exception) {
                            "Unknown error"
                        }
                        disableCaptureForConfigFailure("Server Error (${response.code()}): $errorBody")
                        return
                    }

                    // 2. CEK SUCCESS FLAG
                    if (response.body()?.success != true) {
                        val message = response.body()?.message ?: "Konfigurasi ditolak server"
                        Log.e("CONFIG_DEBUG", "Success Flag FALSE: $message")
                        disableCaptureForConfigFailure(message)
                        return
                    }

                    // 3. CEK DATA NULL
                    if (config == null) {
                        Log.e("CONFIG_DEBUG", "ERROR: Config data is NULL!")
                        disableCaptureForConfigFailure("Data konfigurasi kosong dari server")
                        return
                    }

                    // 4. CEK SETIAP FIELD SECARA INDIVIDUAL
                    val latFromConfig = config.officeLat
                    val lonFromConfig = config.officeLon
                    val radiusFromConfig = config.maxRadius

                    if (latFromConfig == null) {
                        Log.e("CONFIG_DEBUG", "ERROR: officeLat is NULL!")
                        disableCaptureForConfigFailure("Latitude kantor tidak dikonfigurasi di server")
                        return
                    }
                    if (lonFromConfig == null) {
                        Log.e("CONFIG_DEBUG", "ERROR: officeLon is NULL!")
                        disableCaptureForConfigFailure("Longitude kantor tidak dikonfigurasi di server")
                        return
                    }
                    if (radiusFromConfig == null) {
                        Log.e("CONFIG_DEBUG", "ERROR: maxRadius is NULL!")
                        disableCaptureForConfigFailure("Radius maksimal tidak dikonfigurasi di server (NULL)")
                        return
                    }

                    // 5. CEK NILAI 0 (BUKAN NULL, TAPI 0!)
                    if (radiusFromConfig == 0.0) {
                        Log.e("CONFIG_DEBUG", "ERROR: maxRadius is ZERO!")
                        disableCaptureForConfigFailure("Radius maksimal tidak boleh 0! Silakan konfigurasi di server.")
                        return
                    }

                    // 6. CEK HARI LIBUR
                    if (config.isLibur == true) {
                        isHariLibur = true
                        pesanLiburStr = config.pesanLibur ?: "Hari ini adalah hari libur."
                        setCaptureEnabled(false)
                        showErrorDialog("Akses Ditolak", pesanLiburStr)
                        return
                    }

                    // 7. SEMUA CEK LOLOS - UPDATE VARIABEL
                    isHariLibur = false
                    officeLat = latFromConfig
                    officeLon = lonFromConfig
                    maxRadius = radiusFromConfig
                    isOfficeConfigReady = true

                    Log.d("CONFIG_DEBUG", "✓ Config berhasil diload: Lat=$officeLat, Lon=$officeLon, Radius=$maxRadius")
                    setCaptureEnabled(true)
                }

                override fun onFailure(call: Call<ConfigResponse>, t: Throwable) {
                    Log.e("CONFIG_DEBUG", "NETWORK ERROR: ${t.message}", t)
                    disableCaptureForConfigFailure("Gagal terhubung ke server: ${t.message}")
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
        // VALIDASI 1: Config harus siap
        if (!isOfficeConfigReady) {
            Toast.makeText(this, "Konfigurasi lokasi belum siap.", Toast.LENGTH_SHORT).show()
            return false
        }

        // VALIDASI 2: Radius HARUS > 0
        if (maxRadius <= 0.0) {
            Log.e("ABSENSI_DEBUG", "FATAL ERROR: maxRadius invalid = $maxRadius")
            Toast.makeText(
                this,
                "ERROR: Radius tidak valid ($maxRadius). Hubungi admin untuk konfigurasi server.",
                Toast.LENGTH_LONG
            ).show()
            return false
        }

        // VALIDASI 3: Office location HARUS valid
        if (officeLat == 0.0 || officeLon == 0.0) {
            Log.e("ABSENSI_DEBUG", "FATAL ERROR: Office location invalid - Lat:$officeLat, Lon:$officeLon")
            Toast.makeText(
                this,
                "ERROR: Lokasi kantor tidak valid. Hubungi admin.",
                Toast.LENGTH_LONG
            ).show()
            return false
        }

        // HITUNG JARAK
        val results = FloatArray(1)
        Location.distanceBetween(userLat, userLon, officeLat, officeLon, results)
        val distanceInMeters = results[0]

        // LOGGING DETAIL
        Log.d("ABSENSI_DEBUG", "=== LOCATION CHECK ===")
        Log.d("ABSENSI_DEBUG", "User Position: ($userLat, $userLon)")
        Log.d("ABSENSI_DEBUG", "Office Position: ($officeLat, $officeLon)")
        Log.d("ABSENSI_DEBUG", "Distance: ${String.format("%.2f", distanceInMeters)}m")
        Log.d("ABSENSI_DEBUG", "Max Radius: ${String.format("%.2f", maxRadius)}m")
        Log.d("ABSENSI_DEBUG", "Status: ${if (distanceInMeters <= maxRadius) "✓ ALLOWED" else "✗ REJECTED"}")
        Log.d("ABSENSI_DEBUG", "====================")

        // CEK JARAK
        return if (distanceInMeters <= maxRadius) {
            Log.d("ABSENSI_DEBUG", "✓ User berada dalam radius, absen diizinkan")
            true
        } else {
            val jarakFormat = String.format("%.0f", distanceInMeters)
            val batasFormat = String.format("%.0f", maxRadius)
            Toast.makeText(
                this,
                "❌ Gagal! Jarak Anda ${jarakFormat}m dari kantor (Maks ${batasFormat}m)",
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
                tvLocationStatus.text = "GPS Terkunci: ${String.format("%.4f", currentLat)}, ${String.format("%.4f", currentLon)}"
                tvLocationStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            } else {
                tvLocationStatus.text = "Gagal mengunci lokasi"
                tvLocationStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            }
        }.addOnFailureListener { exception ->
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
                        val message = try {
                            val errorString = response.errorBody()?.string()
                            if (errorString != null) {
                                JSONObject(errorString).getString("message")
                            } else {
                                "Gagal absen. Terjadi kesalahan server."
                            }
                        } catch (e: Exception) {
                            "Gagal memproses respon server: ${response.code()}"
                        }

                        if (response.code() == 403) {
                            showErrorDialog("Akses Ditolak", message)
                        } else {
                            Toast.makeText(this@PresensiActivity, message, Toast.LENGTH_LONG).show()
                        }
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    pbLoading.visibility = View.GONE
                    btnSubmit.isEnabled = true
                    btnRetake.isEnabled = true
                    Toast.makeText(this@PresensiActivity, "Koneksi lambat/terputus: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun showErrorDialog(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                if (message.contains("libur", ignoreCase = true) || message.contains("akhir pekan", ignoreCase = true)) {
                    finish()
                } else {
                    hidePreview()
                }
            }
            .show()
    }

    private fun allPermissionsGranted() = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        refreshConfigJob?.cancel()
    }

    override fun onResume() {
        super.onResume()
        if (allPermissionsGranted()) {
            getLocation()
        }
        startPeriodicConfigRefresh()
    }

    override fun onPause() {
        super.onPause()
        refreshConfigJob?.cancel()
    }

    private fun startPeriodicConfigRefresh() {
        refreshConfigJob?.cancel()
        refreshConfigJob = lifecycleScope.launch {
            while (isActive) {
                delay(5 * 60 * 1000)
                fetchOfficeConfig()
            }
        }
    }

    // DITAMBAHKAN: Fungsi Konfirmasi Logout dengan Dialog
    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Apakah Anda yakin ingin keluar?")
            .setPositiveButton("Ya, Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    // DITAMBAHKAN: Fungsi Eksekusi Sesi Cleansing dan Intent ke Login
    private fun logout() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val editor = sharedPref.edit()
        editor.clear()
        editor.apply()

        Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}