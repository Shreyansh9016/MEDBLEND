package com.example.crickstats

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class Navigator : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_navigator)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        // ✅ Ensure user is logged in
        if (!sharedPreferences.getBoolean("isLoggedIn", false)) {
            startActivity(Intent(this, Sign_In::class.java))
            finish()
        }

        val btn0 = findViewById<ImageButton>(R.id.careButton)
        val btn1 = findViewById<Button>(R.id.seriesButton)
        val btn2 = findViewById<Button>(R.id.playersButton)
        val btn3 = findViewById<Button>(R.id.teamsButton)
        val btn4 = findViewById<Button>(R.id.statsButton)
        val logout = findViewById<ImageButton>(R.id.logoutButton)

        btn0.setOnClickListener {
            startActivity(Intent(this, CareGiver::class.java))
        }
        btn1.setOnClickListener {
            startActivity(Intent(this, AlarmActivity::class.java))
        }
        btn2.setOnClickListener {
            startActivity(Intent(this, Searching::class.java))
        }
        btn3.setOnClickListener {
            startActivity(Intent(this, OCR::class.java))
        }
        btn4.setOnClickListener {
            startActivity(Intent(this, BPDiabetese::class.java))
        }

        // ✅ Logout functionality
        logout.setOnClickListener {
            with(sharedPreferences.edit()) {
                putBoolean("isLoggedIn", false)
                apply()
            }
            startActivity(Intent(this, Sign_Up::class.java))
            finish()
        }
    }
}
