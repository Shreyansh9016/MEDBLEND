import com.google.firebase.database.IgnoreExtraProperties

@IgnoreExtraProperties
data class User(
    val name: String = "",
    val username: String = "",
    val password: String = "",
    val email: String = "",
    val medicines: List<Medicine> = emptyList()
)

@IgnoreExtraProperties
data class Medicine(
    val medicineName: String = "",
    val noOfDoses: Int = 0,
    val times: List<String> = emptyList()
) {
    constructor() : this("", 0, emptyList())
}

data class HealthData(
    val date: String = "",
    val bp: String = "",          // Blood Pressure (Systolic)
    val diabetesPre: String? = null,  // Pre-meal blood sugar
    val diabetesPost: String? = null, // Post-meal blood sugar
    val notes: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)