package com.example.crickstats

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class Sign_In : AppCompatActivity() {
    private lateinit var databaseReference: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        const val KEY2 = "com.example.crickstats.Sign_In.name"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_in)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        // ✅ Check if user is already logged in
        if (sharedPreferences.getBoolean("isLoggedIn", false)) {
            startActivity(Intent(this, Navigator::class.java))
            finish()
        }

        val signInButton = findViewById<Button>(R.id.signbtn)
        val username = findViewById<TextInputEditText>(R.id.usernamee)
        val password = findViewById<TextInputEditText>(R.id.pass)

        signInButton.setOnClickListener {
            val uniqueID = username.text.toString().trim()
            val uniquePass = password.text.toString().trim()

            if (uniqueID.isNotEmpty() && uniquePass.isNotEmpty()) {
                readData(uniqueID, uniquePass)
            } else {
                Toast.makeText(this, "Please enter username and password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun readData(uniqueID: String, uniquePass: String) {
        databaseReference = FirebaseDatabase.getInstance().getReference("Users")

        databaseReference.child(uniqueID).get().addOnSuccessListener {
            clearFields()
            if (it.exists()) {
                val storedPass = it.child("password").value.toString()

                if (storedPass == uniquePass) {
                    // ✅ Save login session
                    with(sharedPreferences.edit()) {
                        putBoolean("isLoggedIn", true)
                        putString("username", uniqueID) // Store username if needed
                        apply()
                    }

                    val intentWelcome = Intent(this, Navigator::class.java)
                    startActivity(intentWelcome)
                    finish()
                } else {
                    Toast.makeText(this, "Incorrect password", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "User not Found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error occurred. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearFields() {
        val password = findViewById<TextInputEditText>(R.id.pass)
        password.text?.clear()
    }
}
