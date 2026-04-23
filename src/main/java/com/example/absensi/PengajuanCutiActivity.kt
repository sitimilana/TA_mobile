package com.example.absensi

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.LoginResponse
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.*

class PengajuanCutiActivity : AppCompatActivity() {

    private lateinit var etNama: TextInputEditText
    private lateinit var etDivisi: TextInputEditText
    private lateinit var etTanggalMulai: TextInputEditText
    private lateinit var etTanggalSelesai: TextInputEditText
    private lateinit var spinnerKeterangan: Spinner
    private lateinit var etAlasanCuti: TextInputEditText
    private lateinit var btnChooseFile: MaterialButton
    private lateinit var btnAjukan: MaterialButton

    // Header Views
    private lateinit var tvWelcomeName: TextView
    private lateinit var tvRole: TextView

    private var selectedImageUri: Uri? = null
    private var imageFile: File? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            btnChooseFile.text = "File Terpilih"
            imageFile = uriToFile(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pengajuan_cuti)
        NavigationUtils.setupBottomNav(this)

        // --- 1. Inisialisasi View Header & Form ---
        tvWelcomeName = findViewById(R.id.tvWelcomeName)
        tvRole = findViewById(R.id.tvRole)

        etNama = findViewById(R.id.etNama)
        etDivisi = findViewById(R.id.etDivisi)
        etTanggalMulai = findViewById(R.id.etTanggalMulai)
        etTanggalSelesai = findViewById(R.id.etTanggalSelesai)
        spinnerKeterangan = findViewById(R.id.spinnerKeterangan)
        etAlasanCuti = findViewById(R.id.etAlasanCuti)
        btnChooseFile = findViewById(R.id.btnChooseFile)
        btnAjukan = findViewById(R.id.btnAjukan)

        // --- 2. Ambil Data User dari SharedPreferences (Untuk Header & Isi Form) ---
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val namaLengkap = sharedPref.getString("NAMA_LENGKAP", "Karyawan")
        val divisi = sharedPref.getString("DIVISI", "Belum ada divisi")

        // Set ke Header Profil
        tvWelcomeName.text = "Selamat Datang, $namaLengkap"
        tvRole.text = divisi

        // Set ke Form Input (Disabled agar tidak diubah manual)
        etNama.setText(namaLengkap)
        etNama.isEnabled = false
        etDivisi.setText(divisi)
        etDivisi.isEnabled = false

        // --- 3. Setup Spinner ---
        val listKeterangan = arrayOf("Cuti Tahunan", "Izin", "Sakit", "Cuti Melahirkan", "Cuti Penting")
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listKeterangan)
        spinnerKeterangan.adapter = adapterSpinner

        // --- 4. Setup Kalender (Logika H-1) ---
        etTanggalMulai.setOnClickListener { showDatePicker(true) { date -> etTanggalMulai.setText(date) } }
        etTanggalSelesai.setOnClickListener { showDatePicker(false) { date -> etTanggalSelesai.setText(date) } }

        // --- 5. Setup Tombol Upload Bukti ---
        btnChooseFile.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // --- 6. Setup Tombol Ajukan ---
        btnAjukan.setOnClickListener {
            kirimDataCuti()
        }
    }

    private fun showDatePicker(isTanggalMulai: Boolean, onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
            val formattedDate = String.format("%04d-%02d-%02d", selectedYear, selectedMonth + 1, selectedDay)
            onDateSelected(formattedDate)
        }, year, month, day)

        val jenisCuti = spinnerKeterangan.selectedItem.toString()
        val waktuSekarangMs = System.currentTimeMillis() - 1000

        if (isTanggalMulai) {
            if (jenisCuti.equals("Sakit", ignoreCase = true)) {
                // Sakit boleh pilih hari ini
                datePickerDialog.datePicker.minDate = waktuSekarangMs
            } else {
                // Selain sakit, minimal pilih besok (+ 24 Jam)
                datePickerDialog.datePicker.minDate = waktuSekarangMs + (1000 * 60 * 60 * 24)
            }
        } else {
            // Untuk tanggal selesai, minimal adalah hari ini
            datePickerDialog.datePicker.minDate = waktuSekarangMs
        }

        datePickerDialog.show()
    }

    private fun uriToFile(uri: Uri): File? {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("bukti_cuti", ".jpg", cacheDir)
            val outputStream = FileOutputStream(tempFile)
            inputStream?.copyTo(outputStream)
            return tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private fun kirimDataCuti() {
        val tglMulai = etTanggalMulai.text.toString()
        val tglSelesai = etTanggalSelesai.text.toString()
        val alasan = etAlasanCuti.text.toString().trim()
        val jenisCuti = spinnerKeterangan.selectedItem.toString()

        if (tglMulai.isEmpty() || tglSelesai.isEmpty()) {
            Toast.makeText(this, "Tanggal Mulai & Selesai wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }
        if (alasan.isEmpty()) {
            Toast.makeText(this, "Alasan wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", "") ?: ""
        val bearerToken = "Bearer $token"

        val reqTglMulai = tglMulai.toRequestBody("text/plain".toMediaTypeOrNull())
        val reqTglSelesai = tglSelesai.toRequestBody("text/plain".toMediaTypeOrNull())
        val reqJenisCuti = jenisCuti.toRequestBody("text/plain".toMediaTypeOrNull())
        val reqAlasan = alasan.toRequestBody("text/plain".toMediaTypeOrNull())

        var fotoMultipart: MultipartBody.Part? = null
        if (imageFile != null) {
            val reqImageFile = imageFile!!.asRequestBody("image/jpeg".toMediaTypeOrNull())
            fotoMultipart = MultipartBody.Part.createFormData("berkas_bukti", imageFile!!.name, reqImageFile)
        }

        btnAjukan.isEnabled = false
        btnAjukan.text = "Mengirim..."

        ApiConfig.getApiService().submitCuti(bearerToken, reqTglMulai, reqTglSelesai, reqJenisCuti, reqAlasan, fotoMultipart)
            .enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    btnAjukan.isEnabled = true
                    btnAjukan.text = "Ajukan Sekarang"

                    if (response.isSuccessful && response.code() == 201) {
                        Toast.makeText(this@PengajuanCutiActivity, "Pengajuan cuti berhasil dikirim!", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@PengajuanCutiActivity, RiwayatCutiActivity::class.java))
                        finish()
                    } else {
                        try {
                            val errorString = response.errorBody()?.string()
                            if (errorString != null) {
                                val jsonObject = JSONObject(errorString)
                                val pesanError = jsonObject.getString("message")
                                Toast.makeText(this@PengajuanCutiActivity, pesanError, Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this@PengajuanCutiActivity, "Gagal. Terjadi kesalahan server.", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@PengajuanCutiActivity, "Gagal memproses respons: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    btnAjukan.isEnabled = true
                    btnAjukan.text = "Ajukan Sekarang"
                    Toast.makeText(this@PengajuanCutiActivity, "Koneksi Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }
}