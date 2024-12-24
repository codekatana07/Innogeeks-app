package edu.kiet.innogeeks

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class RegisterActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var signupEmail: EditText
    private lateinit var signupPassword: EditText
    private lateinit var signInButton: Button
    private lateinit var loginRedirectText: TextView
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
    override fun onDestroy() {
        super.onDestroy()
        // Stop the handler when the activity is destroyed to avoid memory leaks
        handler.removeCallbacks(imageSwitcher)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        signupEmail = findViewById(R.id.signup_email)
        signupPassword = findViewById(R.id.signup_password)
        signInButton = findViewById(R.id.sign_in_button)
        loginRedirectText = findViewById(R.id.loginRedirectText)
        imageAnimation = findViewById(R.id.animation)
        handler.post(imageSwitcher)

        signInButton.setOnClickListener {
            val user = signupEmail.text.toString().trim { it <= ' ' }
            val pass = signupPassword.text.toString().trim { it <= ' ' }

            if (user.isEmpty()) {
                signupEmail.error = "Email cannot be empty. Please Enter a valid email."
            }
            if (pass.isEmpty()) {
                signupPassword.error = "Password cannot be empty"
            } else {
                auth.createUserWithEmailAndPassword(user, pass).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(
                            this@RegisterActivity,
                            "Registration Successful",
                            Toast.LENGTH_SHORT
                        ).show()
                        startActivity(
                            Intent(
                                this,
                                LoginActivity::class.java
                            )
                        )
                    } else {
                        Toast.makeText(
                            this,
                            "Registration Failed" + task.exception!!.message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
        loginRedirectText.setOnClickListener {
            startActivity(
                Intent(
                    this,
                    LoginActivity::class.java
                )
            )
        }
    }
}