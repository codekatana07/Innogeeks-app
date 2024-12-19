package edu.kiet.innogeeks

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.EditText
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

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
                            Toast.makeText(
                                this@LoginActivity,
                                "Login Successful",
                                Toast.LENGTH_SHORT
                            ).show()
                            launchMainActivity()
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

    private fun launchMainActivity() {
        val intent = Intent(
            this@LoginActivity,
            TeachersActivity::class.java
        )
        startActivity(intent)
        finish() // Optional: finish LoginActivity to prevent going back to it
    }
}