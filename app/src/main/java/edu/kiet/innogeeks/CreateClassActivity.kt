package edu.kiet.innogeeks

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class CreateClassActivity : AppCompatActivity() {

    private lateinit var gradeName: EditText
    private lateinit var saveBtn: Button
    private lateinit var gradeRef: DatabaseReference
    private lateinit var database: FirebaseDatabase
    private lateinit var storageReference: StorageReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_class)

        // Initialize views
        saveBtn = findViewById(R.id.buttonSave)
        gradeName = findViewById(R.id.editTextGradeName)

        // Initialize Firebase instances
        storageReference = FirebaseStorage.getInstance().reference
        database = FirebaseDatabase.getInstance()
        gradeRef = database.reference.child("Classes")  // Creates a "Classes" node in Realtime Database

        saveBtn.setOnClickListener {
            saveGrade()
        }
    }

    private fun saveGrade() {
        val grade = gradeName.text.toString().trim()

        // Validate input
        if (grade.isEmpty()) {
            gradeName.error = "Please enter class name"
            return
        }

        // Create a unique key for the new grade
        val newGradeKey = gradeRef.push().key

        if (newGradeKey == null) {
            Log.w("CreateClass", "Couldn't get push key for grades")
            return
        }

        // Create grade data
        val gradeData = HashMap<String, Any>()
        gradeData["grade"] = grade
        gradeData["timestamp"] = ServerValue.TIMESTAMP

        // Save to Realtime Database
        gradeRef.child(newGradeKey).setValue(gradeData)
            .addOnSuccessListener {
                Log.d("Firebase", "Record added successfully!")
                Toast.makeText(this, "Class added successfully", Toast.LENGTH_SHORT).show()
                clearForm()
            }
            .addOnFailureListener { e ->
                Log.e("Firebase", "Error adding record: ${e.message}")
                Toast.makeText(this, "Adding class failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearForm() {
        gradeName.text.clear()
    }
}