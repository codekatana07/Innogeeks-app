package edu.kiet.innogeeks.fragments

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.kiet.innogeeks.R
import edu.kiet.innogeeks.databinding.FragmentStudentHomeBinding
import edu.kiet.innogeeks.markAttendanceFragment


class studentHomeFragment : Fragment() {

    private var _binding: FragmentStudentHomeBinding?= null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var studentDomain: String? = null
    private var studentUID: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentStudentHomeBinding.inflate(inflater,container,false)
        // Inflate the layout for this fragment
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        binding.parentNote.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.frame, reasonOfAbsenceFragment())
                .addToBackStack(null)
                .commit()
        }

        setupUI()
        fetchUserData()


    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupUI() {

        // Initialize progress indicator
        binding.attendanceProgress.apply {
            trackThickness = 8
            indicatorSize = 120
            trackColor = Color.parseColor("#E0E0E0")
        }
    }

    private fun fetchUserData() {
        val userId = auth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
            return
        }
        studentUID = userId

        // Fetch student data
        db.collection("Domains")
            .get()
            .addOnSuccessListener { domainDocs ->
                for (domainDoc in domainDocs) {
                    db.collection("Domains")
                        .document(domainDoc.id)
                        .collection("Students")
                        .document(studentUID!!)
                        .get()
                        .addOnSuccessListener { studentDoc ->
                            if (studentDoc.exists()) {
                                val name = studentDoc.getString("name") ?: "Student"
                                studentDomain = domainDoc.id
                                binding.textView.text = "Welcome! $name"

                                val totalPresentDays = studentDoc.getLong("totalPresentDays") ?: 0L
                                fetchTotalDays(studentDomain!!, totalPresentDays)
                            } else {
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load student data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchTotalDays(domain: String, presentDays: Long) {

        db.collection("Domains").document(domain)
            .get()
            .addOnSuccessListener { domainDoc ->
                if (domainDoc.exists()) {
                    val totalDaysHeld = domainDoc.getLong("totalDaysHeld") ?: 0L

                    updateAttendanceUI(presentDays, totalDaysHeld)
                } else {
                    updateAttendanceUI(presentDays, 0)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to load domain data", Toast.LENGTH_SHORT).show()
                updateAttendanceUI(presentDays, 0)
            }
    }

    private fun updateAttendanceUI(presentDays: Long, totalDays: Long) {

        val percentage = if (totalDays > 0) {
            (presentDays.toFloat() / totalDays.toFloat() * 100).toInt()
        } else {
            0
        }

        with(binding) {
            // Update progress indicator
            attendanceProgress.progress = percentage
            percentageText.text = "$percentage%"

            // Format the attendance text
            daysPresent.text = if (totalDays > 0) {
                "Present: $presentDays days out of $totalDays days"
            } else {
                "No attendance records yet"
            }

            // Set color based on attendance percentage
            val color = when {
                percentage >= 75 -> Color.parseColor("#4CAF50") // Green for good attendance
                percentage >= 60 -> Color.parseColor("#FFA000") // Orange for average attendance
                else -> Color.parseColor("#F44336") // Red for poor attendance
            }

            // Update UI colors
            attendanceProgress.setIndicatorColor(color)
            percentageText.setTextColor(color)
        }
    }


}