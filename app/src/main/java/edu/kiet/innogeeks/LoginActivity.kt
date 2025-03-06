package edu.kiet.innogeeks

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.animation.addListener
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import edu.kiet.innogeeks.databinding.ActivityLoginBinding


class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        //Image Animation
        startImageAnimation()

        // Check if user is already logged in
        if (auth.currentUser != null) {
            navigateToMainActivity()
            finish()
            return
        }

        // Configure Google Sign In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        // Email/Password Login
        binding.loginButton.setOnClickListener {
            val email = binding.loginEmail.text.toString().trim()
            val password = binding.loginPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.loginButton.isEnabled = false

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    binding.loginButton.isEnabled = true
                    if (task.isSuccessful) {
                        UserDataManager.initialize { success ->
                            if (success) {
                                navigateToMainActivity()
                            } else {
                                Toast.makeText(this, "Failed to fetch user data", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(
                            this,
                            "Login failed: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }

        // Google Sign In
        binding.buttonText.setOnClickListener {
            startGoogleSignIn()
        }

        binding.signupRedirectText.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun startGoogleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
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
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
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
                        // Check if user exists in Firestore
                        db.collection("users").document(user.uid)
                            .get()
                            .addOnSuccessListener { document ->
                                if (!document.exists()) {
                                    // If user doesn't exist, create new document
                                    val userMap = hashMapOf(
                                        "email" to user.email,
                                        "uid" to user.uid,
                                        "name" to (user.displayName ?: ""),
                                        "photoUrl" to (user.photoUrl?.toString() ?: ""),
                                        "signInMethod" to "google",
                                        "role" to "user"  // Default role
                                    )
                                    db.collection("users").document(user.uid)
                                        .set(userMap)
                                        .addOnSuccessListener {
                                            UserDataManager.initialize { success ->
                                                if (success) {
                                                    navigateToMainActivity()
                                                } else {
                                                    Toast.makeText(this, "Failed to fetch user data", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        }
                                } else {
                                    UserDataManager.initialize { success ->
                                        if (success) {
                                            navigateToMainActivity()
                                        } else {
                                            Toast.makeText(this, "Failed to fetch user data", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Error checking user: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                } else {
                    Toast.makeText(this, "Authentication Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    private fun startImageAnimation() {
        val fadeDuration = 1000L // 2 seconds per transition

        fun fadeInOut(imageView: ImageView, delay: Long, nextAction: () -> Unit) {
            val fadeIn = ObjectAnimator.ofFloat(imageView, "alpha", 0f, 1f).setDuration(fadeDuration)
            val fadeOut = ObjectAnimator.ofFloat(imageView, "alpha", 1f, 0f).setDuration(fadeDuration)

            val animatorSet = AnimatorSet().apply {
                startDelay = delay
                playSequentially(fadeIn, fadeOut)
                addListener(onEnd = { nextAction() })
            }
            animatorSet.start()
        }

        fun startLoop() {
            fadeInOut(binding.image0, 100L) {
                fadeInOut(binding.image1, 100L) {
                    fadeInOut(binding.image2, 100L) {
                        startLoop() // Loop the animation sequence
                    }
                }
            }
        }

        startLoop()
    }
}
