package com.venom.itc

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.venom.itc.databinding.ActivityChatBinding
import com.venom.itc.ui.foreignKeys.ForeignKeysFragment
import com.venom.itc.ui.myKeys.MyKeysFragment
import com.venom.itc.utils.ChatMessage
import com.venom.itc.utils.CryptoUtils.decrypt_msg
import com.venom.itc.utils.CryptoUtils.encrypt_msg
import com.venom.itc.utils.RecyclerVIewMySideMsgItem
import com.venom.itc.utils.RecyclerViewOtherSideMsgItem
import com.xwray.groupie.GroupieAdapter
import org.json.JSONObject
import java.io.File

class ChatActivity : AppCompatActivity() {

    companion object{
        const val TAG = "ChatActivity"
    }

    private lateinit var binding: ActivityChatBinding
    private lateinit var v: Vibrator
    private lateinit var my_user_uid: String
    private lateinit var other_user_uid: String
    private val msg_list = mutableListOf<ChatMessage>()
    private val adapter = GroupieAdapter()
    private lateinit var sharedPref: SharedPreferences
    private var active_crypto_keys:JSONObject? = null
    private var crypto_setting = false
    private val my_keys_list = mutableSetOf<JSONObject>()
    private val foreign_keys_list = mutableSetOf<JSONObject>()
    private lateinit var my_keys_file: File
    private lateinit var foreign_keys_file: File



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.sendButton.setOnClickListener {
            val message_text = binding.messageEdittext.text.toString()
            perform_send_message(message_text)
            binding.messageEdittext.text?.clear()
        }

        sharedPref = this.getPreferences(Context.MODE_PRIVATE)
        my_user_uid = FirebaseAuth.getInstance().currentUser!!.uid
        other_user_uid = intent.getStringExtra("other_uid").toString()
        Firebase.database.reference.child("users").child(other_user_uid).get().addOnSuccessListener {
            title = it.child("pseudonym").value.toString()
        }.addOnFailureListener{
            Log.e(TAG, "Error getting other user pseudonym from firebase realtime database", it)
        }

        v = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        binding.chatRecyclerView.adapter = adapter


        my_keys_file = File(filesDir, "${my_user_uid}-my_keys")
        load_my_keys()
        foreign_keys_file = File(filesDir, "${my_user_uid}-foreign_keys")
        load_foreign_keys()

        crypto_setting = sharedPref.getBoolean("$my_user_uid-$other_user_uid-CRYPTO_SETTING", false)
        val selected_key_name = sharedPref.getString("$my_user_uid-$other_user_uid-ACTIVE_KEY_NAME", "")
        if (selected_key_name == "") {
            Log.d(TAG, "no active key name found in shared prefs")
            //if (crypto_setting == true) { show_key_selection_dialog() }
        } else {
            Log.d(TAG, "active key name found in shared prefs, loading corresponding key object...")
            my_keys_list.forEach {
                if (it["key_name"].toString() == selected_key_name){
                    active_crypto_keys = it
                    Log.d(TAG, "active key object loaded: $active_crypto_keys")
                }
            }
        }

