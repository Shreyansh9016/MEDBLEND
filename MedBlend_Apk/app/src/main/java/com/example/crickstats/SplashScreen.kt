package com.example.crickstats

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SplashScreen : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splashscreen)
       val btn1=findViewById<Button>(R.id.getstarted)
        btn1.setOnClickListener {
            val intentwelcome = Intent(this, Sign_Up::class.java)
            startActivity(intentwelcome)
        }
    }
}