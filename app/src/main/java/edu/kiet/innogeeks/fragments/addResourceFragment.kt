package edu.kiet.innogeeks.fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.kiet.innogeeks.R
import edu.kiet.innogeeks.databinding.FragmentAddResourceBinding
import edu.kiet.innogeeks.model.Resource


// AddResourceFragment.kt
class addResourceFragment : Fragment() {
    private var _binding: FragmentAddResourceBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var domain: String? = null

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

        // First, fetch the coordinator's domain
        fetchCoordinatorDomain()

        binding.submitButton.setOnClickListener {
            addResource()
        }
    }

    private fun fetchCoordinatorDomain() {
        val coordinatorId = auth.currentUser?.uid ?: return

        db.collection("Domains")
            .get()
            .addOnSuccessListener { domains ->
                for (domainDoc in domains) {
                    db.collection("Domains")
                        .document(domainDoc.id)
                        .collection("Coordinators")
                        .document(coordinatorId)
                        .get()
                        .addOnSuccessListener { doc ->
                            if (doc.exists()) {
                                domain = domainDoc.id
                            }
                        }
                }
            }
    }

    private fun addResource() {
        if (domain == null) {
            Toast.makeText(context, "Domain not found", Toast.LENGTH_SHORT).show()
            return
        }

        val title = binding.titleInput.text.toString()
        val description = binding.descriptionInput.text.toString()

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        val resource = Resource(
            title = title,
            description = description,
            timestamp = System.currentTimeMillis(),
            coordinatorId = auth.currentUser?.uid ?: return
        )

        db.collection("Domains")
            .document(domain!!)
            .collection("resources")
            .add(resource)
            .addOnSuccessListener {
                Toast.makeText(context, "Resource added successfully", Toast.LENGTH_SHORT).show()
                binding.titleInput.text?.clear()
                binding.descriptionInput.text?.clear()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to add resource", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}