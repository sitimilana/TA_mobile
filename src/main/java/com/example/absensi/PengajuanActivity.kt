package com.example.absensi

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import android.widget.ImageButton
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.ApiMessageResponse
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

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val MODE_CREATE = "create"
        const val MODE_EDIT = "edit"
        const val EXTRA_CUTI_ID = "extra_cuti_id"
        const val EXTRA_JENIS_CUTI = "extra_jenis_cuti"
        const val EXTRA_TANGGAL_MULAI = "extra_tanggal_mulai"
        const val EXTRA_TANGGAL_SELESAI = "extra_tanggal_selesai"
        const val EXTRA_ALASAN = "extra_alasan"
        const val EXTRA_KETERANGAN_PIMPINAN = "extra_keterangan_pimpinan"
    }

    private lateinit var etNama: TextInputEditText
    private lateinit var etDivisi: TextInputEditText
    private lateinit var etTanggalMulai: TextInputEditText
    private lateinit var etTanggalSelesai: TextInputEditText
    private lateinit var spinnerKeterangan: Spinner
    private lateinit var etAlasanCuti: TextInputEditText
    private lateinit var btnChooseFile: MaterialButton
    private lateinit var btnAjukan: MaterialButton

    private lateinit var btnRiwayat: MaterialButton
    private var isEditMode = false
    private var editCutiId: Int = -1
    private var isInitializingForm = false

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
        NavigationUtils.setupHeaderWithUserData(this)

        // --- 1. Inisialisasi View ---
        tvWelcomeName = findViewById(R.id.tvWelcomeName)
        tvRole = findViewById(R.id.tvRole)

        // Fitur Logout via Header
        val btnLogout: ImageButton = findViewById(R.id.btnLogout)
        btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        etNama = findViewById(R.id.etNama)
        etDivisi = findViewById(R.id.etDivisi)
        etTanggalMulai = findViewById(R.id.etTanggalMulai)
        etTanggalSelesai = findViewById(R.id.etTanggalSelesai)
        spinnerKeterangan = findViewById(R.id.spinnerKeterangan)
        etAlasanCuti = findViewById(R.id.etAlasanCuti)
        btnChooseFile = findViewById(R.id.btnChooseFile)
        btnAjukan = findViewById(R.id.btnAjukan)
        btnRiwayat = findViewById(R.id.btnRiwayat)

        layoutUpload = findViewById(R.id.layoutUpload)
        tvUploadHint = findViewById(R.id.tvUploadHint)
        tvFileName = findViewById(R.id.tvFileName)

        // --- 2. Ambil Data User dari SharedPreferences ---
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val namaLengkap = sharedPref.getString("NAMA_LENGKAP", "Karyawan")
        val divisi = sharedPref.getString("DIVISI", "Belum ada divisi")

        isEditMode = intent.getStringExtra(EXTRA_MODE) == MODE_EDIT
        editCutiId = intent.getIntExtra(EXTRA_CUTI_ID, -1)

        etNama.setText(namaLengkap)
        etDivisi.setText(divisi)

        val listKeterangan = arrayOf("Cuti", "Izin", "Sakit", "Cuti Kehamilan")
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, listKeterangan)
        spinnerKeterangan.adapter = adapterSpinner

        spinnerKeterangan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (isInitializingForm) return

                val jenisPengajuan = listKeterangan[position].lowercase()
                etTanggalMulai.setText("")
                etTanggalSelesai.setText("")
                when (jenisPengajuan) {
                    "cuti kehamilan" -> {
                        layoutUpload.visibility = View.VISIBLE
                        etTanggalSelesai.isEnabled = false
                        etTanggalSelesai.isFocusable = false
                        tvUploadHint.text = "(Wajib dilampirkan surat keterangan hamil)"
                        tvUploadHint.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                    }
                    "sakit" -> {
                        layoutUpload.visibility = View.VISIBLE
                        etTanggalSelesai.isEnabled = true
                        etTanggalSelesai.isFocusable = false
                        tvUploadHint.text = "(Wajib dilampirkan surat dokter/keterangan)"
                        tvUploadHint.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                    }
                    "izin" -> {
                        layoutUpload.visibility = View.VISIBLE
                        etTanggalSelesai.isEnabled = true
                        etTanggalSelesai.isFocusable = false
                        tvUploadHint.text = "(Opsional)"
                        tvUploadHint.setTextColor(resources.getColor(R.color.role_text, null))
                    }
                    "cuti" -> {
                        layoutUpload.visibility = View.GONE
                        // Kunci tanggal selesai agar diisi otomatis lewat logika tanggal mulai
                        etTanggalSelesai.isEnabled = false
                        imageFile = null
                        selectedImageUri = null
                        btnChooseFile.text = "Pilih File"
                        tvFileName.text = "Belum ada file yang dipilih"
                        tvFileName.setTextColor(resources.getColor(R.color.role_text, null))
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        //Tanggal Mulai
        etTanggalMulai.setOnClickListener {
            showDatePicker(true) { date ->
                etTanggalMulai.setText(date)
                val jenisPengajuan = spinnerKeterangan.selectedItem.toString().lowercase()

                when (jenisPengajuan) {
                    "cuti" -> {
                        // Cuti reguler: Tanggal selesai otomatis sama dengan tanggal mulai (1 hari)
                        etTanggalSelesai.setText(date)
                    }
                    "cuti kehamilan" -> {
                        // Cuti kehamilan: Otomatis tambah 89 hari ke depan (Total 90 hari)
                        try {
                            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                            val calendar = java.util.Calendar.getInstance()
                            calendar.time = sdf.parse(date)!!

                            // Tambah 89 hari
                            calendar.add(java.util.Calendar.DAY_OF_MONTH, 89)

                            val tanggalSelesai = sdf.format(calendar.time)
                            etTanggalSelesai.setText(tanggalSelesai)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    else -> {
                        etTanggalSelesai.setText("")
                    }
                }
            }
        }
        //Tanggal Selesai
        etTanggalSelesai.setOnClickListener {
            if (etTanggalMulai.text.toString().isEmpty()) {
                Toast.makeText(this, "Silakan pilih Tanggal Mulai terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            showDatePicker(false) { date ->
                etTanggalSelesai.setText(date)
            }
        }
        if (isEditMode) {
            val jenisEdit = intent.getStringExtra(EXTRA_JENIS_CUTI).orEmpty()
            val tanggalMulaiEdit = intent.getStringExtra(EXTRA_TANGGAL_MULAI).orEmpty()
            val tanggalSelesaiEdit = intent.getStringExtra(EXTRA_TANGGAL_SELESAI).orEmpty()
            val alasanEdit = intent.getStringExtra(EXTRA_ALASAN).orEmpty()

            isInitializingForm = true
            when {
                jenisEdit.contains("cuti kehamilan", ignoreCase = true) -> spinnerKeterangan.setSelection(3)
                jenisEdit.contains("sakit", ignoreCase = true) -> spinnerKeterangan.setSelection(2)
                jenisEdit.contains("izin", ignoreCase = true) -> spinnerKeterangan.setSelection(1)
                else -> spinnerKeterangan.setSelection(0)
            }
            etTanggalMulai.setText(tanggalMulaiEdit)
            etTanggalSelesai.setText(tanggalSelesaiEdit)
            etAlasanCuti.setText(alasanEdit)

            when {
                jenisEdit.contains("cuti kehamilan", ignoreCase = true) -> {
                    layoutUpload.visibility = View.VISIBLE
                    etTanggalSelesai.isEnabled = true
                    etTanggalSelesai.isFocusable = false
                    tvUploadHint.text = "(Wajib dilampirkan surat keterangan hamil)"
                    tvUploadHint.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                }
                jenisEdit.contains("sakit", ignoreCase = true) -> {
                    layoutUpload.visibility = View.VISIBLE
                    etTanggalSelesai.isEnabled = true
                    etTanggalSelesai.isFocusable = false
                    tvUploadHint.text = "(Wajib dilampirkan surat dokter/keterangan)"
                    tvUploadHint.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
                }
                jenisEdit.contains("izin", ignoreCase = true) -> {
                    layoutUpload.visibility = View.VISIBLE
                    etTanggalSelesai.isEnabled = true
                    etTanggalSelesai.isFocusable = false
                    tvUploadHint.text = "(Opsional)"
                    tvUploadHint.setTextColor(resources.getColor(R.color.role_text, null))
                }
                else -> {
                    layoutUpload.visibility = View.GONE
                    imageFile = null
                    selectedImageUri = null
                    btnChooseFile.text = "Pilih File"
                    tvFileName.text = "Belum ada file yang dipilih"
                    tvFileName.setTextColor(resources.getColor(R.color.role_text, null))
                }
            }

            btnAjukan.text = "Update Pengajuan"
            isInitializingForm = false
        }

        // --- 5. Setup Tombol ---
        btnChooseFile.setOnClickListener { pickImageLauncher.launch("image/*") }
        btnAjukan.setOnClickListener { kirimDataCuti() }
        btnRiwayat.setOnClickListener {
            val intent = Intent(this, RiwayatPengajuanActivity::class.java)
            startActivity(intent)
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

        val jenisPengajuan = spinnerKeterangan.selectedItem.toString().lowercase()
        val waktuSekarangMs = System.currentTimeMillis() - 1000

        if (isTanggalMulai) {
            when (jenisPengajuan) {
                "sakit", "izin" -> {
                    datePickerDialog.datePicker.minDate = waktuSekarangMs
                }
                "cuti kehamilan" -> {
                    // Wajib minimal H-10
                    val h10 = Calendar.getInstance()
                    h10.add(Calendar.DAY_OF_MONTH, 10)
                    datePickerDialog.datePicker.minDate = h10.timeInMillis
                }
                else -> {
                    // Cuti reguler wajib minimal H-1 (besok)
                    val besok = Calendar.getInstance()
                    besok.add(Calendar.DAY_OF_MONTH, 1)
                    datePickerDialog.datePicker.minDate = besok.timeInMillis
                }
            }
        }
        else {
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
        if (jenisPengajuan.equals("Cuti Kehamilan", ignoreCase = true)) {
            if (imageFile == null) {
                Toast.makeText(this, "Surat keterangan hamil WAJIB dilampirkan!", Toast.LENGTH_LONG).show()
                return
            }

            try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val dateMulai = sdf.parse(tglMulai)
                val dateSelesai = sdf.parse(tglSelesai)

                val today = java.util.Calendar.getInstance()
                today.set(java.util.Calendar.HOUR_OF_DAY, 0)
                today.set(java.util.Calendar.MINUTE, 0)
                today.set(java.util.Calendar.SECOND, 0)
                today.set(java.util.Calendar.MILLISECOND, 0)

                // Hitung selisih hari pengajuan ke hari pelaksanaan
                val diffHariAwal = (dateMulai!!.time - today.timeInMillis) / (1000 * 60 * 60 * 24)
                if (diffHariAwal < 10) {
                    Toast.makeText(this, "Cuti Kehamilan wajib diajukan minimal H-10!", Toast.LENGTH_LONG).show()
                    return
                }

                // Hitung durasi
                val durasi = ((dateSelesai!!.time - dateMulai.time) / (1000 * 60 * 60 * 24)) + 1
                if (durasi > 90) {
                    Toast.makeText(this, "Durasi maksimal Cuti Kehamilan adalah 90 hari!", Toast.LENGTH_LONG).show()
                    return
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", "") ?: ""
        val bearerToken = "Bearer $token"

        val reqTglMulai = tglMulai.toRequestBody("text/plain".toMediaTypeOrNull())
        val reqTglSelesai = tglSelesai.toRequestBody("text/plain".toMediaTypeOrNull())
        val reqJenisCuti = jenisPengajuan.toRequestBody("text/plain".toMediaTypeOrNull())
        val reqAlasan = alasan.toRequestBody("text/plain".toMediaTypeOrNull())
        val reqMethodOverride = "PUT".toRequestBody("text/plain".toMediaTypeOrNull())

        var fotoMultipart: MultipartBody.Part? = null
        if (imageFile != null) {
            val reqImageFile = imageFile!!.asRequestBody("image/jpeg".toMediaTypeOrNull())
            fotoMultipart = MultipartBody.Part.createFormData("berkas_bukti", imageFile!!.name, reqImageFile)
        }

        btnAjukan.isEnabled = false
        btnAjukan.text = if (isEditMode) "Mengupdate..." else "Mengirim..."

        if (isEditMode && editCutiId > 0) {
            ApiConfig.getApiService().updateCuti(
                bearerToken,
                editCutiId,
                reqMethodOverride,
                reqTglMulai,
                reqTglSelesai,
                reqJenisCuti,
                reqAlasan,
                fotoMultipart
            ).enqueue(object : Callback<ApiMessageResponse> {
                override fun onResponse(call: Call<ApiMessageResponse>, response: Response<ApiMessageResponse>) {
                    btnAjukan.isEnabled = true
                    btnAjukan.text = "Update Pengajuan"

                    if (response.isSuccessful) {
                        Toast.makeText(this@PengajuanActivity, response.body()?.message ?: "Pengajuan berhasil diperbarui!", Toast.LENGTH_LONG).show()
                        startActivity(Intent(this@PengajuanActivity, RiwayatPengajuanActivity::class.java))
                        finish()
                    } else {
                        try {
                            val errorString = response.errorBody()?.string()
                            val pesanError = if (!errorString.isNullOrEmpty()) {
                                JSONObject(errorString).optString("message", "Gagal memproses update.")
                            } else {
                                "Gagal. Terjadi kesalahan server."
                            }
                            Toast.makeText(this@PengajuanActivity, pesanError, Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            Toast.makeText(this@PengajuanActivity, "Gagal memproses respons: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                override fun onFailure(call: Call<ApiMessageResponse>, t: Throwable) {
                    btnAjukan.isEnabled = true
                    btnAjukan.text = "Update Pengajuan"
                    Toast.makeText(this@PengajuanActivity, "Koneksi Error: ${t.message}", Toast.LENGTH_LONG).show()
                }
            })
        } else {
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

    // Fungsi Konfirmasi Logout dengan Dialog
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

    // Fungsi Eksekusi Membersihkan Sesi dan Kembali ke Login
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