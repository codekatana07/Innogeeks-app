package edu.kiet.innogeeks.fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.kiet.innogeeks.R
import edu.kiet.innogeeks.databinding.FragmentReasonOfAbsenceBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class reasonOfAbsenceFragment : Fragment() {

    private var _binding: FragmentReasonOfAbsenceBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private lateinit var userId: String
    private lateinit var currentDate: String
    private lateinit var domain: String


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentReasonOfAbsenceBinding.inflate(inflater,container,false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        db = FirebaseFirestore.getInstance()
        userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        val sdf = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        currentDate = sdf.format(Date())
        binding.editTextDate.text = currentDate

        getStudentDetails()

        binding.btnSubmit.setOnClickListener {
            submitReason()
        }
    }

    private fun submitReason() {
        val reason = binding.editTextTextMultiLine.text.toString()
        if (reason.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a reason", Toast.LENGTH_SHORT).show()
            return
        }

        // Get reference to the student document
        val studentRef = db.collection("Domains")
            .document(domain)
            .collection("S" +
                    "tudents")
            .document(userId)
        studentRef.get()
            .addOnSuccessListener { document ->
                val existingReasons = document.get("reasonOfAbsence") as? Map<String, String> ?: mapOf()

                // Create new map with existing reasons plus new one
                val updatedReasons = existingReasons.toMutableMap()
                updatedReasons[currentDate] = reason

                // Update Firestore with the merged map
                studentRef.update("reasonOfAbsence", updatedReasons)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Reason submitted successfully", Toast.LENGTH_SHORT).show()
                        binding.editTextTextMultiLine.text.clear()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Failed to submit reason", Toast.LENGTH_SHORT).show()
                        Log.w("ParentNotesFragment", "Error submitting reason", e)
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to fetch existing reasons", Toast.LENGTH_SHORT).show()
                Log.w("ParentNotesFragment", "Error fetching reasons", e)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up the binding object to avoid memory leaks
        _binding = null
    }
    private fun getStudentDetails() {
        db.collection("Domains")
            .get()
            .addOnSuccessListener { domains ->
                for (domainDoc in domains) {
                    domain = domainDoc.id
                    db.collection("Domains")
                        .document(domain)
                        .collection("Students")
                        .document(userId)
                        .get()
                        .addOnSuccessListener { document ->
                            Log.d("DOCUMENT INFO", document.getString("name").toString())
                            if (document.exists()) {
                                Log.d("DOCUMENT INFO inside if", document.getString("name").toString())
                                binding.labelStdName.text = "Name: ${document.getString("email")}"
                                binding.labelGrade.text = "Domain: $domain"

                            }
                        }

                        .addOnFailureListener { e ->
                            Log.w("ParentNotesFragment", "Error getting student details", e)
                        }

                }
            }
            .addOnFailureListener { e ->
                Log.w("ParentNotesFragment", "Error getting domains", e)
            }
    }

}