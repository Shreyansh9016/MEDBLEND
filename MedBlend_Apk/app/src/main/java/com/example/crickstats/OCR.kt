package com.example.crickstats

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class OCR : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var imageCapture: ImageCapture
    private lateinit var previewView: PreviewView
    private lateinit var resultText: TextView
    private lateinit var recognizer: TextRecognizer
    private var detectedMedicines: MutableList<String> = mutableListOf()


    private lateinit var tts: TextToSpeech
    private var isTTSReady = false
    private val medicineKeywords = listOf("tab", "cap", "mg", "ml", "g", "inj")
    private val medicineSuffixes = listOf("cin", "micin", "pril", "olol", "pram", "azole")
    private val commonMeds = listOf(
        "paracetamol", "amlodipine", "insulin", "metformin",
        "atenolol", "omeprazole", "simvastatin"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_ocr)

        previewView = findViewById(R.id.camera_preview)
        resultText = findViewById(R.id.result_text)
        val captureButton = findViewById<Button>(R.id.capture_button)
        recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                REQUIRED_PERMISSIONS,
                REQUEST_CODE_PERMISSIONS
            )
        }

        captureButton.setOnClickListener {
            takePhoto()
        }

        cameraExecutor = Executors.newSingleThreadExecutor()


        tts = TextToSpeech(this, this)
    }
    private fun takePhoto() {
        imageCapture.takePicture(
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val bitmap = image.toBitmap()
                    processImage(bitmap)
                    image.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.e(TAG, "Capture failed: ${exception.message}", exception)
                }
            }
        )
    }


    private fun processImage(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->   
                detectedMedicines = filterMedicineNames(visionText.textBlocks).toMutableList()
                val resultString = detectedMedicines.joinToString("\n")

                resultText.text = if (detectedMedicines.isEmpty()) {
                    "No medicines detected. Try a clearer photo."
                } else {
                    "Detected Medicines:\n$resultString"
                }

                speakOut(resultString)
                showMedicineConfirmationDialog(detectedMedicines) // NEW: Add confirmation dialog
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "OCR failed: ${e.message}")
                Toast.makeText(this, "OCR failed! Try again.", Toast.LENGTH_SHORT).show()
            }
    }

    // NEW: Medicine filtering logic
    private fun filterMedicineNames(textBlocks: List<Text.TextBlock>): List<String> {
        return textBlocks.flatMap { block ->
            block.lines.filter { line ->
                val text = line.text.lowercase()
                // Match medicine patterns
                medicineSuffixes.any { text.endsWith(it) } ||
                        medicineKeywords.any { text.contains(it) } ||
                        commonMeds.any { text.contains(it) }
            }.map { it.text }
        }.distinct()
    }

    private fun showFinalDialog(medicines: List<String>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_final_medicine_list, null)
        val listView = dialogView.findViewById<ListView>(R.id.finalListView)
        val btnProceed = dialogView.findViewById<Button>(R.id.btnProceed)

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, medicines)
        listView.adapter = adapter

        val dialog = AlertDialog.Builder(this)
            .setTitle("Medicines Confirmed")
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnProceed.setOnClickListener {
            val selectedMedicine = medicines.joinToString(", ") // or pick one, depends on you
            val intent = Intent(this, AlarmActivity::class.java)
            intent.putStringArrayListExtra("medicines", ArrayList(detectedMedicines))
            startActivity(intent)
            dialog.dismiss()
        }

        dialog.show()
    }

    // NEW: Medicine confirmation dialog
