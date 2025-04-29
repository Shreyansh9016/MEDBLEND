package com.example.crickstats

import Medicine
import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.Calendar

class AlarmActivity : AppCompatActivity() {

    private lateinit var medicineContainer: LinearLayout
    private lateinit var addMedicineButton: FloatingActionButton
    private lateinit var saveMedicinesButton: Button
    private lateinit var database: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var username: String
    private lateinit var alarmManager: AlarmManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_alarm)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotificationPermission()
        }


        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        initializeComponents()
        loadInitialMedicines()
        setupClickListeners()
    }

    private fun initializeComponents() {
        database = FirebaseDatabase.getInstance().getReference("Users")
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        username = sharedPreferences.getString("username", "").toString()
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        medicineContainer = findViewById(R.id.medicineContainer)
        addMedicineButton = findViewById(R.id.addMedicineButton)
        saveMedicinesButton = findViewById(R.id.saveMedicinesButton)
    }

    private fun loadInitialMedicines() {
        intent.getStringExtra("medicine_name")?.let {
            addMedicineCardWithName(it)
        }
        loadMedicinesFromFirebase()
    }

    private fun setupClickListeners() {
        addMedicineButton.setOnClickListener { addMedicineCard() }
        saveMedicinesButton.setOnClickListener { saveAllMedicines() }
    }

    private fun saveAllMedicines() {
        val medicines = getCurrentMedicinesList()
        database.child(username).child("medicines").setValue(medicines)
            .addOnSuccessListener {
                medicines.forEach { medicine ->
                    medicine.times.forEach { time ->
                        scheduleAlarm(medicine.medicineName, time)
                    }
                }
                Toast.makeText(this, "All medicines saved!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Save failed: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getCurrentMedicinesList(): List<Medicine> {
        return (0 until medicineContainer.childCount).mapNotNull { i ->
            val cardView = medicineContainer.getChildAt(i) as? CardView
            cardView?.let { createMedicineFromCard(it) }
        }
    }
    private fun requestNotificationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            NOTIFICATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() &&
                    grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(
                        this,
                        "Notifications disabled! Alarms won't work properly",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }


    private fun createMedicineFromCard(cardView: CardView): Medicine? {
        val name = cardView.findViewById<TextView>(R.id.medicineName).text.toString()
        val doses = cardView.findViewById<EditText>(R.id.noOfDosesEditText).text.toString().toIntOrNull() ?: 0
        val times = (0 until cardView.findViewById<LinearLayout>(R.id.timesContainer).childCount)
            .map { i -> (cardView.findViewById<LinearLayout>(R.id.timesContainer).getChildAt(i) as TextView).text.toString() }

        return if (name.isNotEmpty() && doses > 0) Medicine(name, doses, times) else null
    }

    private fun loadMedicinesFromFirebase() {
        database.child(username).child("medicines").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                medicineContainer.removeAllViews()
                snapshot.children.forEach { medicineSnapshot ->
                    medicineSnapshot.getValue(Medicine::class.java)?.let { medicine ->
                        addMedicineCardFromData(medicine)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@AlarmActivity, "Load failed: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun addMedicineCard() {
        val cardView = LayoutInflater.from(this).inflate(R.layout.medicine_card, null) as CardView
        setupCardView(cardView, "")
        medicineContainer.addView(cardView)
    }

    private fun addMedicineCardWithName(name: String) {
        val cardView = LayoutInflater.from(this).inflate(R.layout.medicine_card, null) as CardView
        setupCardView(cardView, name)
        medicineContainer.addView(cardView)
    }

    private fun addMedicineCardFromData(medicine: Medicine) {
        val cardView = LayoutInflater.from(this).inflate(R.layout.medicine_card, null) as CardView
        setupCardView(cardView, medicine.medicineName)

        cardView.findViewById<EditText>(R.id.noOfDosesEditText).setText(medicine.noOfDoses.toString())
        val timesContainer = cardView.findViewById<LinearLayout>(R.id.timesContainer)
        medicine.times.forEach { time ->
            TextView(this).apply {
                text = time
                textSize = 18f
                setPadding(8, 8, 8, 8)
                timesContainer.addView(this)
            }
        }
        medicineContainer.addView(cardView)
    }

    private fun setupCardView(cardView: CardView, medicineName: String) {
        with(cardView) {
            findViewById<TextView>(R.id.medicineName).text = medicineName

            findViewById<Button>(R.id.saveButton).setOnClickListener {
                createMedicineFromCard(cardView)?.let { medicine ->
                    database.child(username).child("medicines").child(medicine.medicineName).setValue(medicine)
                        .addOnSuccessListener {
                            medicine.times.forEach { time ->
                                scheduleAlarm(medicine.medicineName, time)
                            }
                            Toast.makeText(this@AlarmActivity, "${medicine.medicineName} saved!", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this@AlarmActivity, "Save failed: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }

            findViewById<Button>(R.id.deleteButton).setOnClickListener {
                AlertDialog.Builder(this@AlarmActivity)
                    .setTitle("Delete Medicine")
                    .setMessage("Delete ${findViewById<TextView>(R.id.medicineName).text}?")
                    .setPositiveButton("Delete") { _, _ ->
                        val medicineName = findViewById<TextView>(R.id.medicineName).text.toString()
                        val times = (0 until findViewById<LinearLayout>(R.id.timesContainer).childCount)
                            .map { i -> (findViewById<LinearLayout>(R.id.timesContainer).getChildAt(i) as TextView).text.toString() }

                        cancelAlarms(medicineName, times)
                        database.child(username).child("medicines").child(medicineName).removeValue()
                        medicineContainer.removeView(cardView)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }

            findViewById<Button>(R.id.addTimeButton).setOnClickListener {
                val doses = findViewById<EditText>(R.id.noOfDosesEditText).text.toString().toIntOrNull() ?: 0
                findViewById<LinearLayout>(R.id.timesContainer).removeAllViews()
                repeat(doses) { openTimePicker(findViewById(R.id.timesContainer)) }
            }
        }
    }
    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE=1001
    }

    private fun openTimePicker(container: LinearLayout) {
        Calendar.getInstance().apply {
            TimePickerDialog(this@AlarmActivity, { _, hour, minute ->
                TextView(this@AlarmActivity).apply {
                    text = String.format("%02d:%02d", hour, minute)
                    textSize = 18f
                    setPadding(8, 8, 8, 8)
                    container.addView(this)
                }
            }, get(Calendar.HOUR_OF_DAY), get(Calendar.MINUTE), true).show()
        }
    }

    private fun scheduleAlarm(medicineName: String, time: String) {
        val originalTime = time
        scheduleSingleAlarm(medicineName, originalTime, false)

        // Schedule follow-up alarm 1 minute later
        val followUpTime = getTimePlusOneMinute(time)
        scheduleSingleAlarm(medicineName, followUpTime, true)
    }
    private fun scheduleSingleAlarm(medicineName: String, time: String, isFollowUp: Boolean) {
        val (hour, minute) = time.split(":").map { it.toInt() }
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DATE, 1)
            }
        }

        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("medicineName", medicineName)
            putExtra("originalTime", time)
            putExtra("isFollowUp", isFollowUp)
        }

        val requestCode = "$medicineName$time${if (isFollowUp) "FU" else ""}".hashCode()
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExact(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            pendingIntent
        )
    }
    private fun getTimePlusOneMinute(time: String): String {
        val (hour, minute) = time.split(":").map { it.toInt() }
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            add(Calendar.MINUTE, 1)
        }
        return String.format("%02d:%02d", calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
    }


    private fun cancelAlarms(medicineName: String, times: List<String>) {
        times.forEach { time ->
            val intent = Intent(this, AlarmReceiver::class.java)
            val requestCode = "$medicineName$time".hashCode()
            val pendingIntent = PendingIntent.getBroadcast(
                this,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )

            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
            }
        }
    }

    private lateinit var medicinesListener: ValueEventListener

    override fun onStart() {
        super.onStart()
        medicinesListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Handle data
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        }
        database.child(username).child("medicines").addValueEventListener(medicinesListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        database.child(username).child("medicines").removeEventListener(medicinesListener)
    }

}

class AlarmReceiver : BroadcastReceiver() {
    private var ringtone: Ringtone? = null

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override fun onReceive(context: Context, intent: Intent) {
        val medicineName = intent.getStringExtra("medicineName") ?: return
        val originalTime = intent.getStringExtra("originalTime") ?: return
        val isFollowUp = intent.getBooleanExtra("isFollowUp", false)

        // Start persistent notification with stop control
        showPersistentNotification(context, medicineName, originalTime)
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "medicine_channel",
                "Medicine Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Channel for medicine reminders"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showPersistentNotification(context: Context, medicineName: String, originalTime: String) {
        val notificationManager = NotificationManagerCompat.from(context)
        createNotificationChannel(context)

        // Create stop intent
        val stopIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "STOP_ALARM"
            putExtra("medicineName", medicineName)
            putExtra("originalTime", originalTime)
        }

        val pendingStopIntent = PendingIntent.getBroadcast(
            context,
            (medicineName + originalTime).hashCode(),
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build notification
        val notification = NotificationCompat.Builder(context, "medicine_channel")
            .setContentTitle("Time to take $medicineName")
            .setContentText("Alarm active - Take your medicine now!")
            .setSmallIcon(R.drawable.ic_pill_notification)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .addAction(
                R.drawable.ic_check,
                "Mark as Taken",
                pendingStopIntent
            )
            .build()

        // Start ringtone
        playAlarmSound(context)
        notificationManager.notify(medicineName.hashCode(), notification)
    }

    private fun playAlarmSound(context: Context) {
        val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        ringtone = RingtoneManager.getRingtone(context, alarmSound).apply {
            play()
        }
    }

    private fun stopAlarm() {
        ringtone?.stop()
    }
}
