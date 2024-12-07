package edu.kiet.innogeeks

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
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

        storageReference = FirebaseStorage.getInstance().reference
        database = FirebaseDatabase.getInstance("https://attendme-644ac-default-rtdb.asia-southeast1.firebasedatabase.app/")

        saveBtn.setOnClickListener {
            saveGrade()
        }
    }

    private fun saveGrade() {
        val grade = gradeName.text.toString()
        gradeRef = database.reference.child("Classes").push()
        val recordId = gradeRef.key // Get the unique key for the new record

        val teacherData = HashMap<String, Any>()
        teacherData["grade"] = grade

        gradeRef.setValue(teacherData)
            .addOnSuccessListener {
                // Record added successfully
                Log.d("Firebase", "Record added successfully!")
                Toast.makeText(this, "Class added successfully", Toast.LENGTH_SHORT).show()
                clearForm()
            }
            .addOnFailureListener { e ->
                // Error adding the record
                Log.e("Firebase", "Error adding record: ${e.message}")
                Toast.makeText(this, "Adding class failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearForm() {
        gradeName.text.clear()
    }
}
