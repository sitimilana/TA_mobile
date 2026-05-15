package com.example.absensi

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.SalaryDetailResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class DetailGajiActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_gaji)

        // Atur Bottom Nav dan Header (Jika digunakan di halaman ini)
        NavigationUtils.setupBottomNav(this)
        NavigationUtils.setupHeaderWithUserData(this)

        // Ambil ID Gaji yang dikirim dari SlipGajiAdapter
        val idGaji = intent.getIntExtra("ID_GAJI", -1)

        if (idGaji != -1) {
            fetchDetailGaji(idGaji)
        } else {
            Toast.makeText(this, "ID Gaji tidak valid", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun fetchDetailGaji(idGaji: Int) {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val token = sharedPref.getString("TOKEN", "") ?: ""

        // Memanggil API getDetailSlipGaji (Pastikan endpointnya benar, misal "api/gaji/{id}")
        ApiConfig.getApiService().getDetailSlipGaji("Bearer $token", idGaji)
            .enqueue(object : Callback<SalaryDetailResponse> {
                override fun onResponse(
                    call: Call<SalaryDetailResponse>,
                    response: Response<SalaryDetailResponse>
                ) {
                    if (response.isSuccessful && response.body() != null) {
                        bindDataToViews(response.body()!!)
                    } else {
                        Toast.makeText(this@DetailGajiActivity, "Gagal memuat detail gaji.", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<SalaryDetailResponse>, t: Throwable) {
                    Toast.makeText(this@DetailGajiActivity, "Koneksi Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun bindDataToViews(data: SalaryDetailResponse) {
        // Mengikat data Header
        findViewById<TextView>(R.id.tvPeriodeDetail).text = "Periode: ${data.periode ?: "-"}"

        // Mengikat Data Penerimaan
        val penerimaan = data.penerimaan
        findViewById<TextView>(R.id.tvValGajiPokok).text = formatRupiah(penerimaan?.gajiPokok)
        findViewById<TextView>(R.id.tvValUangMakan).text = formatRupiah(penerimaan?.uangMakan)
        findViewById<TextView>(R.id.tvValLeaderKursus).text = formatRupiah(penerimaan?.tunjanganJabatan)
        findViewById<TextView>(R.id.tvValKinerja).text = formatRupiah(penerimaan?.insentifKinerja)
        findViewById<TextView>(R.id.tvValProgram).text = formatRupiah(penerimaan?.tunjanganProgram)
        // findViewById<TextView>(R.id.tvValBpjsPenerimaan) <- DIHAPUS
        findViewById<TextView>(R.id.tvValTunjLainnya).text = formatRupiah(penerimaan?.lainLain)
        findViewById<TextView>(R.id.tvValBonus).text = formatRupiah(penerimaan?.bonus)

        // Total Penerimaan
        findViewById<TextView>(R.id.tvValTotalPenerimaan).text = formatRupiah(penerimaan?.totalPenerimaan)

        // Mengikat Data Potongan
        val potongan = data.potongan
        findViewById<TextView>(R.id.tvValPotAbsen).text = formatRupiah(potongan?.potonganAbsen)
        findViewById<TextView>(R.id.tvValCashBon1).text = formatRupiah(potongan?.cashBon)
        // Jika API Laravel tidak mengirimkan data cash_bon_2 di respon getDetailSlipGaji, ini di set ke 0
        // Atau jika Anda sudah menambahkannya ke JSON, panggil di sini
        findViewById<TextView>(R.id.tvValCashBon2).text = formatRupiah(potongan?.cashBon2)
        findViewById<TextView>(R.id.tvValBpjsPotongan).text = formatRupiah(potongan?.potonganBpjs)
        findViewById<TextView>(R.id.tvValPotLain).text = formatRupiah(potongan?.potonganLain) // TAMBAHAN BARU

        // Total Potongan (Ditambah dengan potonganLain)
        val totalPotongan = (potongan?.potonganAbsen ?: 0) + (potongan?.cashBon ?: 0) + (potongan?.cashBon2 ?: 0) + (potongan?.potonganBpjs ?: 0) + (potongan?.potonganLain ?: 0)
        findViewById<TextView>(R.id.tvValTotalPotongan).text = formatRupiah(totalPotongan)

        // Take Home Pay (Gaji Bersih)
        findViewById<TextView>(R.id.tvNetSalaryDetail).text = formatRupiah(data.totalGaji)
    }

    private fun formatRupiah(number: Int?): String {
        if (number == null) return "Rp 0"
        val formatRupiah = NumberFormat.getCurrencyInstance(Locale("in", "ID"))
        return formatRupiah.format(number).replace("Rp", "Rp ")
    }
}