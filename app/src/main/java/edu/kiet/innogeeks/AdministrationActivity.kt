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

class AdministrationActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_administration)

        val report: CardView = findViewById(R.id.getReport)
        report.setOnClickListener {
            val intent = Intent(this, GetReportActivity::class.java)
            startActivity(intent)
        }

        val firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUser: FirebaseUser? = firebaseAuth.currentUser
        val database = FirebaseDatabase.getInstance("https://innogeeks2024-default-rtdb.firebaseio.com/")

        firebaseUser?.let { user ->
            val userID = user.uid
            println(userID)

            val myRef: DatabaseReference = database.getReference("users").child(userID)
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
