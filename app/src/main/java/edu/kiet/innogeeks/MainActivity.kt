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
import edu.kiet.innogeeks.databinding.NavHeaderBinding
import edu.kiet.innogeeks.fragments.admin_home
import edu.kiet.innogeeks.fragments.coordinatorHomeFragment
import edu.kiet.innogeeks.fragments.personalDetailsFragment
import edu.kiet.innogeeks.fragments.settingsFragment
import edu.kiet.innogeeks.fragments.studentHomeFragment
import edu.kiet.innogeeks.fragments.userFragment

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var navHeaderBinding: NavHeaderBinding
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

        // Check if user is logged in
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Set up drawer toggle
        val drawerToggle = findViewById<ImageButton>(R.id.openDrawerLayout)
        drawerToggle.setOnClickListener {
            binding.drawerLayout.open()
        }

        // Initialize UserDataManager and set up UI only after data is loaded
        initializeUserData()

        // Set up navigation drawer listener
        setupNavigationDrawer()
    }

    private fun initializeUserData() {
        // Show loading state if needed
        showLoading(true)

        UserDataManager.initialize { success ->
            // Hide loading state
            showLoading(false)

            if (success) {
                // Log user data
//                logUserData()

                // Now that we have data, set up the UI
                runOnUiThread {
                    setupNavHeader()
                    // Load appropriate fragment based on role
                    checkUserRoleAndLoadFragment()
                }
            } else {
                Log.e(TAG, "Failed to initialize UserDataManager")
                runOnUiThread {
                    Toast.makeText(this, "Failed to fetch user data", Toast.LENGTH_SHORT).show()
                    // Handle failure - maybe redirect to login or retry
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }

    private fun showLoading(show: Boolean) {
        runOnUiThread {
            // Implement your loading UI here
            // For example, show/hide a ProgressBar
            // binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    private fun checkUserRoleAndLoadFragment() {
        UserDataManager.getCurrentUser()?.let { userData ->
            when (userData.role) {
                "admins" -> changeFragment(admin_home())
                "Coordinators" -> changeFragment(coordinatorHomeFragment())
                "Students" -> changeFragment(studentHomeFragment())
                "users","user" -> changeFragment(userFragment())
                else -> {
                    Log.e(TAG, "Unknown user role: ${userData.role}")
                    Toast.makeText(this, "Unknown user role", Toast.LENGTH_SHORT).show()
                }
            }
        } ?: run {
            Log.e(TAG, "No user data available")
            Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
            // Handle the error case - maybe redirect to login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupNavigationDrawer() {
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
                    UserDataManager.stopListening()
                    auth.signOut()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                    true
                }
                else -> false
            }
        }
    }

    private fun changeFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frame, fragment)
            .commit()
    }

//    private fun logUserData() {
//        UserDataManager.getCurrentUser()?.let { userData ->
//            Log.d(TAG, "User Data:")
//            Log.d(TAG, "UID: ${userData.uid}")
//            Log.d(TAG, "Name: ${userData.name}")
//            Log.d(TAG, "Email: ${userData.email}")
//            Log.d(TAG, "Role: ${userData.role}")
//            Log.d(TAG, "Domain: ${userData.domain}")
//            Log.d(TAG, "Library ID: ${userData.libraryId}")
//        } ?: Log.e(TAG, "No user data available")
//    }

    override fun onDestroy() {
        super.onDestroy()
        UserDataManager.stopListening()
    }
    private fun setupNavHeader() {
        // Get reference to header view
        val headerView = binding.mainNavView.getHeaderView(0)
        navHeaderBinding = NavHeaderBinding.bind(headerView)

        // Update with user data
        UserDataManager.getCurrentUser()?.let { userData ->
            navHeaderBinding.textViewName.text = userData.name
            navHeaderBinding.textViewRole.text = userData.role.capitalize()
        }
    }
}