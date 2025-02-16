package edu.kiet.innogeeks

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.kiet.innogeeks.databinding.ActivityMainBinding
import edu.kiet.innogeeks.fragments.admin_home
import edu.kiet.innogeeks.fragments.coordinatorHomeFragment
import edu.kiet.innogeeks.fragments.personalDetailsFragment
import edu.kiet.innogeeks.fragments.settingsFragment
import edu.kiet.innogeeks.fragments.studentHomeFragment


class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Set up drawer toggle
        val drawerToggle = findViewById<ImageButton>(R.id.openDrawerLayout)
        drawerToggle.setOnClickListener {
            binding.drawerLayout.open()
        }

        // Get header view for navigation drawer
        val headerView = binding.mainNavView.getHeaderView(0)

        // Check user role and load appropriate fragment
        checkUserRoleAndLoadFragment()

        binding.mainNavView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navHome -> {
                    checkUserRoleAndLoadFragment()
                    binding.drawerLayout.close()
                    true
                }
                R.id.navSetting -> {
                    changeFragment(settingsFragment())
                    binding.drawerLayout.close()
                    true
                }
                R.id.navPersonalDetail -> {
                    changeFragment(personalDetailsFragment())
                    binding.drawerLayout.close()
                    true
                }
                R.id.navLogOut -> {
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }    }

    private fun checkUserRoleAndLoadFragment() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val uid = currentUser.uid

        // First check if user is admin
        db.collection("admins").document(uid).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    changeFragment(admin_home())
                    return@addOnSuccessListener
                }

                // If not admin, check in all domains
                val domains = listOf("Android", "Web_2", "Web_3", "IoT", "ML", "AR-VR")
                var userFound = false

                for (domain in domains) {
                    // Check in coordinators collection
                    db.collection("Domains").document(domain)
                        .collection("Coordinators").document(uid).get()
                        .addOnSuccessListener { coordSnapshot ->
                            if (coordSnapshot.exists() && !userFound) {
                                userFound = true
                                changeFragment(coordinatorHomeFragment())
                                return@addOnSuccessListener
                            }
                        }

                    // Check in students collection
                    db.collection("Domains").document(domain)
                        .collection("Students").document(uid).get()
                        .addOnSuccessListener { studentSnapshot ->
                            if (studentSnapshot.exists() && !userFound) {
                                userFound = true
                                changeFragment(studentHomeFragment())
                                return@addOnSuccessListener
                            }
                        }
                        .addOnFailureListener { exception ->
                            Toast.makeText(
                                this,
                                "Error checking role: ${exception.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }

            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Error checking role: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun changeFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frame, fragment)
            .commit()
    }

}