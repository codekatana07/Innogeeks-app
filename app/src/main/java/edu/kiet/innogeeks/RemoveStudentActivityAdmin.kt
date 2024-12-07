package edu.kiet.innogeeks

import android.content.ContentValues.TAG
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

class RemoveStudentActivityAdmin : AppCompatActivity() {

    private lateinit var adapter: CustomAdapter
    private lateinit var studentToDisplay: ArrayList<Student>
    private lateinit var recyclerView: RecyclerView
    private lateinit var checkedIDs: ArrayList<String>
    private lateinit var gradeList: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remove_student)

        recyclerView = findViewById(R.id.listView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val database = FirebaseDatabase.getInstance("https://attendme-644ac-default-rtdb.asia-southeast1.firebasedatabase.app/")
        val myRef = database.getReference("students")
        val gradeRef = database.getReference("Classes")

        gradeList = ArrayList()
        studentToDisplay = ArrayList()

        gradeRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                gradeList.clear()
                val spinner: Spinner = findViewById(R.id.spinner_grade)
                // Iterate through the database snapshot
                for (snapshot in dataSnapshot.children) {
                    val grade = snapshot.child("grade").getValue(String::class.java)
                    grade?.let { gradeList.add(it) }
                }

                // Update the spinner
                val adapter = ArrayAdapter(this@RemoveStudentActivityAdmin, android.R.layout.simple_spinner_item, gradeList)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.i(TAG, "Failed to read value.", databaseError.toException())
            }
        })

        val spinner: Spinner = findViewById(R.id.spinner_grade)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val grade = parent.getItemAtPosition(position).toString()
                myRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        studentToDisplay.clear()
                        for (child in dataSnapshot.children) {
                            val stdName = child.child("name").getValue(String::class.java)
                            val assigned = child.child("assigned").getValue(Boolean::class.java)
                            val stdID = child.child("studentID").getValue(String::class.java)
                            val studentGrade = child.child("grade").getValue(String::class.java)
                            val approved = child.child("approved").getValue(Boolean::class.java)
                            val currentStd = child.child("currentStudent").getValue(Boolean::class.java)

                            if (currentStd == true && assigned == true && studentGrade == grade) {
                                studentToDisplay.add(Student(stdName.toString(), assigned == true,
                                    stdID.toString()
                                ))
                            }
                        }
                        adapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        Log.w(TAG, "Failed to read value.", databaseError.toException())
                    }
                })
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        adapter = CustomAdapter(studentToDisplay, applicationContext)
        recyclerView.adapter = adapter

        // Set up item click listener in the RecyclerView adapter
        adapter.setOnItemClickListener(object : CustomAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                val student = studentToDisplay[position]
                student.assigned = !student.assigned // Toggle the assigned value
                adapter.notifyDataSetChanged() // Refresh the RecyclerView
            }
        })
    }

    fun remove(view: View) {
        checkedIDs = adapter.getCheckedIDs()
        val studentsRef = FirebaseDatabase.getInstance("https://attendme-644ac-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("students")

        for (stdID in checkedIDs) {
            val query = studentsRef.orderByChild("studentID").equalTo(stdID)
            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (childSnapshot in dataSnapshot.children) {
                        val studentRef = childSnapshot.ref
                        studentRef.child("currentStudent").setValue(false)
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
}
