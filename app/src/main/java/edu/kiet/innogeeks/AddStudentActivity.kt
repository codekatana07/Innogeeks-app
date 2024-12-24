package edu.kiet.innogeeks

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream

class AddStudentActivity : AppCompatActivity() {

    private val PICK_IMAGE_REQUEST = 1
    private val REQUEST_IMAGE_CAPTURE = 2

    private lateinit var editTextName: EditText
    private lateinit var editTextAddress: EditText
    private lateinit var editTextFatherName: EditText
    private lateinit var editTextFatherMobile: EditText
    private lateinit var editTextFatherOccupation: EditText
    private lateinit var editTextFatherEmail: EditText
    private lateinit var editTextMotherEmail: EditText
    private lateinit var editTextMotherName: EditText
    private lateinit var editTextMotherMobile: EditText
    private lateinit var editTextMotherOccupation: EditText
    private lateinit var editTextGuardianName: EditText
    private lateinit var editTextGuardianMobile: EditText
    private lateinit var editTextGuardianOccupation: EditText
    private lateinit var editTextGuardianAddress: EditText
    private lateinit var editTextLandPhone: EditText
    private lateinit var editTextWeight: EditText
    private lateinit var editTextHeight: EditText
    private lateinit var spinnerEmail: Spinner

    private lateinit var buttonAddImages: Button
    private lateinit var buttonSave: Button

    private lateinit var studentRef: DatabaseReference
    private lateinit var userRef: DatabaseReference
    private lateinit var storageReference: StorageReference
    private lateinit var database: FirebaseDatabase

    private var imageUri: Uri? = null
    private val userList = HashMap<String, String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_student)

        database = FirebaseDatabase.getInstance("https://innogeeks2024-default-rtdb.firebaseio.com/")
        studentRef = database.getReference("students")
        userRef = database.getReference("users")
        storageReference = FirebaseStorage.getInstance().reference

        // Initialize views
        editTextName = findViewById(R.id.editTextName)
        editTextAddress = findViewById(R.id.editTextAddress)
        editTextFatherName = findViewById(R.id.editTextFatherName)
        editTextFatherMobile = findViewById(R.id.editTextFatherMobile)
        editTextFatherOccupation = findViewById(R.id.editTextFatherOccupation)
        editTextFatherEmail = findViewById(R.id.editTextFatherEmail)
        editTextMotherEmail = findViewById(R.id.editTextMotherEmail)
        editTextMotherName = findViewById(R.id.editTextMotherName)
        editTextMotherMobile = findViewById(R.id.editTextMotherMobile)
        editTextMotherOccupation = findViewById(R.id.editTextMotherOccupation)
        editTextGuardianName = findViewById(R.id.editTextGuardianName)
        editTextGuardianAddress = findViewById(R.id.editTextGuardianAddress)
        editTextGuardianMobile = findViewById(R.id.editTextGuardianMobile)
        editTextGuardianOccupation = findViewById(R.id.editTextGuardianOccupation)
        editTextLandPhone = findViewById(R.id.editTextLandPhone)
        editTextWeight = findViewById(R.id.editTextWeight)
        editTextHeight = findViewById(R.id.editTextHeight)
        spinnerEmail = findViewById(R.id.spinner_email)

        buttonAddImages = findViewById(R.id.buttonAddImages)
        buttonSave = findViewById(R.id.buttonSave)

        userRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                for (child in snapshot.children) {
                    val email = child.child("email").getValue(String::class.java) ?: ""
                    val userID = child.child("userID").getValue(String::class.java) ?: ""
                    userList[email] = userID
                }
                val emailList = ArrayList(userList.keys)
                val adapter = ArrayAdapter(this@AddStudentActivity, android.R.layout.simple_spinner_item, emailList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerEmail.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("TAG", "Failed to read value.", error.toException())
            }
        })

        buttonAddImages.setOnClickListener { openImageChooser() }
        buttonSave.setOnClickListener { saveStudent() }
    }

    private fun openImageChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        startActivityForResult(Intent.createChooser(intent, "Select Image"), PICK_IMAGE_REQUEST)
    }

    private fun saveStudent() {
        val stdName = editTextName.text.toString().trim()
        val address = editTextAddress.text.toString().trim()
        val fatherName = editTextFatherName.text.toString().trim()
        val fatherMobile = editTextFatherMobile.text.toString().trim()
        val fatherOccupation = editTextFatherOccupation.text.toString().trim()
        val fatherEmail = editTextFatherEmail.text.toString().trim()
        val motherName = editTextMotherName.text.toString().trim()
        val motherMobile = editTextMotherMobile.text.toString().trim()
        val motherOccupation = editTextMotherOccupation.text.toString().trim()
        val motherEmail = editTextMotherEmail.text.toString().trim()
        val guardianName = editTextGuardianName.text.toString().trim()
        val guardianAddress = editTextGuardianAddress.text.toString().trim()
        val guardianMobile = editTextGuardianMobile.text.toString().trim()
        val guardianOccupation = editTextGuardianOccupation.text.toString().trim()
        val landPhone = editTextLandPhone.text.toString().trim()
        val weight = editTextWeight.text.toString().trim()
        val height = editTextHeight.text.toString().trim()
        val email = spinnerEmail.selectedItem.toString()
        val userID = userList[email] ?: ""

        val studentId = studentRef.push().key ?: return
        val studentData = hashMapOf(
            "name" to stdName,
            "address" to address,
            "fatherName" to fatherName,
            "fatherMobile" to fatherMobile,
            "fatherOccupation" to fatherOccupation,
            "fatherEmail" to fatherEmail,
            "motherName" to motherName,
            "motherMobile" to motherMobile,
            "motherOccupation" to motherOccupation,
            "motherEmail" to motherEmail,
            "guardianName" to guardianName,
            "guardianAddress" to guardianAddress,
            "guardianMobile" to guardianMobile,
            "guardianOccupation" to guardianOccupation,
            "landPhone" to landPhone,
            "weight" to weight,
            "height" to height,
            "studentID" to studentId,
            "guardianID" to userID,
            "currentStudent" to true,
            "assigned" to false
        )

        if (imageUri != null) {
            uploadImage(studentId)
            studentData["imageUri"] = imageUri.toString()
        }

        studentRef.child(studentId).setValue(studentData).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Student added successfully", Toast.LENGTH_SHORT).show()
                clearForm()
            } else {
                Toast.makeText(this, "Failed to add student", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun uploadImage(studentId: String) {
        val ref = storageReference.child("images/$studentId.jpg")
        imageUri?.let {
            ref.putFile(it).addOnSuccessListener {
                // Image uploaded successfully
            }.addOnFailureListener {
                // Handle failure
            }
        }
    }

    private fun clearForm() {
        editTextName.setText("")
        editTextAddress.setText("")
        editTextFatherName.setText("")
        editTextFatherMobile.setText("")
        editTextFatherOccupation.setText("")
        editTextFatherEmail.setText("")
        editTextMotherName.setText("")
        editTextMotherMobile.setText("")
        editTextMotherOccupation.setText("")
        editTextMotherEmail.setText("")
        editTextGuardianName.setText("")
        editTextGuardianAddress.setText("")
        editTextGuardianOccupation.setText("")
        editTextGuardianMobile.setText("")
        editTextLandPhone.setText("")
        editTextWeight.setText("")
        editTextHeight.setText("")
        imageUri = null
    }
}
