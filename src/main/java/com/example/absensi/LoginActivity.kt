package com.example.absensi // GANTI dengan nama package aplikasi Anda

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.absensi.network.ApiConfig
import com.example.absensi.network.LoginResponse
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

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
                        if (response.isSuccessful && response.body()?.success == true) {
                            val token = response.body()?.data?.token
                            Toast.makeText(this@LoginActivity, "Login Berhasil! Token: $token", Toast.LENGTH_LONG).show()

                            // Nanti pindah ke dashboard di sini

                        } else {
                            // PERBAIKAN: Baca pesan error ASLI dari Laravel
                            try {
                                val errorBody = response.errorBody()?.string()
                                Toast.makeText(this@LoginActivity, "Server Menolak: $errorBody", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(this@LoginActivity, "Error tidak diketahui", Toast.LENGTH_SHORT).show()
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