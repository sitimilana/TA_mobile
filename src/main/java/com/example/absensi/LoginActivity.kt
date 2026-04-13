package com.example.absensi
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.LoginResponse
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import android.content.Intent

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
                ApiConfig.getApiService().login(username, password).enqueue(object : retrofit2.Callback<LoginResponse> {

                    // Jika berhasil terhubung ke server (Walaupun password salah, tetap masuk sini selama server hidup)
                    override fun onResponse(call: retrofit2.Call<LoginResponse>, response: retrofit2.Response<LoginResponse>) {
                        // Ambil body response satu kali saja untuk efisiensi
                        val body = response.body()

                        if (response.isSuccessful && body?.success == true) {
                            // Definisikan 'data' agar bisa digunakan di bawahnya
                            val data = body.data

                            // Sekarang variabel 'data' sudah dikenali
                            val idUser = data?.user?.idUser.toString()
                            val token = data?.token
                            val namaLengkap = data?.user?.namaLengkap
                            val username = data?.user?.username

                            // 1. Simpan ke SharedPreferences
                            val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
                            val editor = sharedPref.edit()

                            editor.putString("ID_USER", idUser)
                            editor.putString("TOKEN", token)
                            editor.putString("NAMA_LENGKAP", namaLengkap)
                            editor.putString("USERNAME", username)
                            editor.apply()

                            Toast.makeText(this@LoginActivity, "Selamat Datang, $namaLengkap!", Toast.LENGTH_SHORT).show()

                            val intent = Intent(this@LoginActivity, DashboardActivity::class.java)
                            startActivity(intent)
                            finish()

                        } else {
                            try {
                                // Jangan baca seluruh string jika body-nya terlalu besar
                                val errorBody = response.errorBody()
                                val errorString = errorBody?.string() ?: "Gagal"

                                // JIKA ISINYA HTML (Ciri-ciri error 500 Laravel)
                                if (errorString.contains("<!DOCTYPE html>") || errorString.contains("<html>")) {
                                    Toast.makeText(this@LoginActivity, "Server sedang Error (500). Cek Laravel Log!", Toast.LENGTH_LONG).show()
                                } else {
                                    // Jika isinya JSON pendek biasa
                                    Toast.makeText(this@LoginActivity, "Login Gagal: Cek Username/Password", Toast.LENGTH_LONG).show()
                                }
                            } catch (e: Exception) {
                                Toast.makeText(this@LoginActivity, "Terjadi kesalahan sistem", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    // Jika gagal terhubung ke server sama sekali (XAMPP mati, jaringan putus, salah IP)
                    override fun onFailure(call: retrofit2.Call<LoginResponse>, t: Throwable) {
                        Toast.makeText(this@LoginActivity, "Koneksi Error: " + t.message, Toast.LENGTH_LONG).show()
                    }
                })
            }
        }
    }
}