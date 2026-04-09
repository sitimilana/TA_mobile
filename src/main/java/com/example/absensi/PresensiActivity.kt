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
import com.example.absensi.network.ApiConfig // Ganti sesuai package Anda
import com.example.absensi.network.LoginResponse // Ganti sesuai package Anda
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

    private var imageCapture: ImageCapture? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Variabel Penampung Data
    private var currentLat: Double = 0.0
    private var currentLon: Double = 0.0
    private var jenisAbsen: String = "masuk" // Akan diubah berdasarkan dari dashboard

    // 1. Meminta Izin Kamera dan Lokasi
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val cameraGranted = permissions[Manifest.permission.CAMERA] ?: false
            val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

            if (cameraGranted && locationGranted) {
                startCamera()
                getLocation()
            } else {
                Toast.makeText(this, "Izin Kamera & Lokasi wajib diberikan!", Toast.LENGTH_LONG).show()
                finish() // Keluar jika tidak diizinkan
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_presensi)

        viewFinder = findViewById(R.id.viewFinder)
        btnCapture = findViewById(R.id.btnCapture)
        tvTitlePresensi = findViewById(R.id.tvTitlePresensi)

        // Menerima data "jenis" dari Dashboard (Masuk atau Pulang)
        jenisAbsen = intent.getStringExtra("JENIS_ABSEN") ?: "masuk"
        tvTitlePresensi.text = "Presensi ${jenisAbsen.uppercase()}"

        cameraExecutor = Executors.newSingleThreadExecutor()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // Cek Izin
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

        // Aksi Tombol Jepret
        btnCapture.setOnClickListener {
            if (currentLat == 0.0) {
                Toast.makeText(this, "Mencari lokasi GPS... Coba lagi.", Toast.LENGTH_SHORT).show()
                getLocation()
            } else {
                takePhoto()
            }
        }
    }

    // 2. Menghidupkan Kamera Depan (Selfie)
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder().build()

            // Memilih Kamera Depan
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    // 3. Mengambil Lokasi GPS
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

    // 4. Proses Jepret Foto
    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        // Simpan foto di memori sementara (Cache) agar tidak menuh-menuhin HP
        val photoFile = File(
            cacheDir,
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis()) + ".jpg"
        )

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        Toast.makeText(this, "Memproses gambar...", Toast.LENGTH_SHORT).show()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Toast.makeText(this@PresensiActivity, "Gagal mengambil foto.", Toast.LENGTH_SHORT).show()
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    // Jika sukses foto, langsung kirim ke Laravel
                    uploadKeLaravel(photoFile)
                }
            }
        )
    }

    // 5. Mengirim File dan Data ke API Laravel
    private fun uploadKeLaravel(fotoFile: File) {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        // Asumsi saat login Anda menyimpan ID_USER. Pastikan ini ada!
        val idUserString = sharedPref.getString("ID_USER", "1") ?: "1"

        // Mengubah data teks menjadi RequestBody
        val idUser = idUserString.toRequestBody("text/plain".toMediaTypeOrNull())
        val jenis = jenisAbsen.toRequestBody("text/plain".toMediaTypeOrNull())
        val lat = currentLat.toString().toRequestBody("text/plain".toMediaTypeOrNull())
        val lon = currentLon.toString().toRequestBody("text/plain".toMediaTypeOrNull())

        // Mengubah File gambar menjadi MultipartBody
        val requestImageFile = fotoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val fotoMultipart = MultipartBody.Part.createFormData("foto", fotoFile.name, requestImageFile)

        Toast.makeText(this, "Mengirim absen ke server...", Toast.LENGTH_SHORT).show()

        ApiConfig.getApiService().submitAbsensi(idUser, jenis, lat, lon, fotoMultipart)
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    if (response.isSuccessful && response.body()?.success == true) {
                        Toast.makeText(this@PresensiActivity, "Absen $jenisAbsen BERHASIL!", Toast.LENGTH_LONG).show()
                        finish() // Tutup halaman kamera, kembali ke Dashboard
                    } else {
                        // Jika ada pesan error dari Laravel (misal: "Anda sudah absen")
                        val errorMessage = response.body()?.message ?: "Gagal absen."
                        Toast.makeText(this@PresensiActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    Toast.makeText(this@PresensiActivity, "Koneksi Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun allPermissionsGranted() = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION
    ).all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}