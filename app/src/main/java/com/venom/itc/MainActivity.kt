package com.venom.itc

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.venom.itc.databinding.ActivityMainBinding
import com.venom.itc.utils.ChatMessage
import com.venom.itc.utils.RecyclerViewChatItem
import com.venom.itc.utils.RecyclerViewPeerItem
import com.venom.itc.utils.User
import com.xwray.groupie.GroupieAdapter

class MainActivity : AppCompatActivity() {

    companion object{
        const val TAG = "MainActivity"
    }

    private lateinit var fbrtdb: DatabaseReference
    private lateinit var binding: ActivityMainBinding
    private val adapter = GroupieAdapter()
    private val chat_uid_list = mutableSetOf<String>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        auth_check()
        fbrtdb = Firebase.database.reference
        fbrtdb.child("users").child(FirebaseAuth.getInstance().currentUser!!.uid).get().addOnSuccessListener {
            title = it.child("pseudonym").value.toString()
        }.addOnFailureListener{
            Log.e(TAG, "Error getting data from fbrtdb", it)
        }

        binding.mainRecyclerView.adapter = adapter
        binding.mainRecyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        adapter.setOnItemClickListener { item, view ->
            Log.d(PeersActivity.TAG, "opening chat activity from peers activity")
            val item = item as RecyclerViewChatItem
            val intent = Intent(view.context, ChatActivity::class.java)
            intent.putExtra("other_uid", item.other_uid)
            startActivity(intent)
        }
        fetch_chats()

        binding.fabPeers.setOnClickListener { view ->
            val intent = Intent(this, PeersActivity::class.java)
            startActivity(intent)
        }

        binding.fabKeys.setOnClickListener { view ->
            val intent = Intent(this, KeyManagementActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        fetch_chats()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_view_profile -> {
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_logout -> {
                FirebaseAuth.getInstance().signOut()
                Log.d(LoginActivity.TAG,"successfully logged out")
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    //fech all chats for currently logged in user
    fun fetch_chats(){
        fun refresh_recycler_view(){
            //TODO: optimize this so that we dont have to reload the whole recycler view when one row is updated
            adapter.clear()
            chat_uid_list.forEach { it ->
                adapter.add(RecyclerViewChatItem(it))
            }
        }

        val ref = FirebaseDatabase.getInstance().getReference("/users/${FirebaseAuth.getInstance().uid}/messages/")
        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d(TAG, "onChildAdded: ${snapshot.key}")
                val other_uid = snapshot.key ?: return
                chat_uid_list.add(other_uid)
                refresh_recycler_view()
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                Log.d(TAG, "onChildChanged: ${snapshot.key}")
                val other_uid = snapshot.key ?: return
                chat_uid_list.add(other_uid)
                refresh_recycler_view()
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {
                Log.d(TAG, "onChildRemoved: ${snapshot.key}")
                val other_uid = snapshot.key ?: return
                chat_uid_list.remove(other_uid)
                refresh_recycler_view()
                //Toast.makeText(this@MainActivity, "Chat with user with uid $other_uid was deleted!", Toast.LENGTH_LONG).show()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    //checks if the user is logged in, and starts the messaging activity an clears activity stack
    fun auth_check(){
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
}