        fetch_messages()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_chat, menu)

        val item = menu.findItem(R.id.action_cryptography_switch)
        item.isChecked = crypto_setting
        on_crypto_setting_change(item)

        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_delete_chat -> {
                delete_chat()
                true
            }
            R.id.action_cryptography_switch -> {
                item.isChecked = !item.isChecked
                with (sharedPref.edit()) {
                    putBoolean("$my_user_uid-$other_user_uid-CRYPTO_SETTING", item.isChecked)
                    apply()
                }
                Log.d(TAG, "CRYPTO_SETTING is ${item.isChecked}")
                on_crypto_setting_change(item)
                true
            }
            R.id.action_select_keys -> {
                show_key_selection_dialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun load_my_keys(){
        //if keys file exists, load existing keys, otherwise create keys and store them
        if(my_keys_file.exists()) {
            Log.d(TAG, "loading my keys from local private storage")
            my_keys_file.useLines { lines -> lines.forEach { my_keys_list.add(JSONObject(it)) }}
            Log.d(TAG, "${my_keys_list.size} keys loaded successfully")
        } else { Log.d(MyKeysFragment.TAG, "my_keys file for user ${FirebaseAuth.getInstance().currentUser!!.uid} does not exist in local private storage")}
    }
    fun load_foreign_keys(){
        //if keys file exists, load existing keys, otherwise create keys and store them
        if(foreign_keys_file.exists()) {
            Log.d(TAG, "loading foreign keys from local private storage")
            foreign_keys_file.useLines { lines -> lines.forEach { foreign_keys_list.add(JSONObject(it)) }}
            Log.d(TAG, "${foreign_keys_list.size} foreign keys loaded successfully")
        } else { Log.d(TAG, "foreign_keys file for user ${FirebaseAuth.getInstance().currentUser!!.uid} does not exist in local private storage")}
    }

    //updates the checkbox in actionbar and the crypto_status text and color
    fun on_crypto_setting_change(item: MenuItem){
        if (item.isChecked) {
            if (active_crypto_keys == null) { show_key_selection_dialog() }
            item.title = "CRYPTO-[ON]"
            binding.cryptoStatus.text = "Cryptographic Services Are Enabled. ACTIVE KEY: ${active_crypto_keys?.get("key_name")}"
            binding.cryptoStatus.setBackgroundResource(R.color.Chartreuse)
        } else {
            item.title = "CRYPTO-[OFF]"
            binding.cryptoStatus.text = "Cryptographic Services Are Disabled."
            binding.cryptoStatus.setBackgroundResource(R.color.red)
        }
        crypto_setting = item.isChecked
        refresh_recycler_view()
    }

    fun show_key_selection_dialog(){
        val dialogView = LayoutInflater.from(this).inflate(R.layout.choose_key_dialog, null, false)

        val my_keys_names = mutableListOf<String>()
        my_keys_list.forEach { my_keys_names.add(it["key_name"].toString()) }
        val keys_spinner = dialogView.findViewById<Spinner>(R.id.key_selection_spinner)
        if (keys_spinner != null) {
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, my_keys_names)
            keys_spinner.adapter = adapter
        }

        val Builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Select Keys For This Chat.")
            .setPositiveButton("Confirm") { dialog, id ->
                if (keys_spinner.selectedItem == null) { return@setPositiveButton }
                val selected_key_name = keys_spinner.selectedItem.toString()
                Log.d(TAG, "key with name ($selected_key_name) is selected for this chat ")
                my_keys_list.forEach {
                    if (it["key_name"].toString() == selected_key_name){ active_crypto_keys = it }
                }
                with (sharedPref.edit()) {
                    putString("$my_user_uid-$other_user_uid-ACTIVE_KEY_NAME", selected_key_name)
                    apply()
                }
                binding.cryptoStatus.text = "Cryptographic Services Are Enabled. ACTIVE KEY: ${active_crypto_keys?.get("key_name")}"
                binding.cryptoStatus.setBackgroundResource(R.color.Chartreuse)
            }
        Builder.show()

    }

    //fetches all messages from firebase real time database
    fun fetch_messages(){
        val ref = FirebaseDatabase.getInstance().getReference("/users/${my_user_uid}/messages/${other_user_uid}")
        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val chat_message = snapshot.getValue(ChatMessage::class.java)
                msg_list.add(chat_message!!)
                add_message_to_recyclerview(chat_message)
                if(chat_message.timestamp+5 >= (System.currentTimeMillis() / 1000)){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) v.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
                    else v.vibrate(50)  // deprecated in API 26
                }
            }

            override fun onCancelled(error: DatabaseError) {}
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) { finish() } // FIXME: possible bug
        })
    }

    //receives a ChatMessage object and displays its msg content in recycler view in chat_message
    fun add_message_to_recyclerview(chat_message: ChatMessage){
        if (chat_message.sender_usr_id == my_user_uid)
            adapter.add(RecyclerVIewMySideMsgItem(my_user_uid, decrypt_msg(chat_message.msg, foreign_keys_list+my_keys_list, crypto_setting)))
        else
            adapter.add(RecyclerViewOtherSideMsgItem(other_user_uid, decrypt_msg(chat_message.msg, foreign_keys_list+my_keys_list, crypto_setting)))
        binding.chatRecyclerView.scrollToPosition(adapter.itemCount - 1)
    }

    fun refresh_recycler_view(){
        adapter.clear()
        msg_list.forEach {
            add_message_to_recyclerview(it)
        }
    }

    //adds the message for both users in the database
    fun perform_send_message(message_text:String){
        if (message_text.replace(" ", "").isEmpty()) return
        if (message_text.length > 16384) {
            Toast.makeText(this, "The Message is Too Long. Max is 16384 Characters", Toast.LENGTH_LONG).show()
            return
        }
        val my_ref = FirebaseDatabase.getInstance().getReference("/users/${my_user_uid}/messages/${other_user_uid}").push()
        val other_ref = FirebaseDatabase.getInstance().getReference("/users/${other_user_uid}/messages/${my_user_uid}").push()
        val chat_message = ChatMessage(encrypt_msg(message_text, active_crypto_keys, crypto_setting), my_ref.key!!, my_user_uid, other_user_uid, System.currentTimeMillis()/1000)
        my_ref.setValue(chat_message)
            .addOnSuccessListener { Log.d(TAG, "the message (${chat_message.msg}) has been sent") }
            .addOnFailureListener { Log.d(TAG, "the message (${chat_message.msg}) has failed to be sent") }
        other_ref.setValue(chat_message)
            .addOnSuccessListener { Log.d(TAG, "the reverse message (${chat_message.msg}) has been sent") }
            .addOnFailureListener { Log.d(TAG, "the reverse message (${chat_message.msg}) has failed to be sent") }
    }

    //deletes the whole chat from firebase database for both users
    fun delete_chat(){
        val builder = AlertDialog.Builder(this)
        builder
                .setMessage("Are you sure you want to Delete this chat? This operation is irreversible!")
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, id ->
                    //perform deletion
                    val my_ref = FirebaseDatabase.getInstance().getReference("/users/${my_user_uid}/messages/${other_user_uid}")
                    val other_ref = FirebaseDatabase.getInstance().getReference("/users/${other_user_uid}/messages/${my_user_uid}")
                    my_ref.removeValue()
                    other_ref.removeValue()
                    Log.d(TAG, "All messages of this user are deleted from firebase database")
                    finish()
                }
                .setNegativeButton("No") { dialog, id ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
        val alert = builder.create()
        alert.show()
    }
}