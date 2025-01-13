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
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var loginEmail: EditText
    private lateinit var loginPassword:EditText
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
    private val delay = 1500L // 2 seconds delay
    private val handler = Handler(Looper.getMainLooper())

    private val imageSwitcher = object : Runnable {
        override fun run() {
            // Apply fade-out effect
            val fadeOut = ObjectAnimator.ofFloat(imageAnimation, "alpha", 1f, 0.4f)
            fadeOut.duration = 500 // 0.5 seconds
            fadeOut.start()

            // After fade-out completes, update the image
            fadeOut.addListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {}
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    // Set the next image
                    imageAnimation.setImageResource(images[currentImageIndex])

                    // Reset alpha back to fully visible for the next fade-out
                    imageAnimation.alpha = 1f
                }

                override fun onAnimationCancel(animation: android.animation.Animator) {}
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })

            // Update index to the next image
            currentImageIndex++

            // Reset to the first image when the last image has been displayed
            if (currentImageIndex >= images.size) {
                currentImageIndex = 0 // Loop back to the first image
            }

            // Schedule the next run after the delay
            handler.postDelayed(this, delay)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        imageAnimation =findViewById(R.id.animation)
        handler.post(imageSwitcher)

        auth = FirebaseAuth.getInstance()
        loginEmail = findViewById(R.id.login_email)
        loginPassword = findViewById(R.id.login_password)
        loginButton = findViewById(R.id.login_button)
        signupRedirectText = findViewById(R.id.signupRedirectText)


        loginButton.setOnClickListener {
            val email = loginEmail.text.toString()
            val pass: String = loginPassword.text.toString()
            if (email.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                if (pass.isNotEmpty()) {
                    auth.signInWithEmailAndPassword(email, pass)
                        .addOnSuccessListener {
                            val currentUser = auth.currentUser
                            if (currentUser != null) {
                                if(currentUser.email == "rishiarora2705@gmail.com"){
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "Login Successful",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    val intent = Intent(
                                        this@LoginActivity,
                                        AdminsActivity::class.java
                                    )
                                    startActivity(intent)
                                    finish()
                                }else{
                                    Toast.makeText(
                                        this@LoginActivity,
                                        "Login Successful",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    launchMainActivity()
                                }
                            }
                        }.addOnFailureListener {
                            Toast.makeText(this@LoginActivity, "Login Failed", Toast.LENGTH_SHORT)
                                .show()
                        }
                } else {
                    loginPassword.error = "Password cannot be empty"
                }
            } else if (email.isEmpty()) {
                loginEmail.error = "Email cannot be empty"
            } else {
                loginEmail.error = "Please enter valid email"
            }
        }


        signupRedirectText.setOnClickListener {
            startActivity(
                Intent(
                    this@LoginActivity, RegisterActivity::class.java
                )
            )
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the handler when the activity is destroyed to avoid memory leaks
        handler.removeCallbacks(imageSwitcher)
    }

    private fun launchMainActivity() {
        val intent = Intent(
            this@LoginActivity,
            TeachersActivity::class.java
        )
        startActivity(intent)
        finish() // Optional: finish LoginActivity to prevent going back to it
    }
}