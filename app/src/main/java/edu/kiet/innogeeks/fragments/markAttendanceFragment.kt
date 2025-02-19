package edu.kiet.innogeeks

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import edu.kiet.innogeeks.databinding.FragmentMarkAttendanceBinding
import edu.kiet.innogeeks.adapter.MarkAttendanceAdapter
import java.text.SimpleDateFormat
import java.util.*

class markAttendanceFragment : Fragment() {

    private var _binding: FragmentMarkAttendanceBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MarkAttendanceAdapter
    private lateinit var db: FirebaseFirestore
    private var coordinatorDomain: String? = null
    private var currentDate: String = ""
    private var hasAttendanceMarkedToday: Boolean = false  // Add this flag

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMarkAttendanceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentDate = getCurrentDate()
        db = FirebaseFirestore.getInstance()
        val firebaseAuth = FirebaseAuth.getInstance()
        val coordinatorId = firebaseAuth.currentUser?.uid

        // Initialize adapter
        adapter = MarkAttendanceAdapter()

        // Setup RecyclerView
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@markAttendanceFragment.adapter
        }

        // Fetch coordinator data and domain
        coordinatorId?.let { fetchCoordinatorData(it) }

        // Set submit button click listener
        binding.btnSubmit.setOnClickListener {
            submitAttendance()
        }
    }

    private fun fetchCoordinatorData(coordinatorId: String) {
        db.collection("Domains").get()
            .addOnSuccessListener { domains ->
                var found = false
                for (domain in domains) {
                    domain.reference.collection("Coordinators")
                        .document(coordinatorId)
                        .get()
                        .addOnSuccessListener { coordinator ->
                            if (coordinator.exists() && !found) {
                                found = true
                                coordinatorDomain = domain.id
                                binding.coordinatorName.text = "Coordinator: ${coordinator.getString("name")}"
                                binding.coordinatorDomain.text = "Domain: ${domain.id}"

                                // Check if attendance is already marked for today
                                checkTodayAttendance(domain.id)

                                // After finding the domain, fetch its students
                                fetchStudentData(domain.id)
                            }
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error fetching domains", e)
                Toast.makeText(requireContext(), "Failed to load coordinator data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkTodayAttendance(domain: String) {
        db.collection("Domains")
            .document(domain)
            .get()
            .addOnSuccessListener { document ->
                val attendanceMap = document.get("attendance") as? Map<*, *>
                hasAttendanceMarkedToday = attendanceMap?.containsKey(currentDate) == true

                if (hasAttendanceMarkedToday) {
                    binding.btnSubmit.isEnabled = false
                    Toast.makeText(requireContext(), "Attendance already marked for today", Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun fetchStudentData(domain: String) {
        db.collection("Domains")
            .document(domain)
            .collection("Students")
            .get()
            .addOnSuccessListener { documents ->
                val studentList = documents.map { doc ->
                    Pair(
                        Pair(
                            doc.getString("name") ?: "",
                            doc.getString("library-id") ?: ""
                        ),
                        doc.id
                    )
                }
                adapter.updateStudents(studentList)
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error fetching students", e)
                Toast.makeText(requireContext(), "Failed to load students", Toast.LENGTH_SHORT).show()
            }
    }

    private fun submitAttendance() {
        if (hasAttendanceMarkedToday) {
            Toast.makeText(requireContext(), "Attendance already marked for today", Toast.LENGTH_LONG).show()
            return
        }

        val presentStudents = adapter.getCheckedStudentIds()

        if (presentStudents.isEmpty()) {
            Toast.makeText(requireContext(), "Please mark at least one student", Toast.LENGTH_SHORT).show()
            return
        }

        coordinatorDomain?.let { domain ->
            // Disable submit button to prevent double submission
            binding.btnSubmit.isEnabled = false

            val domainRef = db.collection("Domains").document(domain)

            // Create batch
            val batch = db.batch()

            // 1. Increment totalDaysHeld for the domain
            batch.update(domainRef, "totalDaysHeld", FieldValue.increment(1))

            // 2. Add present students to attendance map for current date
            val attendanceUpdate = mapOf(
                "attendance.$currentDate" to presentStudents
            )
            batch.update(domainRef, attendanceUpdate)

            // 3. Update totalPresentDays for each present student
            presentStudents.forEach { studentId ->
                val studentRef = domainRef.collection("Students").document(studentId)
                batch.update(studentRef, "totalPresentDays", FieldValue.increment(1))
            }

            // Execute batch
            batch.commit()
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Attendance marked successfully", Toast.LENGTH_SHORT).show()
                    requireActivity().onBackPressed() // Go back after successful submission
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Error marking attendance", e)
                    Toast.makeText(requireContext(), "Failed to mark attendance", Toast.LENGTH_SHORT).show()
                    // Re-enable submit button on failure
                    binding.btnSubmit.isEnabled = true
                }
        }
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        return sdf.format(Date())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}