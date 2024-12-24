package edu.kiet.innogeeks

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*

class ParentActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parents)

        val parentNotes: CardView = findViewById(R.id.parentNote)
        parentNotes.setOnClickListener {
            val intent = Intent(this, ParentNotesActivity::class.java)
            startActivity(intent)
        }

        val firebaseAuth = FirebaseAuth.getInstance()
        val firebaseUser: FirebaseUser? = firebaseAuth.currentUser
        val database = FirebaseDatabase.getInstance("https://innogeeks2024-default-rtdb.firebaseio.com/")

        firebaseUser?.uid?.let { userID ->
            val myRef = database.getReference("users").child(userID)
            myRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()) {
                        val name = dataSnapshot.child("name").getValue(String::class.java)
                        val textView: TextView = findViewById(R.id.textView)
                        textView.text = "Welcome! $name"
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("Firebase Error", databaseError.message)
                }
            })
        }
    }
}
