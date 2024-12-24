package edu.kiet.innogeeks

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.FirebaseApp  // Make sure Firebase is initialized

class TeachersActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_teachers)

        // Ensure FirebaseApp is initialized (if not done already)
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        // UI elements
        val enrollStudent = findViewById<CardView>(R.id.enrollStudents)
        val markAttendance = findViewById<CardView>(R.id.markAttendance)
        val removeStudent = findViewById<CardView>(R.id.removeStudent)
        val textView = findViewById<TextView>(R.id.textView)

        // Set up CardView click listeners
        enrollStudent.setOnClickListener { v ->
            val intent = Intent(v.context, EnrolStudentActivity::class.java)
            v.context.startActivity(intent)
        }

        markAttendance.setOnClickListener { v ->
            val intent = Intent(v.context, MarkAttendanceActivity::class.java)
            v.context.startActivity(intent)
        }

        removeStudent.setOnClickListener { v ->
            val intent = Intent(v.context, RemoveStudentTeacherActivity::class.java)
            v.context.startActivity(intent)
        }

        // Firebase Authentication and Database reference
        val firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUser = firebaseAuth.currentUser

        // Check if the user is logged in
        if (firebaseUser != null) {
            val userID = firebaseUser.uid
            val database = FirebaseDatabase.getInstance("https://innogeeks2024-default-rtdb.firebaseio.com/")
            val myRef = database.getReference("users").child(userID)

            myRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val name = dataSnapshot.child("name").getValue(String::class.java)
                        if (name != null) {
                            textView.text = "Welcome! $name"
                        } else {
                            Log.e("Firebase", "Name not found in database")
                        }
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase Error", databaseError.message)
                }
            })
        } else {
            Log.e("Firebase Error", "User is not logged in.")
            // Handle the case when the user is not signed in (e.g., show a login screen)
        }
    }
}
