package edu.kiet.innogeeks

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class AdminsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admins)

        val createClass: CardView = findViewById(R.id.create_class_card)
        val removeStudent: CardView = findViewById(R.id.remove_std)
        val addTeachers: CardView = findViewById(R.id.addTeachers)
        val addStudents: CardView = findViewById(R.id.addStudents)
        val approveAddStudents: CardView = findViewById(R.id.approveAddStd)
        val approveRemoveStudents: CardView = findViewById(R.id.approveRemoveStd)

        addTeachers.setOnClickListener {
            startActivity(Intent(this, AddTeacherActivity::class.java))
        }

        createClass.setOnClickListener {
            startActivity(Intent(this, CreateClassActivity::class.java))
        }

        removeStudent.setOnClickListener {
            startActivity(Intent(this, RemoveStudentActivityAdmin::class.java))
        }

        addStudents.setOnClickListener {
            startActivity(Intent(this, AddStudentActivity::class.java))
        }

        approveAddStudents.setOnClickListener {
            startActivity(Intent(this, AddApproveStudentActivityAdmin::class.java))
        }

        approveRemoveStudents.setOnClickListener {
            startActivity(Intent(this, RemoveApproveStudentActivityAdmin::class.java))
        }

        val firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUser: FirebaseUser? = firebaseAuth.currentUser
        val database = FirebaseDatabase.getInstance("https://innogeeks2024-default-rtdb.firebaseio.com/")

        firebaseUser?.let { user ->
            val userID = user.uid
            println(userID)

            val myRef = database.getReference("users").child(userID)
            myRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val name = snapshot.child("name").getValue(String::class.java)
                        val textView: TextView = findViewById(R.id.textView)
                        textView.text = "Welcome! $name"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("Firebase Error", error.message)
                }
            })
        }
    }
}
