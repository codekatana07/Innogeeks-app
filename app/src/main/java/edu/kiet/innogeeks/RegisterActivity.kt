package edu.kiet.innogeeks

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        auth = FirebaseAuth.getInstance()
        signupEmail = findViewById(R.id.signup_email)
        signupPassword = findViewById(R.id.signup_password)
        signInButton = findViewById(R.id.sign_in_button)
        loginRedirectText = findViewById(R.id.loginRedirectText)

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