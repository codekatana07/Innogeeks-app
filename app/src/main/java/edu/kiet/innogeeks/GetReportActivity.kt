package edu.kiet.innogeeks

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.*

class GetReportActivity : AppCompatActivity() {
    private lateinit var presentList: ArrayList<String>
    private lateinit var gradeList: ArrayList<String>
    private lateinit var dateList: ArrayList<String>
    private lateinit var sectionList: ArrayList<String>

    private lateinit var attendance: Map<String, Boolean>
    private lateinit var database: FirebaseDatabase

    private lateinit var students: ArrayList<Student>
    private lateinit var presentSpinner: Spinner
    private lateinit var gradeSpinner: Spinner
    private lateinit var dateSpinner: Spinner
    private lateinit var sectionSpinner: Spinner

    private lateinit var gradeRef: DatabaseReference
    private lateinit var studentRef: DatabaseReference

    private lateinit var presentValue: String
    private lateinit var gradeValue: String
    private lateinit var dateValue: String
    private lateinit var sectionValue: String

    fun check(view: View) {
        presentValue = presentSpinner.selectedItem.toString()
        gradeValue = gradeSpinner.selectedItem.toString()
        dateValue = dateSpinner.selectedItem.toString()
        sectionValue = sectionSpinner.selectedItem.toString()
        students = ArrayList()

        studentRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val stdID = dataSnapshot.child("studentID").getValue(String::class.java) ?: return
                val grade = dataSnapshot.child("grade").getValue(String::class.java) ?: return
                val name = dataSnapshot.child("name").getValue(String::class.java) ?: return
                val section = dataSnapshot.child("section").getValue(String::class.java) ?: return
                val approved = dataSnapshot.child("approved").getValue(Boolean::class.java) ?: false
                val currentStd = dataSnapshot.child("currentStudent").getValue(Boolean::class.java) ?: false
                val assigned = dataSnapshot.child("assigned").getValue(Boolean::class.java) ?: false

                if (approved == true && currentStd == true && assigned == true) {
                    val std = Student(stdID, grade, name, section)
                    val currentStudentValue = dataSnapshot.child("currentStudent").getValue(Boolean::class.java)
                    if (currentStudentValue == true) {
                        val attendanceRef = dataSnapshot.ref.child("attendance")
                        attendanceRef.addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(dataSnapshot: DataSnapshot) {
                                for (attendanceSnapshot in dataSnapshot.children) {
                                    for (child in attendanceSnapshot.children) {
                                        if (isDateFormatValid(child.key)) {
                                            std.addAttendance(child.key!!, child.getValue(Boolean::class.java) ?: false)
                                        }
                                    }
                                }
                                students.add(std)
                                analyseDate(students)
                            }

                            override fun onCancelled(databaseError: DatabaseError) {
                                // Handle error
                            }
                        })
                    }
                }
            }

            private fun isDateFormatValid(input: String?): Boolean {
                if (input == null) return false
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                dateFormat.isLenient = false

                return try {
                    dateFormat.parse(input)
                    true
                } catch (e: ParseException) {
                    false
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun analyseDate(students: ArrayList<Student>) {
        println(students)
        val studentList = ArrayList<String>()
        val listView = findViewById<ListView>(R.id.listView)

        for (std in students) {
            std.calculateAttendance()
            if (std.getReport(presentValue, gradeValue, dateValue, sectionValue)) {
                val temp = "${std.stdName} in ${std.studentSection} section is $presentValue on $dateValue and has attendance percentage of ${String.format("%.3f", std.percentage * 100)}%"
                studentList.add(temp)
            }
        }

        val total = findViewById<TextView>(R.id.totalCount)
        val present = findViewById<TextView>(R.id.presentCount)
        val absent = findViewById<TextView>(R.id.absentCount)

        var presentCount = 0
        var absentCount = 0

        for (std in students) {
            if (std.getReport("Present", gradeValue, dateValue, sectionValue)) {
                presentCount++
            }
            if (std.getReport("Absent", gradeValue, dateValue, sectionValue)) {
                absentCount++
            }
        }

        total.text = "Total Count: ${presentCount + absentCount}"
        present.text = "Present Count: $presentCount"
        absent.text = "Absent Count: $absentCount"

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, studentList)
        listView.adapter = adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_report)

        presentList = ArrayList()
        gradeList = ArrayList()
        dateList = ArrayList()
        sectionList = ArrayList()

        gradeList.add("Any")
        sectionList.add("Any")

        presentList.apply {
            add("Present")
            add("Absent")
        }

        sectionList.apply {
            add("Bio")
            add("Maths")
            add("Arts")
            add("Commerce")
        }

        database = FirebaseDatabase.getInstance("https://innogeeks2024-default-rtdb.firebaseio.com/")

        presentSpinner = findViewById(R.id.spinner_present)
        gradeSpinner = findViewById(R.id.spinner_grade)
        dateSpinner = findViewById(R.id.spinner_date)
        sectionSpinner = findViewById(R.id.spinner_section)

        gradeRef = database.getReference("Classes")
        studentRef = database.getReference("students")

        ArrayAdapter(this, android.R.layout.simple_spinner_item, presentList).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            presentSpinner.adapter = adapter
        }

        ArrayAdapter(this, android.R.layout.simple_spinner_item, sectionList).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            sectionSpinner.adapter = adapter
        }

        gradeRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                for (snapshot in dataSnapshot.children) {
                    snapshot.child("grade").getValue(String::class.java)?.let { grade ->
                        gradeList.add(grade)
                    }
                }

                ArrayAdapter(this@GetReportActivity, android.R.layout.simple_spinner_item, gradeList).also { adapter ->
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    gradeSpinner.adapter = adapter
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.i("TAG", "Failed to read value.", databaseError.toException())
            }
        })

        studentRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(dataSnapshot: DataSnapshot, previousChildName: String?) {
                val currentStudentValue =
                    dataSnapshot.child("currentStudent").getValue(Boolean::class.java)
                if (currentStudentValue == true) {
                    val attendanceRef = dataSnapshot.ref.child("attendance")
                    attendanceRef.addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            for (attendanceSnapshot in dataSnapshot.children) {
                                for (child in attendanceSnapshot.children) {
                                    if (isDateFormatValid(child.key)) {
                                        dateList.add(child.key!!)
                                    }
                                }
                            }
                            ArrayAdapter(
                                this@GetReportActivity,
                                android.R.layout.simple_spinner_item,
                                dateList
                            ).also { adapter ->
                                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                                dateSpinner.adapter = adapter
                            }
                        }

                        override fun onCancelled(databaseError: DatabaseError) {
                            // Handle error
                        }
                    })
                    studentRef.removeEventListener(this)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                TODO("Not yet implemented")
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

            private fun isDateFormatValid(input: String?): Boolean {
                if (input == null) return false
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                dateFormat.isLenient = false

                return try {
                    dateFormat.parse(input)
                    true
                } catch (e: ParseException) {
                    false
                }
            }
        })
    }
}





