package edu.kiet.innogeeks

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class AddTeacherActivity : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var editTextAddress: EditText
    private lateinit var editTextMobile: EditText
    private lateinit var buttonSave: Button
    private lateinit var spinnerGrade: Spinner
    private lateinit var spinnerEmail: Spinner

    private lateinit var database: FirebaseDatabase
    private lateinit var storageReference: StorageReference
    private lateinit var gradeList: ArrayList<String>
    private lateinit var userList: HashMap<String, String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_teacher)

        // Initialize variables
        gradeList = ArrayList()
        userList = HashMap()

        storageReference = FirebaseStorage.getInstance().reference
        database = FirebaseDatabase.getInstance("https://attendme-644ac-default-rtdb.asia-southeast1.firebasedatabase.app/")

        // Initialize views
        editTextName = findViewById(R.id.editTextName)
        editTextAddress = findViewById(R.id.editTextAddress)
        editTextMobile = findViewById(R.id.editTextMobile)
        buttonSave = findViewById(R.id.buttonSave)
        spinnerGrade = findViewById(R.id.spinner_grade)
        spinnerEmail = findViewById(R.id.spinner_email)

        buttonSave.setOnClickListener { saveTeacher() }

        val gradeRef = database.getReference("Classes")
        val usersRef = database.getReference("users")

        // Fetch grades for spinner
        gradeRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                gradeList.clear()
                snapshot.children.mapNotNullTo(gradeList) { it.child("grade").getValue(String::class.java) }

                val adapter = ArrayAdapter(
                    this@AddTeacherActivity,
                    android.R.layout.simple_spinner_item,
                    gradeList
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                spinnerGrade.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read grades.", error.toException())
            }
        })

        // Fetch users for spinner
        usersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                userList.clear()
                snapshot.children.forEach {
                    val email = it.child("email").getValue(String::class.java)
                    val userID = it.child("userID").getValue(String::class.java)
                    if (email != null && userID != null) {
                        userList[email] = userID
                    }
                }

                val emailList = ArrayList(userList.keys)
                val adapter = ArrayAdapter(
                    this@AddTeacherActivity,
                    android.R.layout.simple_spinner_item,
                    emailList
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }
                spinnerEmail.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read users.", error.toException())
            }
        })
    }

    private fun saveTeacher() {
        val name = editTextName.text.toString().trim()
        val address = editTextAddress.text.toString().trim()
        val mobile = editTextMobile.text.toString().trim()
        val grade = spinnerGrade.selectedItem.toString()
        val email = spinnerEmail.selectedItem.toString()
        val userID = userList[email] ?: return

        val userRef = database.getReference("users").child(userID)

        val teacherData = mapOf(
            "name" to name,
            "address" to address,
            "mobile" to mobile,
            "grade" to grade,
            "role" to "teacher"
        )

        userRef.updateChildren(teacherData)
            .addOnSuccessListener {
                Log.d("Firebase", "Fields updated successfully!")
                Toast.makeText(this, "Teacher added successfully", Toast.LENGTH_SHORT).show()
                clearForm()
            }
            .addOnFailureListener {
                Log.e("Firebase", "Error updating fields: ${it.message}")
                Toast.makeText(this, "Failed to add teacher", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearForm() {
        editTextName.text.clear()
        editTextAddress.text.clear()
        editTextMobile.text.clear()
    }

    companion object {
        private const val TAG = "AddTeacherActivity"
    }
}
