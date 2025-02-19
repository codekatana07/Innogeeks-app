package edu.kiet.innogeeks.fragments.admin

import UserListAdapter
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import edu.kiet.innogeeks.databinding.FragmentRemoveStudentBinding
import edu.kiet.innogeeks.model.User


class removeStudentFragment : Fragment() {
    private var _binding: FragmentRemoveStudentBinding? = null
    private val binding get() = _binding!!

    private val selectedUserIds = mutableSetOf<String>()
    private val userAdapter = UserListAdapter()
    private val db = FirebaseFirestore.getInstance()
    private var selectedDomain: String? = null
    private val userDetails = mutableMapOf<String, Map<String, Any>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRemoveStudentBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupDomainSpinner()
        setupRecyclerView()
        setupAdapter()

        setupButtonClickListener() // Add this new function call
    }
    private fun setupButtonClickListener() {
        binding.buttonAction.setOnClickListener {
            if (selectedUserIds.isNotEmpty() && selectedDomain != null) {
                removeCoordinators()
            } else {
                Toast.makeText(requireContext(), "Please select coordinators to remove", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun setupDomainSpinner() {
        db.collection("Domains").get()
            .addOnSuccessListener { documents ->
                val domains = documents.map { it.id }.sorted()

                if (domains.isEmpty()) {
                    Toast.makeText(requireContext(), "No domains available", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val adapter = ArrayAdapter(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    domains
                ).apply {
                    setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                }

                binding.spinnerDomain.adapter = adapter

                binding.spinnerDomain.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        selectedDomain = domains[position]
                        updateActionButton()
                        // Fetch coordinators for the selected domain
                        fetchCoordinators(selectedDomain!!)
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        selectedDomain = null
                        updateActionButton()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to load domains", Toast.LENGTH_SHORT).show()
                Log.e("setupDomainSpinner", "Error fetching domains", e)
            }
    }

    private fun setupRecyclerView() {
        binding.recyclerViewUsers.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = userAdapter
        }
    }

    private fun setupAdapter() {
        userAdapter.onItemCheckedChanged = { user, isChecked ->
            if (isChecked) {
                selectedUserIds.add(user.id)
            } else {
                selectedUserIds.remove(user.id)
            }
            updateSelectionCount()
            updateActionButton()
        }
    }

    private fun updateSelectionCount() {
        binding.textViewSelectedCount.text = "Selected Students: ${selectedUserIds.size}"
    }

    private fun updateActionButton() {
        binding.buttonAction.isEnabled = selectedUserIds.isNotEmpty() && selectedDomain != null
    }

    private fun fetchCoordinators(domain:String) {
        selectedUserIds.clear()
        updateSelectionCount()
        db.collection("Domains")
            .document(domain)
            .collection("Students")
            .get()
            .addOnSuccessListener { documents ->
                val users = documents.map { doc ->
                    // Store full user details for later use
                    userDetails[doc.id] = doc.data
                    // Create User object for adapter
                    User(
                        id = doc.id,
                        name = doc.getString("name") ?: "N/A",
                        email = doc.getString("email") ?: "N/A",
                        isSelected = false
                    )
                }
                userAdapter.submitList(users)
                if (users.isEmpty()) {
                    Toast.makeText(requireContext(), "No students found in this domain", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to fetch users: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("fetchstudents", "Error fetching students", e)

            }
    }

    private fun removeCoordinators() {
        selectedDomain?.let { domain ->
            binding.buttonAction.isEnabled = false // Disable button while processing

            val batch = db.batch()
            val domainRef = db.collection("Domains").document(domain)

            selectedUserIds.forEach { userId ->
                // Get the coordinator details from our stored map
                val studentDetails = userDetails[userId]

                if (studentDetails != null) {
                    // First, save the coordinator details to users collection
                    val userRef = db.collection("users").document(userId)
                    batch.set(userRef, studentDetails)

                    // Then delete from domain's Coordinators subcollection
                    val coordinatorRef = domainRef.collection("Students").document(userId)
                    batch.delete(coordinatorRef)
                }
            }

            batch.commit()
                .addOnSuccessListener {
                    Toast.makeText(
                        requireContext(),
                        "Successfully moved ${selectedUserIds.size} student(s) to users",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Refresh the coordinator list
                    fetchCoordinators(domain)

                    // Clear selection
                    clearSelection()

                    // Use a slight delay before navigation to ensure Toast is visible
                    // and prevent rapid back stack changes
                    binding.root.postDelayed({
                        // Check if fragment is still attached before navigating
                        if (isAdded) {
                            // Use parentFragmentManager to pop back stack instead of activity.onBackPressed()
                            parentFragmentManager.popBackStack()
                        }
                    }, 500) // Half second delay
                }
                .addOnFailureListener { e ->
                    binding.buttonAction.isEnabled = true // Re-enable button on failure
                    Toast.makeText(
                        requireContext(),
                        "Failed to move students: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    Log.e("removeStudents", "Error moving students", e)
                }
        }
    }
    private fun clearSelection() {
        selectedUserIds.clear()
        userAdapter.currentList.forEach { it.isSelected = false }
        userAdapter.notifyDataSetChanged()
        updateSelectionCount()
        updateActionButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}