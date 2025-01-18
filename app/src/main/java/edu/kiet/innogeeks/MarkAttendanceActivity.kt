package edu.kiet.innogeeks

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

class MarkAttendanceActivity : AppCompatActivity(), CustomAdapter.OnItemClickListener {

    private lateinit var adapter: CustomAdapter
    private val studentToDisplay = ArrayList<Student>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var nameLabel: TextView
    private lateinit var gradeLabel: TextView

    private val allStudents = ArrayList<String>()
    private lateinit var db: FirebaseFirestore
    private var coordinatorDomain: String? = null
    private var currentDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mark_attendance)

        currentDate = getCurrentDate()
        db = FirebaseFirestore.getInstance()
        val firebaseAuth = FirebaseAuth.getInstance()
        val coordinatorId = firebaseAuth.currentUser?.uid

        // Initialize views
        nameLabel = findViewById(R.id.label_teacher)
        gradeLabel = findViewById(R.id.label_grade)
        recyclerView = findViewById(R.id.recyclerView)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = CustomAdapter(studentToDisplay, this)
        adapter.setOnItemClickListener(this)
        recyclerView.adapter = adapter

        // Fetch coordinator data and then students
        coordinatorId?.let { fetchCoordinatorData(it) }
    }
    private fun fetchCoordinatorData(coordinatorId: String) {
        db.collection("coordinators").document(coordinatorId)
            .get()
            .addOnSuccessListener { document ->
                coordinatorDomain = document.getString("domain")
                nameLabel.text = "Coordinator: ${document.getString("name")}"
                gradeLabel.text = "Domain: $coordinatorDomain"

                // After getting coordinator domain, fetch students
                coordinatorDomain?.let { fetchStudentData(it) }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching coordinator data", e)
            }
    }
    private fun fetchStudentData(domain: String) {
        db.collection("students")
            .whereEqualTo("domain", domain)
            .get()
            .addOnSuccessListener { documents ->
                val newStudentList = ArrayList<Student>()
                allStudents.clear()

                for (document in documents) {
                    val stdName = document.getString("name") ?: ""
                    val stdId = document.id  // Using Firestore document ID (Firebase Auth UID)
                    newStudentList.add(Student(stdName, stdId))
                    allStudents.add(stdId)
                }

                adapter.updateDataSet(newStudentList)
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error fetching students", e)
            }
    }
    override fun onItemClick(position: Int) {
        val student = studentToDisplay[position]
        student.assigned = !student.assigned
        adapter.notifyItemChanged(position)
    }

    fun submit(view: View) {
        val presentStudents = adapter.getCheckedIDs()
        
        // Update classes collection with present students and increment total days
        coordinatorDomain?.let { domain ->
            val classRef = db.collection("classes").document(domain)
            
            // Create a batch to perform multiple operations
            val batch = db.batch()
            
            // 1. Add present students under today's date
            batch.update(classRef, currentDate, presentStudents)
            
            // 2. Increment total days in attendance map
            batch.update(classRef, "attendance.totalDays", FieldValue.increment(1))

            // Execute the batch
            batch.commit()
                .addOnSuccessListener {
                    // After successful class update, update individual student attendance
                    updateStudentsAttendance(presentStudents)
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error updating class attendance", e)
                    Toast.makeText(this, "Failed to mark attendance", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateStudentsAttendance(presentStudents: List<String>) {
        val batch = db.batch()

        presentStudents.forEach { studentId ->
            val studentRef = db.collection("students").document(studentId)
            
            // Use FieldValue.increment to atomically increment the total present days
            batch.update(studentRef, "attendance.totalPresentDays", FieldValue.increment(1))
        }

        batch.commit()
            .addOnSuccessListener {
                Toast.makeText(this, "Attendance marked successfully", Toast.LENGTH_SHORT).show()
                finish()  // Close the activity after successful submission
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error updating student attendance counts", e)
                Toast.makeText(this, "Failed to update attendance counts", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
//    private fun sendNotificationToGuardian(guardianId: String) {
//        val usersRef = database.getReference("users")
//        usersRef.orderByChild("userID").equalTo(guardianId)
//            .addListenerForSingleValueEvent(object : ValueEventListener {
//                override fun onDataChange(snapshot: DataSnapshot) {
//                    snapshot.children.forEach { child ->
//                        val token = child.child("token").getValue(String::class.java)
//                        if (token != null) sendNotification(token)
//                        else Log.d("Firebase", "Missing deviceToken field for the guardian")
//                    }
//                }
//
//                override fun onCancelled(error: DatabaseError) {
//                    Log.e("Firebase", "Error: ${error.message}")
//                }
//            })
//    }

    private fun sendNotification(recipientToken: String) {
        try {
            val client = OkHttpClient()
            val url = HttpUrl.Builder()
                .scheme("https")
                .host("fcm.googleapis.com")
                .addPathSegments("fcm/send")
                .build()

            val notification = JSONObject().apply {
                put("title", "New message")
                put("body", "You have a new message")
            }

            val data = JSONObject().apply {
                put("key1", "value1")
                put("key2", "value2")
            }

            val payload = JSONObject().apply {
                put("notification", notification)
                put("data", data)
                put("to", recipientToken)
            }

            val requestBody = payload.toString().toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .addHeader("Authorization", "Bearer YOUR_SERVER_KEY")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.e("Notification", "Failed to send notification: ${response.message}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

//    private fun getCurrentDate(): String {
//        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//        return sdf.format(Date())
//    }
}