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
import edu.kiet.innogeeks.CustomAdapter
import edu.kiet.innogeeks.R
import edu.kiet.innogeeks.Student

class EnrolStudentActivity : AppCompatActivity() {

    private lateinit var adapter: CustomAdapter
    private val studentToDisplay = ArrayList<Student>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var checkedIDs: ArrayList<String>
    private lateinit var gradeList: ArrayList<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_enroll_student)

        val database = FirebaseDatabase.getInstance("https://innogeeks2024-default-rtdb.firebaseio.com/")
        val myRef = database.getReference("students")

        recyclerView = findViewById(R.id.listView) // Initialize RecyclerView
        gradeList = ArrayList()

        // Set up the RecyclerView with LinearLayoutManager
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize the adapter and set it to the RecyclerView
        adapter = CustomAdapter(studentToDisplay, applicationContext)
        recyclerView.adapter = adapter

        // Handle grade selection from the Spinner
        val spinner: Spinner = findViewById(R.id.spinner_grade)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val grade = parent.getItemAtPosition(position).toString()
                myRef.addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        studentToDisplay.clear()
                        for (child in dataSnapshot.children) {
                            val stdName = child.child("name").getValue(String::class.java) ?: return
                            val assigned = child.child("assigned").getValue(Boolean::class.java)
                            val stdID = child.child("studentID").getValue(String::class.java)?:return
                            val gradeInDb = child.child("grade").getValue(String::class.java)

                            if (assigned != null && !assigned && gradeInDb == grade) {
                                studentToDisplay.add(Student(stdName, assigned, stdID))
                            }
                        }
                        adapter.notifyDataSetChanged()  // Notify adapter of data change
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Log.w("EnrolStudentActivity", "Failed to read value.", error.toException())
                    }
                })
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Do nothing
            }
        }

        // Populate the grade spinner
        val gradeRef = database.getReference("Classes")
        gradeRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                gradeList.clear()
                for (snapshot in dataSnapshot.children) {
                    val grade = snapshot.child("grade").getValue(String::class.java)
                    grade?.let { gradeList.add(it) }
                }

                val adapter = ArrayAdapter(
                    this@EnrolStudentActivity,
                    android.R.layout.simple_spinner_item,
                    gradeList
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinner.adapter = adapter
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.w("EnrolStudentActivity", "Failed to read value.", databaseError.toException())
            }
        })
    }

    fun add(view: View) {
        checkedIDs = adapter.getCheckedIDs() // Get the selected student IDs
        val spinner: Spinner = findViewById(R.id.spinner_grade)
        val selectedGrade = spinner.selectedItem.toString()

        val studentsRef = FirebaseDatabase.getInstance("https://innogeeks2024-default-rtdb.firebaseio.com/").getReference("students")

        // Update selected students' grades in the database
        for (stdID in checkedIDs) {
            val query = studentsRef.orderByChild("studentID").equalTo(stdID)
            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    for (childSnapshot in dataSnapshot.children) {
                        val studentRef = childSnapshot.ref
                        studentRef.child("grade").setValue(selectedGrade)
                        studentRef.child("assigned").setValue(true)
                        studentRef.child("approved").setValue(false)
                    }
                }

                override fun onCancelled(databaseError: DatabaseError) {
                    Log.e("EnrolStudentActivity", "Failed to update grade: " + databaseError.message)
                }
            })
        }
    }
}
