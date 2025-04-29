package com.example.crickstats

import User
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class Sign_Up : AppCompatActivity() {
    private lateinit var database: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_sign_up)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)

        // ✅ If user is already logged in, go to Navigator and skip signup
        if (sharedPreferences.getBoolean("isLoggedIn", false)) {
            startActivity(Intent(this, Navigator::class.java))
            finish()
        }

        val signupbtn = findViewById<Button>(R.id.btn)
        val signIn = findViewById<TextView>(R.id.signbtn)

        signupbtn.setOnClickListener {
            val name = findViewById<TextInputEditText>(R.id.name).text.toString()
            val username = findViewById<TextInputEditText>(R.id.username).text.toString()
            val password = findViewById<TextInputEditText>(R.id.password).text.toString()
            val email = findViewById<TextInputEditText>(R.id.email).text.toString()

            if (name.isNotEmpty() && username.isNotEmpty() && password.isNotEmpty() && email.isNotEmpty()) {
                checkIfUserExists(username, name, password, email)
            } else {
                Toast.makeText(this, "Enter all the required details", Toast.LENGTH_SHORT).show()
            }
        }

        signIn.setOnClickListener {
            val intent = Intent(this, Sign_In::class.java)
            startActivity(intent)
        }
    }

    private fun checkIfUserExists(username: String, name: String, password: String, email: String) {
        database = FirebaseDatabase.getInstance().getReference("Users")

        database.child(username).get().addOnSuccessListener {
            if (it.exists()) {
                Toast.makeText(this, "Username already exists! Try a different one.", Toast.LENGTH_SHORT).show()
                clearFields()
            } else {
                showConfirmationDialog(username, name, password, email)
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error checking username. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showConfirmationDialog(username: String, name: String, password: String, email: String) {
        val builder1 = AlertDialog.Builder(this)
        builder1.setTitle("Are you sure?")
        builder1.setMessage("Do you want to continue?")
        builder1.setIcon(R.drawable.baseline_double_arrow_24)

        builder1.setPositiveButton("Yes") { _, _ ->
            registerUser(username, name, password, email)
        }
        builder1.setNegativeButton("No", null)
        clearFields()

        val dialog = builder1.create()
        dialog.setOnShowListener {
            dialog.window?.setBackgroundDrawableResource(R.drawable.background) // Set custom drawable
        }
        dialog.show()

    }

    private fun registerUser(username: String, name: String, password: String, email: String) {
        val user = User(username.trim(), name.trim(), password.trim(), email.trim())
        database.child(username).setValue(user).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                saveLoginState(username)
                clearFields()
                Toast.makeText(this, "User Registered Successfully!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, Navigator::class.java))
                finish()
            } else {
                Toast.makeText(this, "Registration Failed. Try Again.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveLoginState(username: String) {
        with(sharedPreferences.edit()) {
            putBoolean("isLoggedIn", true)
            putString("username", username)
            apply()
        }
    }

    private fun clearFields() {
        findViewById<TextInputEditText>(R.id.name).text?.clear()
        findViewById<TextInputEditText>(R.id.username).text?.clear()
        findViewById<TextInputEditText>(R.id.password).text?.clear()
        findViewById<TextInputEditText>(R.id.email).text?.clear()
    }
}
