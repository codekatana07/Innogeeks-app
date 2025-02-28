package edu.kiet.innogeeks.fragments

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.kiet.innogeeks.UserDataManager
import edu.kiet.innogeeks.databinding.FragmentAddResourceBinding
import edu.kiet.innogeeks.model.Resource
import edu.kiet.innogeeks.model.UserData

class addResourceFragment : Fragment() {
    private var _binding: FragmentAddResourceBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private val domains = listOf("Android", "Web_2", "Web_3", "Ar-Vr", "Ml", "IoT")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddResourceBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val currentUser = UserDataManager.getCurrentUser()
        if (currentUser != null) {
            Log.d("addResourceFragment", "${currentUser.role} is logged in")
            setupUI(currentUser)
        } else {
            Toast.makeText(context, "User data not available", Toast.LENGTH_SHORT).show()
            binding.submitButton.isEnabled = false
        }
    }

    private fun setupUI(userData: UserData) {
        setupDomainSpinner()

        when (userData.role) {
            "admin", "Coordinators" -> {
                binding.submitButton.isEnabled = true
                binding.submitButton.setOnClickListener { addResource() }

                if (userData.role == "Coordinators" && userData.domain != "none") {
                    val domainIndex = domains.indexOf(userData.domain)
                    if (domainIndex != -1) {
                        binding.domainSpinner.setSelection(domainIndex)
                    }
                }
            }
            else -> {
                binding.submitButton.isEnabled = false
                Toast.makeText(context, "Unauthorized to add resources", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupDomainSpinner() {
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            domains
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.domainSpinner.adapter = adapter
    }

    private fun addResource() {
        val currentUser = UserDataManager.getCurrentUser() ?: run {
            Toast.makeText(context, "User data not available", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentUser.role != "admin" && currentUser.role != "Coordinators") {
            Toast.makeText(context, "Unauthorized to add resources", Toast.LENGTH_SHORT).show()
            return
        }

        val title = binding.titleInput.text.toString()
        val description = binding.descriptionInput.text.toString()
        val selectedDomain = binding.domainSpinner.selectedItem.toString()

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val resource = Resource(
            title = title,
            description = description,
            timestamp = System.currentTimeMillis(),
            authorName = currentUser.name,
            libraryId = currentUser.libraryId
        )

        db.collection("Domains")
            .document(selectedDomain)
            .collection("resources")
            .add(resource)
            .addOnSuccessListener {
                Toast.makeText(context, "Resource added successfully", Toast.LENGTH_SHORT).show()
                clearInputs()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to add resource", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearInputs() {
        hideKeyboard()
        binding.titleInput.text?.clear()
        binding.descriptionInput.text?.clear()


        val currentUser = UserDataManager.getCurrentUser()
        if (currentUser?.role == "admin") {
            binding.domainSpinner.setSelection(0)
        }
    }
    private fun hideKeyboard() {
        val imm = requireContext().getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view?.windowToken, 0)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}