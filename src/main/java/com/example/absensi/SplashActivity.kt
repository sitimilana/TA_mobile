package com.example.absensi // GANTI dengan nama package aplikasi Anda

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Handler untuk menunda eksekusi selama 2 detik (2000 ms)
        Handler(Looper.getMainLooper()).postDelayed({
            // Pindah ke halaman Login
            val intent = Intent(this@SplashActivity, LoginActivity::class.java)
            startActivity(intent)

            // Hancurkan activity splash agar tidak bisa di-back
            finish()
        }, 2000)
    }
}