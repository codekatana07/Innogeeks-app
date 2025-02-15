package edu.kiet.innogeeks.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.kiet.innogeeks.R

class MarkAttendanceAdapter : RecyclerView.Adapter<MarkAttendanceAdapter.AttendanceViewHolder>() {

    private var students = mutableListOf<Pair<Pair<String, String>, String>>() // ((name, libraryId), uid)
    private val checkedStudents = mutableSetOf<String>()

    fun updateStudents(newStudents: List<Pair<Pair<String, String>, String>>) {
        students.clear()
        students.addAll(newStudents)
        checkedStudents.clear()
        notifyDataSetChanged()
    }

    fun getCheckedStudentIds(): List<String> = checkedStudents.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AttendanceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.attendance_student_item, parent, false)
        return AttendanceViewHolder(view)
    }

    override fun onBindViewHolder(holder: AttendanceViewHolder, position: Int) {
        val student = students[position]
        holder.bind(student.first.first, student.first.second, student.second)
    }

    override fun getItemCount(): Int = students.size

    inner class AttendanceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.studentName)
        private val libraryIdTextView: TextView = itemView.findViewById(R.id.studentLibraryId)
        private val checkBox: CheckBox = itemView.findViewById(R.id.attendanceCheckbox)

        fun bind(name: String, libraryId: String, uid: String) {
            nameTextView.text = name
            libraryIdTextView.text = libraryId

            // Set checkbox state
            checkBox.isChecked = checkedStudents.contains(uid)

            // Set click listeners for both the checkbox and the entire item
            checkBox.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    checkedStudents.add(uid)
                } else {
                    checkedStudents.remove(uid)
                }
            }

            // Make the entire item clickable
            itemView.setOnClickListener {
                checkBox.isChecked = !checkBox.isChecked
            }
        }
    }
}