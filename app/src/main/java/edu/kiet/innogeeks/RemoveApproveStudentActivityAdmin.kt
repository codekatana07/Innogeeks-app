package edu.kiet.innogeeks

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
import java.util.*

class RemoveApproveStudentActivityAdmin : AppCompatActivity() {
    private lateinit var adapter: CustomAdapter
    private lateinit var studentToDisplay: ArrayList<Student>
    private lateinit var recyclerView: RecyclerView
    private lateinit var reference: DatabaseReference
    private lateinit var gradeList: ArrayList<String>

    fun remove(view: View) {
        val checkedIDs = adapter.getCheckedIDs()
        val studentsRef = FirebaseDatabase.getInstance("https://innogeeks2024-default-rtdb.firebaseio.com/")
            .getReference("students")

        for (stdID in checkedIDs) {
            val query = studentsRef.orderByChild("studentID").equalTo(stdID)
            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (childSnapshot in dataSnapshot.children) {
                        val studentRef = childSnapshot.ref
                        studentRef.child("currentStudent").setValue(true)
                        studentRef.child("assigned").setValue(false)
                        studentRef.child("approved").setValue(true)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e(TAG, "Failed to update grade: ${databaseError.message}")
                }
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_approve_add_studnet)

        // Initialize RecyclerView
        recyclerView = findViewById(R.id.listView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize lists
        studentToDisplay = ArrayList()
        gradeList = ArrayList()

        // Initialize Firebase
        val database = FirebaseDatabase.getInstance("https://innogeeks2024-default-rtdb.firebaseio.com/")
        val studentsRef = database.getReference("students")
        val gradeRef = database.getReference("Classes")

        // Initialize adapter
        adapter = CustomAdapter(studentToDisplay, applicationContext)
        recyclerView.adapter = adapter

        adapter.setOnItemClickListener(object : CustomAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                val student = studentToDisplay[position]
                student.assigned = !student.assigned
                adapter.notifyItemChanged(position)
            }
        })

        // Setup grade spinner
        setupGradeSpinner(gradeRef)
    }

    private fun setupGradeSpinner(gradeRef: DatabaseReference) {
        gradeRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                gradeList.clear()
                val spinner: Spinner = findViewById(R.id.spinner_grade)

                for (snapshot in dataSnapshot.children) {
                    val grade = snapshot.child("grade").getValue(String::class.java)
                    if (grade != null) gradeList.add(grade)
                }

                val spinnerAdapter = ArrayAdapter(
                    this@RemoveApproveStudentActivityAdmin,
                    android.R.layout.simple_spinner_item,
                    gradeList
                )
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = spinnerAdapter

                // Setup spinner selection listener
                setupSpinnerListener(spinner)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.i(TAG, "Failed to read value.", databaseError.toException())
            }
        })
    }

    private fun setupSpinnerListener(spinner: Spinner) {
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedGrade = parent.getItemAtPosition(position) as String
                fetchStudentsForGrade(selectedGrade)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
    }

    private fun fetchStudentsForGrade(grade: String) {
        val studentsRef = FirebaseDatabase.getInstance("https://innogeeks2024-default-rtdb.firebaseio.com/")
            .getReference("students")

        studentsRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val newStudentList = ArrayList<Student>()

                for (child in dataSnapshot.children) {
                    val name = child.child("name").getValue(String::class.java) ?: continue
                    val assigned = child.child("assigned").getValue(Boolean::class.java) ?: false
                    val studentID = child.child("studentID").getValue(String::class.java) ?: continue
                    val studentGrade = child.child("grade").getValue(String::class.java)
                    val approved = child.child("approved").getValue(Boolean::class.java) ?: false
                    val currentStudent = child.child("currentStudent").getValue(Boolean::class.java) ?: false

                    if (!approved && currentStudent && assigned && studentGrade == grade) {
                        newStudentList.add(Student(name, assigned, studentID))
                    }
                }

                studentToDisplay.clear()
                studentToDisplay.addAll(newStudentList)
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w(TAG, "Failed to read value.", error.toException())
            }
        })
    }

    companion object {
        private const val TAG = "RemoveApproveActivity"
    }
}