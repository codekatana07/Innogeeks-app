package edu.kiet.innogeeks

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ParentNotesActivity : AppCompatActivity() {

    private lateinit var nameLabel: TextView
    private lateinit var gradeLabel: TextView
    private lateinit var currentdate: TextView
    private lateinit var currentDate: String
    private lateinit var textReason: EditText
    private lateinit var dateButton: Button
    private lateinit var submitButton: Button
    private lateinit var db: FirebaseFirestore
    private lateinit var userId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_notes)

        // Initialize Firestore
        db = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

        // Initialize views
        nameLabel = findViewById(R.id.labelStdName)
        gradeLabel = findViewById(R.id.labelGrade)
        textReason = findViewById(R.id.editTextTextMultiLine)
//        dateButton = findViewById(R.id.dateButton)
        currentdate = findViewById(R.id.editTextDate)
        submitButton = findViewById(R.id.btnSubmit)
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        currentDate = sdf.format(Date())
        currentdate.text = currentDate

        // Get student details
        getStudentDetails()

//        // Set up date picker
//        dateButton.setOnClickListener {
//            showDatePicker()
//        }

        // Set up submit button
        submitButton.setOnClickListener {
            submitReason()
        }
    }

    private fun getStudentDetails() {
        db.collection("students").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    nameLabel.text = "Name: ${document.getString("name")}"
                    gradeLabel.text = "Domain: ${document.getString("domain")}"
                }
            }
            .addOnFailureListener { e ->
                Log.w("ParentNotes", "Error getting student details", e)
            }
    }

//    private fun showDatePicker() {
//        val calendar = Calendar.getInstance()
//        val year = calendar.get(Calendar.YEAR)
//        val month = calendar.get(Calendar.MONTH)
//        val day = calendar.get(Calendar.DAY_OF_MONTH)
//
//        val datePickerDialog = DatePickerDialog(
//            this,
//            { _, selectedYear, selectedMonth, selectedDay ->
//                val selectedCalendar = Calendar.getInstance()
//                selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
//
//                val today = Calendar.getInstance()
//                val nextWeek = Calendar.getInstance()
//                nextWeek.add(Calendar.DAY_OF_MONTH, 7)
//
//                when {
//                    selectedCalendar.before(today) -> {
//                        Toast.makeText(this, "Cannot select past dates", Toast.LENGTH_SHORT).show()
//                    }
//                    selectedCalendar.after(nextWeek) -> {
//                        Toast.makeText(this, "Cannot select dates beyond next week", Toast.LENGTH_SHORT).show()
//                    }
//                    else -> {
//                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//                        selectedDate = sdf.format(selectedCalendar.time)
//                        dateButton.text = selectedDate
//                    }
//                }
//            },
//            year,
//            month,
//            day
//        )
//        datePickerDialog.show()
//    }

    private fun submitReason() {
//        if (!::selectedDate.isInitialized) {
//            Toast.makeText(this, "Please select a date first", Toast.LENGTH_SHORT).show()
//            return
//        }

        val reason = textReason.text.toString()
        if (reason.isEmpty()) {
            Toast.makeText(this, "Please enter a reason", Toast.LENGTH_SHORT).show()
            return
        }

        // Get reference to the student document
        val studentRef = db.collection("students").document(userId)

        // First, get the existing reasons map (if any)
        studentRef.get()
            .addOnSuccessListener { document ->
                val existingReasons = document.get("reasonsOfAbsence") as? Map<String, String> ?: mapOf()
                
                // Create new map with existing reasons plus new one
                val updatedReasons = existingReasons.toMutableMap()
                updatedReasons[currentdate.text.toString()] = reason

                // Update Firestore with the merged map
                studentRef.update("reasonsOfAbsence", updatedReasons)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Reason submitted successfully", Toast.LENGTH_SHORT).show()
                        textReason.text.clear()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Failed to submit reason", Toast.LENGTH_SHORT).show()
                        Log.w("ParentNotes", "Error submitting reason", e)
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to fetch existing reasons", Toast.LENGTH_SHORT).show()
                Log.w("ParentNotes", "Error fetching reasons", e)
            }
    }
}
