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

private const val TAG = "markAttendanceFragment"

class markAttendanceFragment : Fragment() {


    private var _binding: FragmentMarkAttendanceBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: MarkAttendanceAdapter
    private lateinit var db: FirebaseFirestore
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

        // Set submit button click listener
        binding.btnSubmit.setOnClickListener {
            submitAttendance()
        }

        // Initialize the view with user data
        initializeView()
    }
    private fun initializeView() {
        val userData = UserDataManager.getCurrentUser()
        if (userData == null) {
            showToast("User data not available")
            return
        }

        if (userData.role != "Coordinators") {
            showToast("Only coordinators can mark attendance")
            binding.btnSubmit.isEnabled = false
            return
        }

        // Display coordinator info
        binding.apply {
            coordinatorName.text = "Coordinator: ${userData.name}"
            coordinatorDomain.text = "Domain: ${userData.domain}"
        }

        // Check if attendance is already marked for today
        checkTodayAttendance(userData.domain)

        // Fetch students for the domain
        fetchStudentData(userData.domain)
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
                    showToast("Attendance already marked for today")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking today's attendance", e)
                showToast("Failed to check today's attendance status")
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
                Log.e(TAG, "Error fetching students", e)
                showToast("Failed to load students")
            }
    }
    private fun submitAttendance() {
        val userData = UserDataManager.getCurrentUser() ?: run {
            showToast("User data not available")
            return
        }

        if (hasAttendanceMarkedToday) {
            showToast("Attendance already marked for today")
            return
        }

        val presentStudents = adapter.getCheckedStudentIds()
        if (presentStudents.isEmpty()) {
            showToast("Please mark at least one student")
            return
        }

        // Disable submit button to prevent double submission
        binding.btnSubmit.isEnabled = false

        val domainRef = db.collection("Domains").document(userData.domain)
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
                showToast("Attendance marked successfully")
                requireActivity().onBackPressed()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error marking attendance", e)
                showToast("Failed to mark attendance")
                binding.btnSubmit.isEnabled = true
            }
    }

    private fun getCurrentDate(): String {
        return SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
