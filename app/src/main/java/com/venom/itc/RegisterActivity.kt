package com.venom.itc

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.database.FirebaseDatabase
import com.venom.itc.databinding.ActivityLoginBinding
import com.venom.itc.databinding.ActivityRegisterBinding
import com.venom.itc.utils.User

class RegisterActivity : AppCompatActivity() {
    companion object {
        const val TAG = "RegisterActivity"
    }

    private lateinit var binding: ActivityRegisterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        supportActionBar?.hide()

        binding.registerButton.setOnClickListener {
            val pseudonym = binding.newUserPseudonymEdittext.text.toString()
            val email = binding.newUserEmailEdittext.text.toString()
            val password = binding.newUserPasswdEdittext.text.toString()
            val confirm_password = binding.newUserPasswdConfirmEdittext.text.toString()

            //ensure that email and password are provided
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "please provide both Email and Password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            //ensure that passwords match
            if (password != confirm_password) {
                Toast.makeText(this, "passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            Log.d(TAG, "registration with pseudonym: $pseudonym - Email: $email - Password: $password")

            //register the user with email and password in firebase
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener {
                    if (!it.isSuccessful) return@addOnCompleteListener //if registration is unsuccessful
                    val uid = it.result?.user?.uid!!
                    Log.d(TAG, "successfully registered a user with uid $uid")
                    Toast.makeText(this, "Registration Successful.", Toast.LENGTH_SHORT).show()

                    //save user data (uid, pseudonym) into firebase realtime database
                    val ref = FirebaseDatabase.getInstance().getReference("/users/$uid")
                    val user = User(uid, pseudonym)
                    ref.setValue(user)
                        .addOnSuccessListener {
                            Log.d(TAG, "User saved to Firebase Database!!")
                            val firebase_user: FirebaseUser? = FirebaseAuth.getInstance().currentUser
                            val profileUpdates: UserProfileChangeRequest = UserProfileChangeRequest.Builder().setDisplayName(user.pseudonym).build()
                            firebase_user?.updateProfile(profileUpdates)
                        }
                        .addOnFailureListener {
                            Log.d(TAG, "user have not been saved to the fkin database!!")
                        }

                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                }
                .addOnFailureListener {
                    Log.d(TAG, "failed to register the user: ${it.message}")
                    Toast.makeText(this, "Registration Failed: ${it.message}", Toast.LENGTH_LONG).show()
                }
        }
    }
}