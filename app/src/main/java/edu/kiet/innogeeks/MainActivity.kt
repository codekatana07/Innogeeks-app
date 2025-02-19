package edu.kiet.innogeeks

import android.content.Intent
import android.os.Bundle
import android.util.Log
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
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize UserDataManager and fetch user data
        initializeUserData()

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
                    UserDataManager.stopListening() // Stop listening before logout
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun initializeUserData() {
        UserDataManager.initialize { success ->
            if (success) {
                logUserData()
            } else {
                Log.e(TAG, "Failed to initialize UserDataManager")
                Toast.makeText(this, "Failed to fetch user data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun logUserData() {
        UserDataManager.getCurrentUser()?.let { userData ->
            Log.d(TAG, "User Data:")
            Log.d(TAG, "UID: ${userData.uid}")
            Log.d(TAG, "Name: ${userData.name}")
            Log.d(TAG, "Email: ${userData.email}")
            Log.d(TAG, "Role: ${userData.role}")
            Log.d(TAG, "Domain: ${userData.domain}")
            Log.d(TAG, "Library ID: ${userData.libraryId}")
        } ?: Log.e(TAG, "No user data available")
    }

    private fun checkUserRoleAndLoadFragment() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        UserDataManager.getCurrentUser()?.let { userData ->
            when (userData.role) {
                "admin" -> changeFragment(admin_home())
                "coordinator" -> changeFragment(coordinatorHomeFragment())
                "student" -> changeFragment(studentHomeFragment())
                else -> {
                    Log.e(TAG, "Unknown user role: ${userData.role}")
                    Toast.makeText(this, "Unknown user role", Toast.LENGTH_SHORT).show()
                }
            }
        } ?: run {
            Log.e(TAG, "No user data available")
            Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun changeFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frame, fragment)
            .commit()
    }

    override fun onDestroy() {
        super.onDestroy()
        UserDataManager.stopListening()
    }
}