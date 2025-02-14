package edu.kiet.innogeeks

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import edu.kiet.innogeeks.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
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

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(imageSwitcher)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        imageAnimation = findViewById(R.id.animation)
        handler.post(imageSwitcher)

        binding.signUpButton.setOnClickListener {
            if(validateInput()){
                checkIfUserExists()
            }
        }

        binding.loginRedirectText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }

    private fun validateInput(): Boolean {
        val email = binding.signupEmail.text.toString().trim()
        val password = binding.signupPassword.text.toString().trim()
        val emailRegex = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$".toRegex()
        val passwordRegex = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d]{6,}$".toRegex()

        return when {
            !email.matches(emailRegex) -> {
                binding.signupEmail.error = "Invalid Email"
                false
            }
            !password.matches(passwordRegex) -> {
                binding.signupPassword.error = "Password must be 6+ chars with letters & numbers"
                false
            }
            else -> true
        }
    }

    private fun checkIfUserExists() {
        val email = binding.signupEmail.text.toString().trim()

        auth.fetchSignInMethodsForEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val signInMethods = task.result?.signInMethods
                if (signInMethods.isNullOrEmpty()) {
                    createUser()
                } else {
                    Toast.makeText(this, "Account already exists! Redirecting to login...", Toast.LENGTH_LONG).show()
                    // Add delay to show toast before redirecting
                    Handler(Looper.getMainLooper()).postDelayed({
                        val intent = Intent(this, LoginActivity::class.java)
                        startActivity(intent)
                        finish()
                    }, 1000) // 1 second delay
                }
            } else {
                Toast.makeText(this, "Error checking user existence: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun createUser() {
        val email = binding.signupEmail.text.toString().trim()
        val password = binding.signupPassword.text.toString().trim()

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Get the user UID
                val uid = auth.currentUser?.uid

                if (uid != null) {
                    // Create a user document in Firestore
                    val userMap = hashMapOf(
                        "email" to email,
                        "uid" to uid,
                        "createdAt" to System.currentTimeMillis()
                    )

                    // Add to users collection
                    db.collection("users").document(uid)
                        .set(userMap)
                        .addOnSuccessListener {
                            Toast.makeText(this, "User registered successfully!", Toast.LENGTH_LONG).show()
                            navigateToMainActivity()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Error adding to Firestore: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            } else {
                Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}