package com.example.absensi

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.ProfilResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import java.io.File
import java.io.FileOutputStream
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import com.bumptech.glide.Glide
import android.widget.ImageView
import id.zelory.compressor.Compressor
import id.zelory.compressor.constraint.quality
import id.zelory.compressor.constraint.resolution
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ProfileActivity : AppCompatActivity() {
    private var imageFile: File? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            imageFile = uriToFile(uri)
            if (imageFile != null) {
                // Tampilkan gambar yang dipilih ke ImageView
                val ivProfilFoto: ImageView = findViewById(R.id.ivProfilFoto)
                ivProfilFoto.setImageURI(uri)

                // Munculkan tombol Simpan Foto
                val btnSimpanFoto: Button = findViewById(R.id.btnSimpanFoto)
                btnSimpanFoto.visibility = View.VISIBLE
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val tvProfilNama: TextView = findViewById(R.id.tvProfilNama)
        val tvProfilRole: TextView = findViewById(R.id.tvProfilRole)
        val btnUbahPassword: Button = findViewById(R.id.btnUbahPassword)
        val btnProfilLogout: Button = findViewById(R.id.btnProfilLogout)

        // Ambil data sementara dari SharedPreferences
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        tvProfilNama.text = sharedPref.getString("NAMA_LENGKAP", "Pengguna")
        tvProfilRole.text = sharedPref.getString("ROLE", "Karyawan")

        // Inisialisasi data biodata dari layout include
        val rowEmail = findViewById<View>(R.id.row_email)
        rowEmail.findViewById<TextView>(R.id.tvLabel).text = "Email"
        rowEmail.findViewById<TextView>(R.id.tvValue).text = sharedPref.getString("EMAIL", "-")

        val rowHp = findViewById<View>(R.id.row_hp)
        rowHp.findViewById<TextView>(R.id.tvLabel).text = "No. HP"
        rowHp.findViewById<TextView>(R.id.tvValue).text = sharedPref.getString("NO_HP", "-")

        val rowAlamat = findViewById<View>(R.id.row_alamat)
        rowAlamat.findViewById<TextView>(R.id.tvLabel).text = "Alamat"
        rowAlamat.findViewById<TextView>(R.id.tvValue).text = sharedPref.getString("ALAMAT", "-")

        val rowTglMasuk = findViewById<View>(R.id.row_tgl_masuk)
        rowTglMasuk.findViewById<TextView>(R.id.tvLabel).text = "Tanggal Masuk"
        rowTglMasuk.findViewById<TextView>(R.id.tvValue).text = sharedPref.getString("TANGGAL_MASUK", "-")

        // Panggil API untuk update data terbaru
        fetchDataProfil()

        // Tombol Ubah Password
        btnUbahPassword.setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            startActivity(intent)
        }

        // Tombol Logout
        btnProfilLogout.setOnClickListener {
            showLogoutConfirmation()
        }
        val btnEditFoto: View = findViewById(R.id.btnEditFoto) // Pastikan ID ini ada di XML
        btnEditFoto.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        val btnSimpanFoto: Button = findViewById(R.id.btnSimpanFoto)
        btnSimpanFoto.setOnClickListener {
            if (imageFile != null) {
                uploadFoto(imageFile!!)
                btnSimpanFoto.isEnabled = false
                btnSimpanFoto.text = "Menyimpan..."
            }
        }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")
            .setPositiveButton("Ya, Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun logout() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        sharedPref.edit().clear().apply()

        Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun fetchDataProfil() {
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val token = "Bearer " + sharedPref.getString("TOKEN", "")

        ApiConfig.getApiService().getProfil(token).enqueue(object : Callback<ProfilResponse> {
            override fun onResponse(call: Call<ProfilResponse>, response: Response<ProfilResponse>) {
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()!!.data

                    // Update TextView
                    findViewById<TextView>(R.id.tvProfilNama).text = data.namaLengkap
                    findViewById<TextView>(R.id.tvProfilRole).text = data.divisi

                    // Update Data Row
                    updateRow(R.id.row_email, "Email", data.email)
                    updateRow(R.id.row_hp, "No. HP", data.noHp)
                    updateRow(R.id.row_alamat, "Alamat", data.alamat)
                    updateRow(R.id.row_tgl_masuk, "Tanggal Masuk", data.tanggalMasuk)

                    val fotoUrl = data.foto
                    if (!fotoUrl.isNullOrEmpty()) {
                        sharedPref.edit().putString("FOTO_PROFIL", fotoUrl).apply()
                        val ivProfilFoto: ImageView = findViewById(R.id.ivProfilFoto)
                        Glide.with(this@ProfileActivity)
                            .load(fotoUrl)
                            .placeholder(R.drawable.profile) // Tampilkan gambar default selagi loading
                            .into(ivProfilFoto)
                    }
                }
            }

            override fun onFailure(call: Call<ProfilResponse>, t: Throwable) {
                Toast.makeText(this@ProfileActivity, "Gagal memuat profil", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateRow(rowId: Int, label: String, value: String) {
        val row = findViewById<View>(rowId)
        row.findViewById<TextView>(R.id.tvLabel).text = label
        row.findViewById<TextView>(R.id.tvValue).text = value
    }
    private fun uriToFile(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("temp_profil", ".jpg", cacheDir)
            val outputStream = FileOutputStream(tempFile)
            inputStream?.copyTo(outputStream)
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun uploadFoto(file: File) {
        val sharedPref = getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val token = "Bearer " + sharedPref.getString("TOKEN", "")

        val btnSimpanFoto: Button = findViewById(R.id.btnSimpanFoto)
        btnSimpanFoto.isEnabled = false
        btnSimpanFoto.text = "Memproses foto..."

        // Jalankan kompresi di Background Thread menggunakan Coroutine
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // 1. Proses Kompresi Gambar (mengecilkan ukuran file)
                val compressedImageFile = withContext(Dispatchers.IO) {
                    Compressor.compress(this@ProfileActivity, file) {
                        resolution(800, 800) // Ukuran maksimal resolusi
                        quality(80)         // Kualitas gambar 80%
                    }
                }

                // 2. Siapkan File yang Sudah Dikompres untuk Retrofit
                val reqImageFile = compressedImageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
                val fotoMultipart = MultipartBody.Part.createFormData("foto", compressedImageFile.name, reqImageFile)

                btnSimpanFoto.text = "Mengunggah..."

                // 3. Kirim ke Server
                ApiConfig.getApiService().updateFoto(token, fotoMultipart).enqueue(object : Callback<com.example.absensi.network.ApiMessageResponse> {
                    override fun onResponse(call: Call<com.example.absensi.network.ApiMessageResponse>, response: Response<com.example.absensi.network.ApiMessageResponse>) {
                        if (response.isSuccessful && response.body() != null) {
                            Toast.makeText(this@ProfileActivity, response.body()!!.message, Toast.LENGTH_SHORT).show()

                            // Sembunyikan tombol dan reset teksnya
                            btnSimpanFoto.visibility = View.GONE
                            btnSimpanFoto.isEnabled = true
                            btnSimpanFoto.text = "Simpan Foto"

                            // Panggil API profil untuk memuat foto terbaru ke UI
                            fetchDataProfil()
                        } else {
                            Toast.makeText(this@ProfileActivity, "Gagal mengunggah foto. Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                            btnSimpanFoto.isEnabled = true
                            btnSimpanFoto.text = "Simpan Foto"
                        }
                    }

                    override fun onFailure(call: Call<com.example.absensi.network.ApiMessageResponse>, t: Throwable) {
                        Toast.makeText(this@ProfileActivity, "Koneksi Error: ${t.message}", Toast.LENGTH_SHORT).show()
                        btnSimpanFoto.isEnabled = true
                        btnSimpanFoto.text = "Simpan Foto"
                    }
                })

            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ProfileActivity, "Gagal mengompres gambar", Toast.LENGTH_SHORT).show()
                btnSimpanFoto.isEnabled = true
                btnSimpanFoto.text = "Simpan Foto"
            }
        }
    }
}