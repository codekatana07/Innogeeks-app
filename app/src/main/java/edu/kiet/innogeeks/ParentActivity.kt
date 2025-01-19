package edu.kiet.innogeeks

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.kiet.innogeeks.databinding.ActivityParentsBinding

class ParentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityParentsBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var studentDomain: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Log.d(TAG, "onCreate: Initializing ParentActivity")

        // Initialize Firebase
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupUI()
        fetchUserData()
    }

    private fun setupUI() {
        binding.parentNote.setOnClickListener {
            startActivity(Intent(this, ParentNotesActivity::class.java))
        }

        // Initialize progress indicator
        binding.attendanceProgress.apply {
            trackThickness = 8
            indicatorSize = 120
            trackColor = Color.parseColor("#E0E0E0")
        }
    }

    private fun fetchUserData() {
        val userId = auth.currentUser?.uid
        Log.d(TAG, "fetchUserData: Current user ID: $userId")

        if (userId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }

        // Fetch student data
        db.collection("students").document(userId)
            .get()
            .addOnSuccessListener { studentDoc ->
                if (studentDoc.exists()) {
                    Log.d(TAG, "Student document exists")
                    
                    // Update welcome message
                    val name = studentDoc.getString("name") ?: "Student"
                    binding.textView.text = "Welcome! $name"

                    // Get domain and attendance data
                    studentDomain = studentDoc.getString("domain")
                    val presentDays = studentDoc.getLong("attendance.totalPresentDays") ?: 0L
                    Log.d(TAG, "Student domain: $studentDomain, Present days: $presentDays")

                    // Fetch total days from classes collection
                    fetchTotalDays(presentDays)
                } else {
                    Log.e(TAG, "Student document does not exist for ID: $userId")
                    Toast.makeText(this, "Student data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching student data", e)
                Toast.makeText(this, "Failed to load student data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchTotalDays(presentDays: Long) {
        Log.d(TAG, "fetchTotalDays: Starting with presentDays = $presentDays")
        
        studentDomain?.let { domain ->
            Log.d(TAG, "Fetching data for domain: $domain")
            
            db.collection("classes").document(domain)
                .get()
                .addOnSuccessListener { classDoc ->
                    if (classDoc.exists()) {
                        val totalDays = classDoc.getLong("attendance.totalDays") ?: 0L
                        Log.d(TAG, "Total days from class doc: $totalDays")
                        
                        updateAttendanceUI(presentDays, totalDays)
                    } else {
                        Log.e(TAG, "Class document does not exist for domain: $domain")
                        updateAttendanceUI(presentDays, 0)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error fetching class data", e)
                    Toast.makeText(this, "Failed to load attendance data", Toast.LENGTH_SHORT).show()
                    updateAttendanceUI(presentDays, 0)
                }
        } ?: run {
            Log.e(TAG, "Student domain is null")
            updateAttendanceUI(presentDays, 0)
        }
    }

    private fun updateAttendanceUI(presentDays: Long, totalDays: Long) {
        Log.d(TAG, "updateAttendanceUI: Present=$presentDays, Total=$totalDays")
        
        val percentage = if (totalDays > 0) {
            (presentDays.toFloat() / totalDays.toFloat() * 100).toInt()
        } else {
            0
        }
        Log.d(TAG, "Calculated percentage: $percentage%")

        with(binding) {
            // Update progress indicator
            attendanceProgress.progress = percentage
            percentageText.text = "$percentage%"
            
            // Format the attendance text
            daysPresent.text = if (totalDays > 0) {
                "Present: $presentDays days out of $totalDays days"
            } else {
                "No attendance records yet"
            }

            // Set color based on attendance percentage
            val color = when {
                percentage >= 75 -> Color.parseColor("#4CAF50") // Green for good attendance
                percentage >= 60 -> Color.parseColor("#FFA000") // Orange for average attendance
                else -> Color.parseColor("#F44336") // Red for poor attendance
            }
            
            // Update UI colors
            attendanceProgress.setIndicatorColor(color)
            percentageText.setTextColor(color)
        }
    }

    companion object {
        private const val TAG = "ParentActivity"
    }
}
