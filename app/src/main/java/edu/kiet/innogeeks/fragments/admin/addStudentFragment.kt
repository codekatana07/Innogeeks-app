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
import edu.kiet.innogeeks.R
import edu.kiet.innogeeks.databinding.FragmentAddStudentBinding
import edu.kiet.innogeeks.databinding.FragmentAddTeacherBinding
import edu.kiet.innogeeks.model.User


class addStudentFragment : Fragment() {
    private var _binding: FragmentAddStudentBinding? = null
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
        _binding = FragmentAddStudentBinding.inflate(inflater, container, false)
        return binding.root
    }
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupAdapter()
        setupDomainSpinner()
        setupButtonClickListener() // Add this new function call
        fetchUsers()
    }

    // Add this new function to setup the button click listener
    private fun setupButtonClickListener() {
        binding.buttonAction.setOnClickListener {
            assignCoordinators()
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
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {
                        selectedDomain = ""
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

    private fun fetchUsers() {
        db.collection("users")
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
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to fetch users: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun assignCoordinators() {
        selectedDomain?.let { domain ->
            val batch = db.batch()
            val domainRef = db.collection("Domains").document(domain)

            selectedUserIds.forEach { userId ->
                val userDetails = userDetails[userId] ?: return@forEach
                val coordinatorRef = domainRef.collection("Students").document(userId)
                batch.set(coordinatorRef, userDetails)
            }

            batch.commit()
                .addOnSuccessListener {
                    Toast.makeText(requireContext(),
                        "Successfully assigned ${selectedUserIds.size} students to $domain",
                        Toast.LENGTH_SHORT).show()
                    clearSelection()
                    // Navigate back to admin dashboard
                    requireActivity().onBackPressed()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(requireContext(),
                        "Failed to assign students: ${e.message}",
                        Toast.LENGTH_SHORT).show()
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