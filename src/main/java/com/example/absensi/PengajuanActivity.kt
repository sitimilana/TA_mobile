package com.example.absensi

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
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

class PengajuanActivity : AppCompatActivity() {

    private lateinit var etNama: TextInputEditText
    private lateinit var etDivisi: TextInputEditText
    private lateinit var etTanggalMulai: TextInputEditText
    private lateinit var etTanggalSelesai: TextInputEditText
    private lateinit var spinnerKeterangan: Spinner
    private lateinit var etAlasanCuti: TextInputEditText
    private lateinit var btnChooseFile: MaterialButton
    private lateinit var btnAjukan: MaterialButton

    // View Tambahan untuk Upload
    private lateinit var layoutUpload: LinearLayout
    private lateinit var tvUploadHint: TextView
    private lateinit var tvFileName: TextView

    // Header Views
    private lateinit var tvWelcomeName: TextView
    private lateinit var tvRole: TextView

    private var selectedImageUri: Uri? = null
    private var imageFile: File? = null

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            selectedImageUri = uri
            btnChooseFile.text = "Ubah File"
            tvFileName.text = "File berhasil dilampirkan"
            tvFileName.setTextColor(resources.getColor(R.color.avatar_blue, null))
            imageFile = uriToFile(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_pengajuan)
        NavigationUtils.setupBottomNav(this)

        // --- 1. Inisialisasi View ---
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

        layoutUpload = findViewById(R.id.layoutUpload)
        tvUploadHint = findViewById(R.id.tvUploadHint)
        tvFileName = findViewById(R.id.tvFileName)

        // --- 2. Ambil Data User dari SharedPreferences ---
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val namaLengkap = sharedPref.getString("NAMA_LENGKAP", "Karyawan")
        val divisi = sharedPref.getString("DIVISI", "Belum ada divisi")

        tvWelcomeName.text = "Selamat Datang, $namaLengkap"
        tvRole.text = divisi

        etNama.setText(namaLengkap)
        etDivisi.setText(divisi)

        // --- 3. Setup Spinner (Disederhanakan menjadi 3 Kategori) ---
        val listKeterangan = arrayOf("Cuti", "Izin", "Sakit")
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listKeterangan)
        spinnerKeterangan.adapter = adapterSpinner

        spinnerKeterangan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val jenisPengajuan = listKeterangan[position].lowercase()

                when {
                    jenisPengajuan == "sakit" -> {
                        layoutUpload.visibility = View.VISIBLE
                        tvUploadHint.text = "(Wajib dilampirkan surat dokter/keterangan)"
                        tvUploadHint.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                    }
                    jenisPengajuan == "izin" -> {
                        layoutUpload.visibility = View.VISIBLE
                        tvUploadHint.text = "(Opsional)"
                        tvUploadHint.setTextColor(resources.getColor(R.color.role_text, null))
                    }
                    jenisPengajuan == "cuti" -> {
                        // Untuk Cuti, sembunyikan kotak upload
                        layoutUpload.visibility = View.GONE

                        // Bersihkan file jika ada
                        imageFile = null
                        selectedImageUri = null
                        btnChooseFile.text = "Pilih File"
                        tvFileName.text = "Belum ada file yang dipilih"
                        tvFileName.setTextColor(resources.getColor(R.color.role_text, null))
                    }
                }

                // Reset tanggal agar user memilih ulang sesuai aturan validasi kalender
                etTanggalMulai.setText("")
                etTanggalSelesai.setText("")
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // --- 4. Setup Kalender ---
        etTanggalMulai.setOnClickListener { showDatePicker(true) { date -> etTanggalMulai.setText(date) } }
        etTanggalSelesai.setOnClickListener { showDatePicker(false) { date -> etTanggalSelesai.setText(date) } }

        // --- 5. Setup Tombol ---
        btnChooseFile.setOnClickListener { pickImageLauncher.launch("image/*") }
        btnAjukan.setOnClickListener { kirimDataCuti() }
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

        val jenisPengajuan = spinnerKeterangan.selectedItem.toString().lowercase()
        val waktuSekarangMs = System.currentTimeMillis() - 1000

        if (isTanggalMulai) {
            // Izin dan Sakit boleh hari ini
            if (jenisPengajuan == "sakit" || jenisPengajuan == "izin") {
                datePickerDialog.datePicker.minDate = waktuSekarangMs
            } else {
                // Cuti wajib minimal H-1 (besok)
                val besok = Calendar.getInstance()
                besok.add(Calendar.DAY_OF_MONTH, 1)
                datePickerDialog.datePicker.minDate = besok.timeInMillis
            }
        } else {
            // Tanggal selesai minimal hari ini
            datePickerDialog.datePicker.minDate = waktuSekarangMs
        }

        datePickerDialog.show()
    }

    private fun uriToFile(uri: Uri): File? {
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val tempFile = File.createTempFile("bukti_pengajuan", ".jpg", cacheDir)
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
        val jenisPengajuan = spinnerKeterangan.selectedItem.toString()

        if (tglMulai.isEmpty() || tglSelesai.isEmpty()) {
            Toast.makeText(this, "Tanggal Mulai & Selesai wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }
        if (alasan.isEmpty()) {
            Toast.makeText(this, "Alasan wajib diisi!", Toast.LENGTH_SHORT).show()
            return
        }

        // Validasi Frontend: Mencegah tombol submit jika Sakit tapi file kosong
        if (jenisPengajuan.equals("Sakit", ignoreCase = true) && imageFile == null) {
            Toast.makeText(this, "Surat keterangan dokter/sakit WAJIB dilampirkan!", Toast.LENGTH_LONG).show()
            return
        }

        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", "") ?: ""
        val bearerToken = "Bearer $token"

        val reqTglMulai = tglMulai.toRequestBody("text/plain".toMediaTypeOrNull())
        val reqTglSelesai = tglSelesai.toRequestBody("text/plain".toMediaTypeOrNull())
        val reqJenisCuti = jenisPengajuan.toRequestBody("text/plain".toMediaTypeOrNull())
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
                        Toast.makeText(this@PengajuanActivity, "Pengajuan berhasil dikirim!", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@PengajuanActivity, RiwayatPengajuanActivity::class.java))
                        finish()
                    } else {
                        try {
                            val errorString = response.errorBody()?.string()
                            if (errorString != null) {
                                val jsonObject = JSONObject(errorString)
                                val pesanError = jsonObject.getString("message")
                                Toast.makeText(this@PengajuanActivity, pesanError, Toast.LENGTH_LONG).show()
                            } else {
                                Toast.makeText(this@PengajuanActivity, "Gagal. Terjadi kesalahan server.", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(this@PengajuanActivity, "Gagal memproses respons: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    btnAjukan.isEnabled = true
                    btnAjukan.text = "Ajukan Sekarang"
                    Toast.makeText(this@PengajuanActivity, "Koneksi Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
    }
}