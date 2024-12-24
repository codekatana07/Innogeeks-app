package edu.kiet.innogeeks

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ParentNotesActivity : AppCompatActivity() {

    private lateinit var gradeList: ArrayList<String>
    private lateinit var userId: String
    private lateinit var teacherName: String
    private lateinit var teacherGrade: String
    private lateinit var nameLabel: TextView
    private lateinit var gradeLabel: TextView
    private lateinit var dateLabel: TextView
    private lateinit var teacherNameLabel: TextView
    private lateinit var textReason: EditText
    private lateinit var presentStudents: ArrayList<String>
    private lateinit var allStudents: ArrayList<String>
    private lateinit var database: FirebaseDatabase
    private lateinit var myRef: DatabaseReference
    private lateinit var attendanceData: HashMap<String, Any>
    private lateinit var currentDate: String
    private lateinit var studentID: String

    // Function to handle submission of reason
    fun submit(view: View) {
        textReason = findViewById(R.id.editTextTextMultiLine)
        val reason = textReason.text.toString()
        Log.d("ParentNotes", reason)

        myRef = database.getReference("students")
        myRef.orderByChild("studentID").equalTo(studentID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (studentSnapshot in dataSnapshot.children) {
                    val attendanceRef = studentSnapshot.ref.child("attendance")
                    attendanceRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            for (attendanceSnapshot in dataSnapshot.children) {
                                val attendanceKey = attendanceSnapshot.child(currentDate).key
                                Log.d("ParentNotes", attendanceKey ?: "No key found")
                                if (attendanceKey == currentDate) {
                                    val reasonRef = attendanceSnapshot.ref.child("reason")
                                    reasonRef.setValue(reason)
                                    break
                                }
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            // Handle error
                        }
                    })
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
            }
        })
    }

    // Function to get current date
    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return sdf.format(Date())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_notes)
        currentDate = getCurrentDate()

        database = FirebaseDatabase.getInstance("https://innogeeks2024-default-rtdb.firebaseio.com/")
        val firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUser = firebaseAuth.currentUser

        allStudents = ArrayList()
        attendanceData = HashMap()

        userId = firebaseUser?.uid ?: ""
        Log.d("ParentNotes", userId)

        val userRef = database.getReference("users")
        val query = userRef.orderByChild("userID").equalTo(userId)

        nameLabel = findViewById(R.id.labelStdName)
        gradeLabel = findViewById(R.id.labelGrade)

        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (snapshot in dataSnapshot.children) {
                        if (snapshot.hasChild("studentID")) {
                            studentID = snapshot.child("studentID").getValue(String::class.java) ?: ""
                            searchStudent(studentID)
                        } else {
                            Log.d("Firebase", "Missing name or grade fields for the user")
                        }
                    }
                } else {
                    Log.d("Firebase", "User not found")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
            }
        })
    }

    // Function to search for a student based on studentID
    private fun searchStudent(studentID: String) {
        Log.d("ParentNotes", "Searching for student with ID: $studentID")
        myRef = database.getReference("students")
        val query = myRef.orderByChild("studentID").equalTo(studentID)
        query.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    for (snapshot in dataSnapshot.children) {
                        if (snapshot.hasChild("name") && snapshot.hasChild("grade")) {
                            val name = snapshot.child("name").getValue(String::class.java)
                            val grade = snapshot.child("grade").getValue(String::class.java)
                            nameLabel.text = "Name: $name"
                            gradeLabel.text = "Grade: $grade"
                        } else {
                            Log.d("Firebase", "Missing name or grade fields for the user")
                        }
                    }
                } else {
                    Log.d("Firebase", "User not found")
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Handle error
            }
        })
    }
}
