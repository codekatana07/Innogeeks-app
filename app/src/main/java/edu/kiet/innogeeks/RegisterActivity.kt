package edu.kiet.innogeeks

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import edu.kiet.innogeeks.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private lateinit var imageAnimation: ImageView
    private val RC_SIGN_IN = 9001
    private val TAG = "RegisterActivity"

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
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        imageAnimation = findViewById(R.id.animation)
        handler.post(imageSwitcher)

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Email/Password Registration
        binding.signUpButton.setOnClickListener {
            if(validateInput()) {
                binding.signUpButton.isEnabled = false
                checkIfUserExists()
            }
        }

        // Google Registration
        binding.buttonText.setOnClickListener {
            binding.buttonText.isEnabled = false
            startGoogleSignIn()
        }

        binding.loginRedirectText.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun startGoogleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        // Sign out first to always show account picker
        googleSignInClient.signOut().addOnCompleteListener {
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            try {
                val task = GoogleSignIn.getSignedInAccountFromIntent(data)
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "Google Sign In successful")
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                binding.buttonText.isEnabled = true
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Check if user already exists
                        db.collection("users").document(user.uid)
                            .get()
                            .addOnSuccessListener { document ->
                                if (document.exists()) {
                                    // User exists, just navigate to main
                                    Log.d(TAG, "Existing user signed in with Google")
                                    initializeUserAndNavigate()
                                } else {
                                    // Create new user document
                                    val userMap = hashMapOf(
                                        "email" to user.email,
                                        "uid" to user.uid,
                                        "name" to (user.displayName ?: ""),
                                        "photoUrl" to (user.photoUrl?.toString() ?: ""),
                                        "signInMethod" to "google",
                                        "role" to "user",  // Default role
                                        "library-id" to "", // Empty library ID
                                        "createdAt" to System.currentTimeMillis()
                                    )

                                    db.collection("users").document(user.uid)
                                        .set(userMap)
                                        .addOnSuccessListener {
                                            Log.d(TAG, "New user created with Google")
                                            initializeUserAndNavigate()
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e(TAG, "Error creating user document", e)
                                            binding.buttonText.isEnabled = true
                                            Toast.makeText(this, "Error creating user: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e(TAG, "Error checking user existence", e)
                                binding.buttonText.isEnabled = true
                                Toast.makeText(this, "Error checking user: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    binding.buttonText.isEnabled = true
                    Toast.makeText(this, "Authentication Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
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
                    binding.signUpButton.isEnabled = true
                    Toast.makeText(this, "Account already exists! Redirecting to login...", Toast.LENGTH_LONG).show()
                    Handler(Looper.getMainLooper()).postDelayed({
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    }, 800)
                }
            } else {
                binding.signUpButton.isEnabled = true
                Log.e(TAG, "Error checking user existence", task.exception)
                Toast.makeText(this, "Error checking user existence: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun createUser() {
        val email = binding.signupEmail.text.toString().trim()
        val password = binding.signupPassword.text.toString().trim()

        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = auth.currentUser?.uid
                if (uid != null) {
                    val userMap = hashMapOf(
                        "email" to email,
                        "uid" to uid,
                        "name" to "",
                        "photoUrl" to "",
                        "signInMethod" to "email",
                        "role" to "user",  // Default role
                        "library-id" to "", // Empty library ID
                        "createdAt" to System.currentTimeMillis()
                    )

                    db.collection("users").document(uid)
                        .set(userMap)
                        .addOnSuccessListener {
                            Log.d(TAG, "User created successfully")
                            initializeUserAndNavigate()
                        }
                        .addOnFailureListener { e ->
                            binding.signUpButton.isEnabled = true
                            Log.e(TAG, "Error creating user document", e)
                            Toast.makeText(this, "Error creating user: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                }
            } else {
                binding.signUpButton.isEnabled = true
                Log.e(TAG, "Error creating user with email/password", task.exception)
                Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun initializeUserAndNavigate() {
        UserDataManager.initialize { success ->
            if (success) {
                Toast.makeText(this, "Registration successful!", Toast.LENGTH_SHORT).show()
                navigateToMainActivity()
            } else {
                binding.signUpButton.isEnabled = true
                binding.buttonText.isEnabled = true
                Log.e(TAG, "Failed to initialize user data")
                Toast.makeText(this, "Failed to load user data", Toast.LENGTH_SHORT).show()
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