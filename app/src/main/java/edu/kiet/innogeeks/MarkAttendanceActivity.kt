package edu.kiet.innogeeks

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.TextView
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

class MarkAttendanceActivity : AppCompatActivity(), CustomAdapter.OnItemClickListener {

    private lateinit var adapter: CustomAdapter
    private val studentToDisplay = ArrayList<Student>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var nameLabel: TextView
    private lateinit var gradeLabel: TextView

    private val allStudents = ArrayList<String>()
    private val attendanceData = HashMap<String, Any>()
    private lateinit var database: FirebaseDatabase
    private lateinit var myRef: DatabaseReference

    private var currentDate: String = ""
    private var teacherGrade: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mark_attendance)

        currentDate = getCurrentDate()
        database = FirebaseDatabase.getInstance("https://attendme-644ac-default-rtdb.asia-southeast1.firebasedatabase.app/")
        val firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUser: FirebaseUser? = firebaseAuth.currentUser
        myRef = database.getReference("students")

        val userId = firebaseUser?.uid

        // Initialize views
        nameLabel = findViewById(R.id.label_teacher)
        gradeLabel = findViewById(R.id.label_grade)
        recyclerView = findViewById(R.id.listView)

        // Setup RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = CustomAdapter(studentToDisplay, this)
        adapter.setOnItemClickListener(this)
        recyclerView.adapter = adapter

        // Fetch teacher data
        userId?.let { fetchTeacherData(it) }

        // Fetch student data
        fetchStudentData()
    }

    private fun fetchTeacherData(userId: String) {
        val userRef = database.getReference("users")
        val query = userRef.orderByChild("userID").equalTo(userId)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    for (child in snapshot.children) {
                        val name = child.child("name").getValue(String::class.java)
                        teacherGrade = child.child("grade").getValue(String::class.java)

                        nameLabel.text = "Teacher: $name"
                        gradeLabel.text = "Grade: $teacherGrade"
                    }
                } else {
                    Log.d("Firebase", "User not found")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error: ${error.message}")
            }
        })
    }

    private fun fetchStudentData() {
        myRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val newStudentList = ArrayList<Student>()
                allStudents.clear()

                for (child in snapshot.children) {
                    val stdName = child.child("name").getValue(String::class.java)
                    val stdID = child.child("studentID").getValue(String::class.java)
                    val grade = child.child("grade").getValue(String::class.java)

                    if (grade == teacherGrade) {
                        newStudentList.add(Student(stdName ?: "", stdID ?: ""))
                        allStudents.add(stdID ?: "")
                    }
                }

                adapter.updateDataSet(newStudentList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("Firebase", "Failed to read value.", error.toException())
            }
        })
    }

    override fun onItemClick(position: Int) {
        val student = studentToDisplay[position]
        student.assigned = !student.assigned
        adapter.notifyItemChanged(position)
    }

    fun submit(view: View) {
        val presentStudents = adapter.getCheckedIDs()
        allStudents.forEach { studentId ->
            attendanceData[studentId] = presentStudents.contains(studentId)
        }

        attendanceData.forEach { (studentId, isPresent) ->
            markAttendance(studentId, isPresent as Boolean)
        }
    }

    private fun markAttendance(studentId: String, isPresent: Boolean) {
        myRef.orderByChild("studentID").equalTo(studentId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (child in snapshot.children) {
                        val studentKey = child.key ?: continue
                        val attendanceRef = myRef.child(studentKey).child("attendance")
                        val attendanceID = attendanceRef.push().key

                        attendanceID?.let {
                            val attendanceEntry = hashMapOf(currentDate to isPresent)
                            attendanceRef.child(it).setValue(attendanceEntry)

                            if (!isPresent) {
                                val guardianId = child.child("guardianID").getValue(String::class.java)
                                guardianId?.let { sendNotificationToGuardian(it) }
                            }
                        }
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error: ${error.message}")
                }
            })
    }

    private fun sendNotificationToGuardian(guardianId: String) {
        val usersRef = database.getReference("users")
        usersRef.orderByChild("userID").equalTo(guardianId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { child ->
                        val token = child.child("token").getValue(String::class.java)
                        if (token != null) sendNotification(token)
                        else Log.d("Firebase", "Missing deviceToken field for the guardian")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase", "Error: ${error.message}")
                }
            })
    }

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

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }
}