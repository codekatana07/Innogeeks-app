package edu.kiet.innogeeks

import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Spinner
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.RecyclerListener
import com.google.firebase.database.*
import java.util.*

class RemoveApproveStudentActivityAdmin : AppCompatActivity() {
    private lateinit var adapter: CustomAdapter
    private lateinit var studentToDisplay: ArrayList<Student>
    private lateinit var listView: RecyclerView
    private lateinit var reference: DatabaseReference
    private lateinit var checkedIDs: ArrayList<String>
    private lateinit var gradeList: ArrayList<String>

    fun remove(view: View) {
        checkedIDs = adapter.getCheckedIDs()
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
                    Log.e(TAG, "Failed to update grade: " + databaseError.message)
                }
            })
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_approve_add_studnet)

        val database = FirebaseDatabase.getInstance("https://innogeeks2024-default-rtdb.firebaseio.com/")
        val myRef = database.getReference("students")

        val gradeRef = database.getReference("Classes")
        gradeList = ArrayList()
        studentToDisplay = ArrayList()
        listView = findViewById(R.id.listView)

        gradeRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                gradeList.clear()
                val spinner: Spinner = findViewById(R.id.spinner_grade)
                for (snapshot in dataSnapshot.children) {
                    val grade = snapshot.child("grade").getValue(String::class.java)
                    if (grade != null) gradeList.add(grade)
                }

                val adapter = ArrayAdapter(
                    this@RemoveApproveStudentActivityAdmin,
                    android.R.layout.simple_spinner_item,
                    gradeList
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.i("TAG", "Failed to read value.", databaseError.toException())
            }
        })

        val spinner: Spinner = findViewById(R.id.spinner_grade)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val grade = parent.getItemAtPosition(position) as String
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

                            if (approved == false && currentStd == true && assigned == true && studentGrade == grade) {
                                studentToDisplay.add(Student(stdName.toString(), assigned,
                                    stdID.toString()
                                ))
                            }
                        }
                        adapter.notifyDataSetChanged()
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w(TAG, "Failed to read value.", error.toException())
                    }
                })
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        // Initialize the adapter with the student data
        val adapter = CustomAdapter(studentToDisplay, applicationContext)
        listView.adapter = adapter // Replace listView with recyclerView if it's a RecyclerView

// Set the onItemClickListener inside the adapter
        adapter.setOnItemClickListener(object : CustomAdapter.OnItemClickListener {
            override fun onItemClick(position: Int) {
                val student = studentToDisplay[position]
                student.assigned = !student.assigned // Toggle the assigned value
                adapter.notifyDataSetChanged() // Refresh the RecyclerView
            }
        })

    }
}
