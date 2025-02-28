package edu.kiet.innogeeks.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.firestore.FirebaseFirestore
import edu.kiet.innogeeks.R
import edu.kiet.innogeeks.UserDataManager
import edu.kiet.innogeeks.databinding.FragmentPersonalDetailsBinding
import edu.kiet.innogeeks.model.UserData

class personalDetailsFragment : Fragment() {

    private  var _binding: FragmentPersonalDetailsBinding? = null
    private val binding get() = _binding!!
    private var isEditMode = false
    private var currentUser: UserData? = null
    private val db = FirebaseFirestore.getInstance()




    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPersonalDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Load initial data
        loadUserData()

        // Set up button click listener
        binding.btnEditSave.setOnClickListener {
            if (isEditMode) {
                saveProfile()
            } else {
                enableEditMode()
            }
        }
    }

    private fun loadUserData() {
        currentUser = UserDataManager.getCurrentUser()
        currentUser?.let { user ->
            binding.apply {
                tvUid.text = user.uid
                etEmail.setText(user.email)
                etName.setText(user.name)
                tvRole.text = user.role
                tvDomain.text = if (user.domain == "none") "N/A" else user.domain
                etLibraryId.setText(user.libraryId)
            }
        }
    }

    private fun enableEditMode() {
        isEditMode = true
        binding.apply {
            // UID, Role, and Domain are not editable
            etName.isEnabled = true
            etLibraryId.isEnabled = true
            btnEditSave.text = "Save Profile"
        }
    }

    private fun disableEditMode() {
        isEditMode = false
        binding.apply {
            etName.isEnabled = false
            etLibraryId.isEnabled = false
            btnEditSave.text = "Edit Profile"
        }
    }
    private fun saveProfile() {
        val updatedName = binding.etName.text.toString().trim()
        val updatedLibraryId = binding.etLibraryId.text.toString().trim()

        if (updatedName.isEmpty()) {
            binding.etName.error = "Name cannot be empty"
            return
        }
        currentUser?.let { user ->
            val collectionPath = when (user.role) {
                "admin" -> "admins"
                "user" -> "users"
                else -> "Domains/${user.domain}/${user.role}"
            }

            val updates = hashMapOf<String, Any>(
                "name" to updatedName,
                "library-id" to updatedLibraryId
            )

            db.collection(collectionPath)
                .document(user.uid)
                .update(updates)
                .addOnSuccessListener {
                    disableEditMode()
                    Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}