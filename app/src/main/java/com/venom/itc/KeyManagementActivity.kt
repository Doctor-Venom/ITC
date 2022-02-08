package com.venom.itc

import android.os.Bundle
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import com.google.firebase.auth.FirebaseAuth
import com.venom.itc.databinding.ActivityKeyManagementBinding
import org.json.JSONObject
import java.io.File

class KeyManagementActivity : AppCompatActivity() {

    companion object{
        const val TAG = "KeyManagementActivity"
        var mFilesDir:File? = null
        val my_uid = FirebaseAuth.getInstance().currentUser!!.uid
    }
    private lateinit var binding: ActivityKeyManagementBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        mFilesDir = filesDir
        super.onCreate(savedInstanceState)
        binding = ActivityKeyManagementBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)


        val navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each menu should be considered as top level destinations.
        val appBarConfiguration = AppBarConfiguration(setOf(R.id.navigation_my_keys, R.id.navigation_foreign_keys))
        setupActionBarWithNavController(navController, appBarConfiguration)
        binding.navView.setupWithNavController(navController)
    }
}