package com.example.crickstats

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.example.crickstats.databinding.ActivityBpdiabeteseBinding
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import java.text.SimpleDateFormat
import java.util.*

class BPDiabetese : AppCompatActivity() {

    private lateinit var binding: ActivityBpdiabeteseBinding
    private lateinit var database: DatabaseReference
    private lateinit var lineChart: LineChart

    // Add SharedPreferences
    private lateinit var sharedPreferences: SharedPreferences

    // Days in correct order for display
    private val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
    private val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())
    private var currentDate = dateFormat.format(Date())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBpdiabeteseBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize SharedPreferences
        sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val username = sharedPreferences.getString("username", "") ?: run {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Edge-to-edge setup
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        database = FirebaseDatabase.getInstance().reference
        lineChart = binding.lineChart

        initializeChart()
        setupDateNavigation()
        setupSaveButton()
        setupRadioGroup()
        loadChartData(username) // Pass username to load data
    }

    private fun initializeChart() {
        with(lineChart) {
            // Basic chart setup
            description.isEnabled = true
            description.text = "Weekly Health Data"
            setTouchEnabled(true)
            isDragEnabled = true
            setScaleEnabled(true)
            setPinchZoom(true)

            // X-axis setup
            xAxis.position = XAxis.XAxisPosition.BOTTOM
            xAxis.setDrawGridLines(false)
            xAxis.granularity = 1f
            xAxis.labelCount = 7

            // Y-axis setup
            axisLeft.setDrawGridLines(true)
            axisRight.isEnabled = false

            // Legend
            legend.isEnabled = true
            legend.textColor = Color.BLACK

            // No data text
            setNoDataText("No data available. Save some measurements!")
            setNoDataTextColor(Color.RED)
        }
    }

    private fun setupDateNavigation() {
        binding.btnPreviousDate.setOnClickListener { changeDate(-1) }
        binding.btnNextDate.setOnClickListener { changeDate(1) }
        updateDateDisplay()
    }

    private fun changeDate(days: Int) {
        val calendar = Calendar.getInstance()
        calendar.time = dateFormat.parse(currentDate) ?: Date()
        calendar.add(Calendar.DAY_OF_YEAR, days)
        currentDate = dateFormat.format(calendar.time)
        updateDateDisplay()
    }

    private fun updateDateDisplay() {
        val displayFormat = SimpleDateFormat("EEE, MMM d", Locale.getDefault())
        val date = dateFormat.parse(currentDate) ?: Date()
        binding.tvSelectedDate.text = displayFormat.format(date)
    }

    private fun setupRadioGroup() {
        binding.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val username = sharedPreferences.getString("username", "") ?: return@setOnCheckedChangeListener
            loadChartData(username)
        }
    }

    private fun setupSaveButton() {
        binding.btnSave.setOnClickListener {
            saveHealthData()
        }
    }

    private fun saveHealthData() {
        val systolic = binding.etSystolic.text.toString().toIntOrNull()
        val diastolic = binding.etDiastolic.text.toString().toIntOrNull()
        val pulse = binding.etPulse.text.toString().toIntOrNull()
        val preMeal = binding.etPreMeal.text.toString().toFloatOrNull()?.toInt()
        val postMeal = binding.etPostMeal.text.toString().toFloatOrNull()?.toInt()
        val notes = binding.etNotes.text.toString()

        if (systolic == null || diastolic == null || pulse == null) {
            Toast.makeText(this, "Please enter valid BP values", Toast.LENGTH_SHORT).show()
            return
        }

        val username = sharedPreferences.getString("username", "") ?: run {
            Toast.makeText(this, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        val dayName = dayFormat.format(dateFormat.parse(currentDate) ?: Date())
        val healthData = hashMapOf<String, Any>(
            "bp" to systolic.toString(),
            "timestamp" to System.currentTimeMillis()
        )

        preMeal?.let { healthData["diabetesPre"] = it.toString() }
        postMeal?.let { healthData["diabetesPost"] = it.toString() }
        notes.takeIf { it.isNotEmpty() }?.let { healthData["notes"] = it }

        // Save to the correct path in your structure
        database.child("Users").child(username).child("weeklyHealthData").child(dayName)
            .setValue(healthData)
            .addOnSuccessListener {
                Toast.makeText(this, "Data saved successfully", Toast.LENGTH_SHORT).show()
                loadChartData(username)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadChartData(username: String) {
        Log.d("ChartDebug", "Loading data for username: $username")
        database.child("Users").child(username).child("weeklyHealthData").get()
            .addOnSuccessListener { snapshot ->
                Log.d("ChartDebug", "Data snapshot: ${snapshot.value}")

                val entriesBP = mutableListOf<Entry>()
                val entriesSugar = mutableListOf<Entry>()
                val labels = mutableListOf<String>()

                // Process each day in correct order
                daysOfWeek.forEachIndexed { index, day ->
                    val dayData = snapshot.child(day)
                    if (dayData.exists()) {
                        // Blood Pressure
                        dayData.child("bp").getValue(String::class.java)?.toFloatOrNull()?.let { bp ->
                            entriesBP.add(Entry(index.toFloat(), bp))
                        }

                        // Blood Sugar (prefer pre-meal)
                        dayData.child("diabetesPre").getValue(String::class.java)?.toFloatOrNull()
                            ?: dayData.child("diabetesPost").getValue(String::class.java)?.toFloatOrNull()
                                ?.let { sugar ->
                                    entriesSugar.add(Entry(index.toFloat(), sugar))
                                }
                    }
                    labels.add(day)
                }

                updateChart(entriesBP, entriesSugar, labels)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show()
                Log.e("ChartDebug", "Error loading data", it)
            }
    }

    private fun updateChart(
        bpEntries: List<Entry>,
        sugarEntries: List<Entry>,
        labels: List<String>
    ) {
        val dataSets = mutableListOf<ILineDataSet>()

        if (bpEntries.isNotEmpty()) {
            dataSets.add(LineDataSet(bpEntries, "Blood Pressure").apply {
                color = Color.BLUE
                valueTextColor = Color.BLACK
                lineWidth = 2.5f
                setCircleColor(Color.BLUE)
                circleRadius = 4f
                setDrawValues(true)
                valueTextSize = 10f
            })
        }

        if (sugarEntries.isNotEmpty()) {
            dataSets.add(LineDataSet(sugarEntries, "Blood Sugar").apply {
                color = Color.GREEN
                valueTextColor = Color.BLACK
                lineWidth = 2.5f
                setCircleColor(Color.GREEN)
                circleRadius = 4f
                setDrawValues(true)
                valueTextSize = 10f
            })
        }

        if (dataSets.isNotEmpty()) {
            lineChart.apply {
                data = LineData(dataSets)
                xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                invalidate()
                animateY(1000)
            }
        } else {
            lineChart.clear()
            lineChart.invalidate()
        }
    }
}