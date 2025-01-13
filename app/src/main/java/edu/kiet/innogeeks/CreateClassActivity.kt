package edu.kiet.innogeeks

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class CreateClassActivity : AppCompatActivity() {

    private lateinit var gradeName: EditText
    private lateinit var saveBtn: Button
    private lateinit var classesRef: DatabaseReference
    private lateinit var database: FirebaseDatabase
    private lateinit var storageReference: StorageReference
    private lateinit var valueEventListener: ValueEventListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_class)

        // Initialize views
        saveBtn = findViewById(R.id.buttonSave)
        gradeName = findViewById(R.id.editTextGradeName)

        // Initialize Firebase instances
        storageReference = FirebaseStorage.getInstance().reference
        database = FirebaseDatabase.getInstance("https://innogeeks2024-default-rtdb.firebaseio.com/")
        classesRef = database.getReference("Classes")

        // Enable offline persistence
        database.setPersistenceEnabled(true)

        // Keep classes data synced
        classesRef.keepSynced(true)

        // Initialize realtime listener
        setupRealtimeListener()

        saveBtn.setOnClickListener {
            saveGrade()
        }
    }

    private fun setupRealtimeListener() {
        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                // Handle data changes here
                Log.d("Firebase", "Data updated: ${snapshot.childrenCount} classes")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Firebase", "Error listening to data: ${error.message}")
                Toast.makeText(
                    this@CreateClassActivity,
                    "Database error: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        classesRef.addValueEventListener(valueEventListener)
    }

    private fun saveGrade() {
        val grade = gradeName.text.toString().trim()

        // Validate input
        if (grade.isEmpty()) {
            gradeName.error = "Please enter class name"
            return
        }

        // Create a unique key for new grade
        val newGradeKey = classesRef.push().key

        if (newGradeKey == null) {
            Log.e("Firebase", "Couldn't get push key for grades")
            return
        }

        // Create grade object
        val gradeData = Grade(
            id = newGradeKey,
            name = grade,
            timestamp = ServerValue.TIMESTAMP
        )

        // Save grade with transaction to ensure data consistency
        classesRef.child(newGradeKey).runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                mutableData.value = gradeData
                return Transaction.success(mutableData)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error != null) {
                    Log.e("Firebase", "Transaction failed: ${error.message}")
                    Toast.makeText(
                        this@CreateClassActivity,
                        "Failed to save class: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Log.d("Firebase", "Transaction completed successfully")
                    Toast.makeText(
                        this@CreateClassActivity,
                        "Class added successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    clearForm()
                }
            }
        })
    }

    private fun clearForm() {
        gradeName.text.clear()
    }

    // Data class for Grade
    data class Grade(
        val id: String = "",
        val name: String = "",
        val timestamp: Any? = null
    )

    override fun onDestroy() {
        super.onDestroy()
        // Remove listener to prevent memory leaks
        classesRef.removeEventListener(valueEventListener)
    }
}