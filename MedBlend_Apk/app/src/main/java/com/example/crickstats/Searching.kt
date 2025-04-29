package com.example.crickstats

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class Searching : AppCompatActivity() {
    private lateinit var searchInput: EditText
    private lateinit var searchButton: Button
    private lateinit var loadingLayout: LinearLayout
    private lateinit var resultCard: CardView
    private lateinit var resultContainer: LinearLayout
    private lateinit var errorText: TextView

    private var useGreen = true // For alternating colors

    private val client = OkHttpClient()
    private val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
    private val apiKey = BuildConfig.GEMINI_API_KEY

    companion object {
        private const val GEMINI_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-1.5-flash:generateContent"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_searching)
        initViews()
        setupListeners()
    }

    private fun initViews() {
        searchInput = findViewById(R.id.search_input)
        searchButton = findViewById(R.id.search_button)
        loadingLayout = findViewById(R.id.loading_layout)
        resultCard = findViewById(R.id.result_card)
        resultContainer = findViewById(R.id.result_container)
        errorText = findViewById(R.id.error_text)
    }

    private fun setupListeners() {
        searchButton.setOnClickListener {
            val query = searchInput.text.toString().trim()
            if (query.isNotEmpty()) {
                fetchMedicineDetails(query)
            } else {
                showError("Please enter a medicine name")
            }
        }
    }

    private fun fetchMedicineDetails(medicineName: String) {
        showLoading(true)
        clearResults()
        useGreen = true // Reset color alternation for new search

        val prompt = """
            Provide comprehensive information about the medicine '$medicineName' in the following structured format:
            
            **1. Basic Information**
            - Salt Name: [Provide the active ingredient(s)]
            - Brand Names: [List common brand names]
            - Therapeutic Class: [Classification]
            - Government Equivalents: [If applicable]
            
            **2. Clinical Details**
            - Uses: [Primary indications]
            - Dosage: [Standard dosage information]
            - Contraindications: [When not to use]
            - Side Effects: [Common adverse effects]
            - Precautions: [Special warnings]
            
            **3. Physical Characteristics**
            - Appearance: [Description of typical form/color]
            - Common Packaging: [How it's usually packaged]
            
            **4. Additional Information**
            - Price Range: [Approximate cost]
            - Storage Conditions: [How to store]
            - Availability: [Prescription status]
            
            Format the response in clear sections with bold headings.
            """.trimIndent()

        val requestBody = RequestBody.create(
            mediaType,
            JSONObject()
                .put("contents", JSONArray().put(
                    JSONObject().put("parts", JSONArray().put(
                        JSONObject().put("text", prompt)
                    ))
                )).toString()
        )

        val request = Request.Builder()
            .url("$GEMINI_URL?key=$apiKey")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    showLoading(false)
                    showError("Network error: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread { showLoading(false) }

                if (!response.isSuccessful) {
                    runOnUiThread {
                        showError("API Error: ${response.code}")
                    }
                    return
                }

                response.body?.string()?.let { responseBody ->
                    try {
                        val answer = parseGeminiResponse(responseBody)
                        runOnUiThread {
                            displayResults(answer)
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            showError("Failed to parse response")
                        }
                    }
                }
            }
        })
    }

    private fun parseGeminiResponse(responseBody: String): String {
        val mainObj = JSONObject(responseBody)

        mainObj.optJSONObject("promptFeedback")?.let { feedback ->
            feedback.optString("blockReason")?.takeIf { it.isNotEmpty() }?.let {
                return "Content blocked: $it"
            }
        }

        return mainObj.optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?.optJSONObject(0)
            ?.optString("text", "No information available")
            ?: "Invalid response format"
    }

    private fun displayResults(text: String) {
        resultContainer.removeAllViews()

        if (text.contains("No information available") || text.contains("Content blocked")) {
            showError(text)
            return
        }

        val sections = text.split("**").filter { it.isNotBlank() }

        for (section in sections) {
            if (section.trim().isEmpty()) continue

            val sectionTitleEnd = section.indexOf("\n")
            val sectionTitle = if (sectionTitleEnd != -1) section.substring(0, sectionTitleEnd).trim() else section.trim()
            val sectionContent = if (sectionTitleEnd != -1) section.substring(sectionTitleEnd).trim() else ""

            addSectionTitle(sectionTitle)

            if (sectionContent.isNotEmpty()) {
                val lines = sectionContent.split("\n").filter { it.isNotBlank() }
                for (line in lines) {
                    if (line.startsWith("- ")) {
                        addBulletPoint(line.substring(2))
                    } else {
                        addParagraph(line)
                    }
                }
            }

            if (section != sections.last()) {
                addDivider()
            }
        }

        resultCard.visibility = View.VISIBLE
    }

    private fun addSectionTitle(title: String) {
        val textView = TextView(this).apply {
            text = title
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(ContextCompat.getColor(context, R.color.primary_color))
            setPadding(0, 24, 0, 8)
        }
        resultContainer.addView(textView)
    }

    private fun addParagraph(text: String) {
        val textView = TextView(this).apply {
            this.text = text
            textSize = 16f
            setTextColor(ContextCompat.getColor(context,
                if (useGreen) R.color.green else R.color.white))
            setPadding(0, 8, 0, 8)
        }
        resultContainer.addView(textView)
        useGreen = !useGreen
    }

    private fun addBulletPoint(text: String) {
        val bullet = "• "
        val spannable = SpannableStringBuilder()
        spannable.append(bullet)
        spannable.append(text)
        spannable.setSpan(StyleSpan(Typeface.BOLD), 0, bullet.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)

        val textView = TextView(this).apply {
            setText(spannable)
            textSize = 16f
            setTextColor(ContextCompat.getColor(context,
                if (useGreen) R.color.green else R.color.white))
            setPadding(32, 4, 0, 4)
        }
        resultContainer.addView(textView)
        useGreen = !useGreen
    }

    private fun addDivider() {
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                1
            ).apply {
                setMargins(0, 16, 0, 16)
            }
            setBackgroundColor(ContextCompat.getColor(context, R.color.divider_color))
        }
        resultContainer.addView(divider)
    }

    private fun clearResults() {
        resultContainer.removeAllViews()
        resultCard.visibility = View.GONE
        errorText.visibility = View.GONE
    }

    private fun showLoading(show: Boolean) {
        loadingLayout.visibility = if (show) View.VISIBLE else View.GONE
        if (show) {
            resultCard.visibility = View.GONE
            errorText.visibility = View.GONE
        }
    }

    private fun showError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
        resultCard.visibility = View.GONE
        loadingLayout.visibility = View.GONE
    }
}