package com.example.absensi

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.LoginResponse
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    // Deklarasi variabel dengan lateinit (akan diinisialisasi nanti)
    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Hubungkan variabel dengan ID di XML
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)

        // Aksi klik tombol login
        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Username dan Password wajib diisi!", Toast.LENGTH_SHORT).show()
            } else {
                // Tembak API Login Laravel menggunakan Retrofit
                ApiConfig.getApiService().login(username, password).enqueue(object : Callback<LoginResponse> {

                    // Jika berhasil terhubung ke server
                    override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                        val body = response.body()

                        // Jika login sukses (Password benar & Akun aktif)
                        if (response.isSuccessful && body?.success == true) {
                            val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                            val editor = sharedPref.edit()

                            editor.putString("TOKEN", body.token ?: "")

                            // Simpan Data User
                            val userData = body.data
                            editor.putString("ID_USER", userData?.idUser?.toString() ?: "")
                            editor.putString("USERNAME", userData?.username ?: "")
                            editor.putString("NAMA_LENGKAP", userData?.namaLengkap ?: "")

                            // Cek apakah divisi kosong/null dari Laravel. Jika iya, beri teks sementara
                            val divisi = userData?.divisi
                            if (divisi.isNullOrEmpty()) {
                                editor.putString("DIVISI", "Divisi belum diatur")
                            } else {
                                editor.putString("DIVISI", divisi)
                            }

                            editor.apply() // Wajib agar tersimpan!

                            Toast.makeText(this@LoginActivity, body.message, Toast.LENGTH_SHORT).show()

                            // Pindah ke Dashboard
                            val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                            startActivity(intent)
                            finish()

                        } else {
                            // Jika Gagal (Password salah, Akun mati, atau Server Error)
                            try {
                                val errorBody = response.errorBody()
                                val errorString = errorBody?.string() ?: "Gagal"

                                // Mempertahankan logika cerdas Anda: JIKA ISINYA HTML (Ciri-ciri error 500 Laravel)
                                if (errorString.contains("<!DOCTYPE html>") || errorString.contains("<html>")) {
                                    Toast.makeText(this@LoginActivity, "Server sedang Error (500). Cek Laravel Log!", Toast.LENGTH_LONG).show()
                                } else {
                                    // Menampilkan pesan error dari JSON Laravel (misal: "Login Gagal: Username atau Password salah!")
                                    Toast.makeText(this@LoginActivity, body?.message ?: "Login Gagal", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this@LoginActivity, "Terjadi kesalahan sistem", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    // Jika gagal terhubung ke server sama sekali (XAMPP mati, jaringan putus, dll)
                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                        Toast.makeText(this@LoginActivity, "Koneksi Error: " + t.message, Toast.LENGTH_LONG).show()
                    }
                })
            }
        }
    }
}