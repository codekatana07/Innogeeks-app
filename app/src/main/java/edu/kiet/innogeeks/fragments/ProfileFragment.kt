package edu.kiet.innogeeks.fragments

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import edu.kiet.innogeeks.R
import edu.kiet.innogeeks.UserDataManager
import edu.kiet.innogeeks.databinding.FragmentProfileBinding
import edu.kiet.innogeeks.model.UserData
import java.util.UUID

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private var isEditMode = false
    private var currentUser: UserData? = null
    private val db = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var selectedImageUri: Uri? = null

    private val CAMERA_PERMISSION = Manifest.permission.CAMERA
    private val STORAGE_PERMISSION = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_IMAGES
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    private val PERMISSIONS_CAMERA = arrayOf(CAMERA_PERMISSION)
    private val PERMISSIONS_GALLERY = arrayOf(STORAGE_PERMISSION)

    private val PERMISSION_REQUEST_CAMERA = 100
    private val PERMISSION_REQUEST_GALLERY = 101

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                binding.ivProfileImage.setImageURI(uri)
            }
        }
    }

    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.extras?.get("data")?.let { bitmap ->
                binding.ivProfileImage.setImageBitmap(bitmap as android.graphics.Bitmap)
                // Convert bitmap to URI
                val uri = getImageUriFromBitmap(bitmap)
                selectedImageUri = uri
            }
        }
    }

    private val requestMultiplePermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allPermissionsGranted = permissions.entries.all { it.value }
        if (allPermissionsGranted) {
            showImageSourceDialog()
        } else {
            Toast.makeText(context, "Permission denied. Cannot proceed without permissions.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
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

        binding.ivProfileImage.setOnClickListener {
            if (isEditMode) {
                showImageSourceDialog()
            }
        }
    }

    private fun loadUserData() {
        currentUser = UserDataManager.getCurrentUser()
        currentUser?.let { user ->
            binding.apply {
                // Check if user has an image URL in Firestore
                if (user.imageUrl != null && user.imageUrl.isNotEmpty()) {
                    // Load image from Firebase Storage using Glide
                    Glide.with(requireContext())
                        .load(user.imageUrl)
                        .placeholder(R.drawable.baseline_person_24)
                        .error(R.drawable.baseline_person_24)
                        .into(ivProfileImage)
                } else {
                    // Load default image resource
                    ivProfileImage.setImageResource(R.drawable.baseline_person_24)
                }

                etEmail.setText(user.email)
                etName.setText(user.name)
                tvRole.text = user.role
                tvDomain.text = if (user.domain == "none") "N/A" else user.domain
            }
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery", "Cancel")
        AlertDialog.Builder(requireContext())
            .setTitle("Select Profile Picture")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> checkAndRequestCameraPermission()
                    1 -> checkAndRequestGalleryPermission()
                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun checkAndRequestCameraPermission() {
        if (hasPermissions(PERMISSIONS_CAMERA)) {
            openCamera()
        } else {
            requestCameraPermissions()
        }
    }

    private fun checkAndRequestGalleryPermission() {
        if (hasPermissions(PERMISSIONS_GALLERY)) {
            openGallery()
        } else {
            requestGalleryPermissions()
        }
    }

    private fun hasPermissions(permissions: Array<String>): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestCameraPermissions() {
        if (shouldShowRequestPermissionRationale(PERMISSIONS_CAMERA)) {
            showPermissionRationaleDialog(
                "Camera Permission",
                "Camera permission is required to take photos for your profile picture.",
                PERMISSIONS_CAMERA
            )
        } else {
            requestMultiplePermissionsLauncher.launch(PERMISSIONS_CAMERA)
        }
    }

    private fun requestGalleryPermissions() {
        if (shouldShowRequestPermissionRationale(PERMISSIONS_GALLERY)) {
            showPermissionRationaleDialog(
                "Gallery Permission",
                "Storage permission is required to select photos from your gallery for your profile picture.",
                PERMISSIONS_GALLERY
            )
        } else {
            requestMultiplePermissionsLauncher.launch(PERMISSIONS_GALLERY)
        }
    }

    private fun shouldShowRequestPermissionRationale(permissions: Array<String>): Boolean {
        return permissions.any {
            ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(), it)
        }
    }

    private fun showPermissionRationaleDialog(title: String, message: String, permissions: Array<String>) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("Grant Permission") { _, _ ->
                requestMultiplePermissionsLauncher.launch(permissions)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(
                    requireContext(),
                    "Feature unavailable without permission",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .create()
            .show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }

    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    private fun getImageUriFromBitmap(bitmap: android.graphics.Bitmap): Uri {
        val path = MediaStore.Images.Media.insertImage(
            requireActivity().contentResolver,
            bitmap,
            "Profile_Image_" + System.currentTimeMillis(),
            null
        )
        return Uri.parse(path)
    }

    private fun enableEditMode() {
        isEditMode = true
        binding.apply {
            // UID, Role, and Domain are not editable
            etName.isEnabled = true
            ivProfileImage.isEnabled = true
            btnEditSave.text = "Save Profile"

            // Add a hint to show user they can change the image
            Toast.makeText(context, "Tap on image to change profile picture", Toast.LENGTH_SHORT).show()
        }
    }

    private fun disableEditMode() {
        isEditMode = false
        binding.apply {
            etName.isEnabled = false
            ivProfileImage.isEnabled = false
            btnEditSave.text = "Edit Profile"
        }
    }

    private fun saveProfile() {
        val updatedName = binding.etName.text.toString().trim()
        val updatedImageUrl = selectedImageUri?.toString()
        if (updatedName.isEmpty()) {
            binding.etName.error = "Name cannot be empty"
            return
        }

        // Show loading indicator
        showLoading(true)

        currentUser?.let { user ->
            val collectionPath = when (user.role) {
                "admin" -> "admins"
                "user" -> "users"
                else -> "Domains/${user.domain}/${user.role}" // Note: added 's' at the end
            }

            // Prepare updates map
            val updates = hashMapOf<String, Any>(
                "name" to updatedName
            )

            // Only add the imageUrl if it's not null
            if (selectedImageUri != null) {
                updates["imageUrl"] = selectedImageUri.toString()
            }

            // If image was selected, upload it first
            if (selectedImageUri != null) {
                uploadImageAndSaveProfile(collectionPath, user.uid, updates)
            } else {
                // No new image, just update the text fields
                updateProfileInFirestore(collectionPath, user.uid, updates)
            }
        }
    }

    private fun uploadImageAndSaveProfile(collectionPath: String, uid: String, updates: HashMap<String, Any>) {
        val storageRef = storage.reference.child("profile_images/${uid}/${UUID.randomUUID()}")

        selectedImageUri?.let { uri ->
            storageRef.putFile(uri)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                        // Add image URL to updates
                        updates["imageUrl"] = downloadUri.toString()
                        // Update Firestore with all fields including image URL
                        updateProfileInFirestore(collectionPath, uid, updates)
                    }.addOnFailureListener { e ->
                        showLoading(false)
                        Toast.makeText(context, "Failed to get download URL: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(context, "Failed to upload image: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun updateProfileInFirestore(collectionPath: String, uid: String, updates: HashMap<String, Any>) {
        db.collection(collectionPath)
            .document(uid)
            .update(updates)
            .addOnSuccessListener {
                showLoading(false)
                disableEditMode()
                Toast.makeText(context, "Profile updated successfully", Toast.LENGTH_SHORT).show()

                // Refresh user data to update the image in the UserDataManager
                UserDataManager.initialize { success ->
                    if (success) {
                        loadUserData()
                    }
                }
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(context, "Failed to update profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.btnEditSave.isEnabled = !isLoading
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSION_REQUEST_CAMERA -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    openCamera()
                } else {
                    Toast.makeText(requireContext(), "Camera permission denied", Toast.LENGTH_SHORT).show()
                }
            }
            PERMISSION_REQUEST_GALLERY -> {
                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    openGallery()
                } else {
                    Toast.makeText(requireContext(), "Gallery permission denied", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}