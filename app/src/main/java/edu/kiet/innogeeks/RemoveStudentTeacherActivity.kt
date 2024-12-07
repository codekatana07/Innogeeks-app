package edu.kiet.innogeeks

import android.content.ContentValues
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class RemoveStudentTeacherActivity : AppCompatActivity() {

    private lateinit var adapter: CustomAdapter
    private val studentToDisplay = ArrayList<Student>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var gradeList: ArrayList<String?>
    private lateinit var checkedIDs: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remove_student_teacher)

        val database = FirebaseDatabase.getInstance("https://attendme-644ac-default-rtdb.asia-southeast1.firebasedatabase.app/")
        val studentsRef = database.getReference("students")
        val gradeRef = database.getReference("Classes")

        gradeList = ArrayList()
        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        adapter = CustomAdapter(studentToDisplay, applicationContext)
        recyclerView.adapter = adapter

        val spinner = findViewById<Spinner>(R.id.spinner_grade)

        gradeRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                gradeList.clear()
                for (snapshot in dataSnapshot.children) {
                    val grade = snapshot.child("grade").getValue(String::class.java)
                    gradeList.add(grade)
                }
                val spinnerAdapter = ArrayAdapter(
                    this@RemoveStudentTeacherActivity,
                    android.R.layout.simple_spinner_item,
                    gradeList
                )
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = spinnerAdapter
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("TAG", "Failed to load grades.", databaseError.toException())
            }
        })

        // Handle grade selection
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedGrade = parent.getItemAtPosition(position) as String?
                fetchStudents(studentsRef, selectedGrade)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
    }

    private fun fetchStudents(studentsRef: DatabaseReference, grade: String?) {
        studentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                studentToDisplay.clear()
                for (child in dataSnapshot.children) {
                    val name = child.child("name").getValue(String::class.java) ?: return
                    val assigned = child.child("assigned").getValue(Boolean::class.java) ?: false
                    val studentID = child.child("studentID").getValue(String::class.java) ?: return
                    val studentGrade = child.child("grade").getValue(String::class.java)
                    val approved = child.child("approved").getValue(Boolean::class.java) ?: false
                    val currentStudent = child.child("currentStudent").getValue(Boolean::class.java) ?: false

                    if (approved && currentStudent && assigned && studentGrade == grade) {
                        studentToDisplay.add(Student(name, assigned, studentID))
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e(ContentValues.TAG, "Failed to load students.", databaseError.toException())
            }
        })
    }

    fun remove(view: View) {
        checkedIDs = adapter.getCheckedIDs()
        val studentsRef = FirebaseDatabase.getInstance("https://attendme-644ac-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("students")

        for (studentID in checkedIDs) {
            val query = studentsRef.orderByChild("studentID").equalTo(studentID)
            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (childSnapshot in dataSnapshot.children) {
                        val studentRef = childSnapshot.ref
                        studentRef.child("currentStudent").setValue(false)
                        studentRef.child("assigned").setValue(false)
                        studentRef.child("approved").setValue(false)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(ContentValues.TAG, "Failed to update student.", databaseError.toException())
                }
            })
        }
    }
}
