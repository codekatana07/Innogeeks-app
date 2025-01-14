package edu.kiet.innogeeks

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Patterns
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var loginEmail: EditText
    private lateinit var loginPassword: EditText
    private lateinit var loginButton: Button
    private lateinit var signupRedirectText: TextView
    private lateinit var imageAnimation: ImageView
    private val images = arrayOf(
        R.drawable.img,
        R.drawable.img_1,
        R.drawable.img_2,
        R.drawable.img_3,
    )

    private var currentImageIndex = 0
    private val delay = 1500L
    private val handler = Handler(Looper.getMainLooper())

    private val imageSwitcher = object : Runnable {
        override fun run() {
            val fadeOut = ObjectAnimator.ofFloat(imageAnimation, "alpha", 1f, 0.4f)
            fadeOut.duration = 500
            fadeOut.start()

            fadeOut.addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    imageAnimation.setImageResource(images[currentImageIndex])
                    imageAnimation.alpha = 1f
                }
                override fun onAnimationCancel(animation: android.animation.Animator) {}
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })

            currentImageIndex = (currentImageIndex + 1) % images.size
            handler.postDelayed(this, delay)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize Firebase instances
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // Initialize views
        imageAnimation = findViewById(R.id.animation)
        loginEmail = findViewById(R.id.login_email)
        loginPassword = findViewById(R.id.login_password)
        loginButton = findViewById(R.id.login_button)
        signupRedirectText = findViewById(R.id.signupRedirectText)

        handler.post(imageSwitcher)

        loginButton.setOnClickListener {
            val email = loginEmail.text.toString()
            val pass = loginPassword.text.toString()

            if (validateInputs(email, pass)) {
                performLogin(email, pass)
            }
        }

        signupRedirectText.setOnClickListener {
            startActivity(Intent(this@LoginActivity, RegisterActivity::class.java))
        }
    }

    private fun validateInputs(email: String, pass: String): Boolean {
        when {
            email.isEmpty() -> {
                loginEmail.error = "Email cannot be empty"
                return false
            }
            !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                loginEmail.error = "Please enter valid email"
                return false
            }
            pass.isEmpty() -> {
                loginPassword.error = "Password cannot be empty"
                return false
            }
            else -> return true
        }
    }

    private fun performLogin(email: String, pass: String) {
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener { authResult ->
                val userId = authResult.user?.uid ?: return@addOnSuccessListener
                checkUserRole(userId)
            }
            .addOnFailureListener {
                Toast.makeText(this@LoginActivity, "Login Failed", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkUserRole(userId: String) {
        // Check in admins collection
        db.collection("admins").document(userId).get()
            .addOnSuccessListener { adminDoc ->
                if (adminDoc.exists()) {
                    navigateToActivity(AdminsActivity::class.java)
                    return@addOnSuccessListener
                }

                // Check in coordinators collection
                db.collection("coordinators").document(userId).get()
                    .addOnSuccessListener { coordinatorDoc ->
                        if (coordinatorDoc.exists()) {
                            navigateToActivity(TeachersActivity::class.java)
                            return@addOnSuccessListener
                        }

                        // Check in students collection
                        db.collection("students").document(userId).get()
                            .addOnSuccessListener { studentDoc ->
                                if (studentDoc.exists()) {
                                    navigateToActivity(ParentActivity::class.java)
                                } else {
                                    // User not found in any role collection
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "User role not assigned",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    auth.signOut()
                                }
                            }
                    }
            }
            .addOnFailureListener {
                Toast.makeText(
                    this@LoginActivity,
                    "Error checking user role",
                    Toast.LENGTH_SHORT
                ).show()
                auth.signOut()
            }
    }

    private fun navigateToActivity(activityClass: Class<*>) {
        Toast.makeText(this@LoginActivity, "Login Successful", Toast.LENGTH_SHORT).show()
        startActivity(Intent(this@LoginActivity, activityClass))
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(imageSwitcher)
    }
}