//    private fun showMedicineConfirmationDialog(medicines: List<String>) {
//        AlertDialog.Builder(this)
//            .setTitle("Confirm Medicines")
//            .setMessage(medicines.joinToString("\n"))
//            .setPositiveButton("Confirm") { _, _ -> saveToDatabase(medicines) }
//            .setNegativeButton("Edit") { _, _ -> enableTextEditing() }
//            .show()
//    }
    private fun showMedicineConfirmationDialog(medicines: List<String>) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_medicine_confirmation, null)
        val listView = dialogView.findViewById<ListView>(R.id.listView)

        val adapter = object : ArrayAdapter<String>(this, R.layout.list_item_medicine, medicines) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val view = convertView ?: layoutInflater.inflate(
                    R.layout.list_item_medicine,
                    parent,
                    false
                )
                val medicineName = view.findViewById<TextView>(R.id.tvMedicineName)
                val btnAction = view.findViewById<Button>(R.id.btnAction)

                medicineName.text = getItem(position)

                btnAction.setOnClickListener {
                    showEditDialogForSingleMedicine(position, getItem(position)!!)
                }

                return view
            }
        }

        listView.adapter = adapter

        AlertDialog.Builder(this)
            .setTitle("Confirm Each Medicine")
            .setView(dialogView)
            .setPositiveButton("Confirm") { _, _ ->
                saveToDatabase(medicines)
                showFinalDialog(medicines) // 👉 After confirming, show final dialog
            }
            .setNegativeButton("Cancel", null)
            .show()
    }


    private fun showEditDialogForSingleMedicine(position: Int, currentName: String) {
        val input = EditText(this).apply {
            setText(currentName)
            hint = "Enter medicine name"
            setSingleLine(true)
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Medicine")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text.toString().trim()
                if (newName.isNotEmpty()) {
                    detectedMedicines = detectedMedicines.toMutableList().apply {
                        set(position, newName)
                    }
                    showMedicineConfirmationDialog(detectedMedicines)
                }
            }
            .setNegativeButton("Cancel") { _, _ -> }
            .show()
    }
    // Add to OCR class
    private fun enableTextEditing() {
        val input = android.widget.EditText(this).apply {
            setText(resultText.text.toString().substringAfter(":\n").replace("\n", ", "))
            hint = "Edit detected medicines (comma separated)"
            setSingleLine(false)
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Medicines")
            .setView(input)
            .setPositiveButton("Save") { _, _ ->
                val editedText = input.text.toString()
                val medicines = editedText.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                if (medicines.isNotEmpty()) {
                    resultText.text = "Detected Medicines:\n${medicines.joinToString("\n")}"
                    saveToDatabase(medicines)
                } else {
                    Toast.makeText(this, "No medicines entered!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel") { _, _ -> }
            .show()
    }
    private fun saveToDatabase(medicines: List<String>) {
        // Implement your database logic here
        Toast.makeText(this, "${medicines.size} medicines saved!", Toast.LENGTH_SHORT).show()
    }

    // NEW: Complete TTS implementation
    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.getDefault()
            isTTSReady = true
            speakOut("Point your camera at a prescription")
        }
    }
    // Camera setup functions
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = androidx.camera.core.Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    this,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch(exc: Exception) {
                Log.e(TAG, "Camera binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }


    private fun speakOut(text: String) {
        if (isTTSReady) {
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    // NEW: Improved permission handling
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(
                        this,
                        Manifest.permission.CAMERA
                    )
                ) {
                    AlertDialog.Builder(this)
                        .setTitle("Permission Needed")
                        .setMessage("Camera access is required to scan prescriptions")
                        .setPositiveButton("Grant") { _, _ ->
                            ActivityCompat.requestPermissions(
                                this,
                                REQUIRED_PERMISSIONS,
                                REQUEST_CODE_PERMISSIONS
                            )
                        }
                        .setNegativeButton("Cancel") { _, _ -> finish() }
                        .show()
                } else {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                }
            }
        }
    }
    // Permission handling
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        // NEW: Clean up TTS
        tts.stop()
        tts.shutdown()
    }

    companion object {
        private const val TAG = "OCR_Activity"
        private const val REQUEST_CODE_PERMISSIONS = 101
        private val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.INTERNET // For API calls if added later
        )
    }
}