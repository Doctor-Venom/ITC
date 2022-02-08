package com.venom.itc

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.venom.itc.databinding.ActivityProfileBinding
import com.venom.itc.utils.toDp


class ProfileActivity : AppCompatActivity() {

    companion object{
        const val TAG = "ProfileActivity"
    }

    private lateinit var binding:ActivityProfileBinding
    private lateinit var user: FirebaseUser
    private lateinit var pseudonym: String
    private lateinit var email: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        user = Firebase.auth.currentUser!!
        binding.profileUid.text = user.uid

        email = user.email.toString()
        binding.profileEmail.text = email

        Firebase.database.reference.child("users").child(user.uid).get().addOnSuccessListener {
            pseudonym = it.child("pseudonym").value.toString()
            binding.pseudonym.text = pseudonym
        }

        val ref = FirebaseDatabase.getInstance().getReference("/users/${user.uid}/user-peers")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                binding.numofPeers.text = snapshot.childrenCount.toString()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        binding.changePasswdBtn.setOnClickListener {
            show_change_password_dialog(this)
        }

        binding.deleteAccoutnButton.setOnClickListener {
            show_delete_user_dialog(this)
        }
    }

    fun show_change_password_dialog(ctx: Context) {
        val layout = LinearLayout(ctx)
        layout.orientation = LinearLayout.VERTICAL
        val layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(16.toDp(ctx), 0.toDp(ctx), 16.toDp(ctx), 0.toDp(ctx))

        val old_password_box = EditText(ctx)
        old_password_box.hint = "Old Password"
        old_password_box.layoutParams = layoutParams
        layout.addView(old_password_box)

        val new_password_box = EditText(ctx)
        new_password_box.hint = "New Password"
        new_password_box.layoutParams = layoutParams
        layout.addView(new_password_box)

        val builder = MaterialAlertDialogBuilder(ctx)
                .setTitle("Change Account Password")
                .setView(layout)
                .setPositiveButton("Change") { dialog, which ->
                    val old_password = old_password_box.text.toString()
                    val new_password = new_password_box.text.toString()
                    Log.d(TAG, "changing password from $old_password to $new_password")
                    val credential = EmailAuthProvider.getCredential(email, old_password)
                    user.reauthenticate(credential).addOnCompleteListener { task_reauthenticate ->
                        if (task_reauthenticate.isSuccessful) {
                            Log.d(TAG, "User re-authenticated.")
                            user.updatePassword(new_password).addOnCompleteListener { task_change_passwd ->
                                if (task_change_passwd.isSuccessful) {
                                    Toast.makeText(ctx, "Password Has Been Changed!", Toast.LENGTH_LONG).show()
                                    Log.d(TAG, "password update Success")
                                } else {
                                    Toast.makeText(ctx, "Error Changing Password: ${task_change_passwd.exception?.message}", Toast.LENGTH_LONG).show()
                                    Log.d(TAG, "password update error ${task_change_passwd.exception?.message}")
                                }
                            }
                        } else {
                            Toast.makeText(ctx, "Failed to reauthenticate - incorrect old password!", Toast.LENGTH_LONG).show()
                            Log.d(TAG, "User re-authentication failed")
                        }
                    }
                }
                .setNeutralButton("Cancel", null)
        val dialog = builder.create()
        dialog.show()
    }

    fun show_delete_user_dialog(ctx: Context) {
        val layout = LinearLayout(ctx)
        layout.orientation = LinearLayout.VERTICAL
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(16.toDp(ctx), 0.toDp(ctx), 16.toDp(ctx), 0.toDp(ctx))

        val password_box = EditText(ctx)
        password_box.hint = "Password"
        password_box.layoutParams = layoutParams
        layout.addView(password_box)
        val builder = MaterialAlertDialogBuilder(ctx)
            .setTitle("Delete Account And Wipe Data")
            .setView(layout)
            .setPositiveButton("DELETE ACCOUNT") { dialog, which ->
                val password = password_box.text.toString()
                val credential = EmailAuthProvider.getCredential(email, password)
                user.reauthenticate(credential).addOnCompleteListener { task_reauthenticate ->
                    if (task_reauthenticate.isSuccessful) {
                        Log.d(TAG, "User re-authenticated.")
                        user.delete().addOnCompleteListener { task_delete_user ->
                            val ref = FirebaseDatabase.getInstance().getReference("/users/${user.uid}")
                            ref.removeValue()
                            Log.d(TAG, "user and all his relevant data has been deleted!")
                            val uid = FirebaseAuth.getInstance().uid
                            if (uid != null){
                                Log.d(TAG, "the user $uid is logged in")
                            } else {
                                Log.d(TAG, "the user is not logged in")
                                val intent = Intent(this, LoginActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                            }
                        }
                    } else {
                        Toast.makeText(ctx, "Authentication Failed - incorrect password!", Toast.LENGTH_LONG).show()
                        Log.d(TAG, "User re-authentication failed")
                    }
                }
            }
            .setNeutralButton("Cancel", null)
        val dialog = builder.create()
        dialog.show()
    }
}

