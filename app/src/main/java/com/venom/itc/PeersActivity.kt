package com.venom.itc

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.ScanOptions
import com.venom.itc.databinding.ActivityPeersBinding
import com.venom.itc.ui.myKeys.MyKeysFragment
import com.venom.itc.utils.QRImageDialog
import com.venom.itc.utils.RecyclerViewPeerItem
import com.xwray.groupie.GroupieAdapter
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import org.json.JSONObject

class PeersActivity : AppCompatActivity() {
    companion object {
        const val TAG = "PeersActivity"
        val peerList = mutableSetOf<String>()
    }

    private lateinit var binding: ActivityPeersBinding
    private lateinit var mQrScannerLauncher : ActivityResultLauncher<Intent>
    private val adapter = GroupieAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_peers)
        binding = ActivityPeersBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        supportActionBar?.hide()

        mQrScannerLauncher  = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if(it.resultCode == Activity.RESULT_OK) {
                val intentResult = IntentIntegrator.parseActivityResult(it.resultCode, it.data)
                if(intentResult.contents != null) {
                    val my_uid = FirebaseAuth.getInstance().uid
                    val other_uid = intentResult.contents.toString()
                    if (my_uid != other_uid){
                        val my_ref = FirebaseDatabase.getInstance().getReference("/users/$my_uid/user-peers/$other_uid")
                        val other_ref = FirebaseDatabase.getInstance().getReference("/users/$other_uid/user-peers/$my_uid")
                        my_ref.setValue(true)
                        .addOnSuccessListener {
                            Log.d(TAG, "successfully added peer for local user")
                            other_ref.setValue(true)
                            .addOnSuccessListener { Log.d(TAG, "successfully added peer for other user") }
                            .addOnFailureListener { Log.d(TAG, "error adding peer for other user") } }
                        .addOnFailureListener { Log.d(TAG, "error adding peer for local user") }
                    } else {
                        Toast.makeText(this, "Can not add yourself as a peer!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.fabAddPeer.setOnClickListener { view ->
            val scanner = IntentIntegrator(this).setDesiredBarcodeFormats(ScanOptions.QR_CODE).setPrompt("Scan QR code to add peer.")
            mQrScannerLauncher.launch(scanner.createScanIntent())
        }
        binding.fabShareme.setOnClickListener { view ->
            val intent = Intent(this, QRImageDialog::class.java)
            intent.putExtra("DATA", FirebaseAuth.getInstance().uid.toString())
            startActivity(intent)
        }

        binding.peersRecyclerView.adapter = adapter
        binding.peersRecyclerView.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))
        adapter.setOnItemClickListener { item, view ->
            Log.d(TAG, "opening chat activity from peers activity")
            val item = item as RecyclerViewPeerItem
            val intent = Intent(view.context, ChatActivity::class.java)
            intent.putExtra("other_uid", item.other_uid)
            startActivity(intent)
            finish() //finish this activity so that the user goes back to MainActivity when he goes back
        }
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(/*ItemTouchHelper.UP or ItemTouchHelper.DOWN*/ 0, ItemTouchHelper.LEFT) {
            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                TODO("Not yet implemented")
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.LEFT) {
                    //TODO: maybe also delete the chat? or let the user decide that and do it manually
                    val position = viewHolder.adapterPosition
                    val removed_item = adapter.getItem(position)
                    val peer_uid_textview = viewHolder.itemView.findViewById<TextView>(R.id.peer_uid)
                    var removed_peer_uid:String? = null
                    adapter.removeGroupAtAdapterPosition(position)
                    peerList.forEach {
                        if (it == peer_uid_textview.text.toString()){
                            removed_peer_uid = it
                            return@forEach
                        }
                    }
                    peerList.remove(removed_peer_uid)
                    val my_ref = FirebaseDatabase.getInstance().getReference("/users/${Firebase.auth.currentUser!!.uid}/user-peers/$removed_peer_uid")
                    val other_ref = FirebaseDatabase.getInstance().getReference("/users/$removed_peer_uid/user-peers/${Firebase.auth.currentUser!!.uid}")
                    my_ref.removeValue()
                    other_ref.removeValue()
                    Log.d(TAG, "item at $position is removed from firebase database!!")

                    val snackbar: Snackbar = Snackbar.make(viewHolder.itemView, "Peer has been removed.", Snackbar.LENGTH_LONG)
                    snackbar.setAction("UNDO") {
                        Log.d(TAG, "item $removed_peer_uid is restored to the position $position")
                        adapter.add(position, removed_item)
                        binding.peersRecyclerView.scrollToPosition(position)
                        peerList.add(removed_peer_uid!!)
                        val my_ref = FirebaseDatabase.getInstance().getReference("/users/${Firebase.auth.currentUser!!.uid}/user-peers/$removed_peer_uid")
                        val other_ref = FirebaseDatabase.getInstance().getReference("/users/$removed_peer_uid/user-peers/${Firebase.auth.currentUser!!.uid}")
                        my_ref.setValue(true)
                            .addOnSuccessListener {
                                Log.d(TAG, "successfully added peer for local user")
                                other_ref.setValue(true)
                                    .addOnSuccessListener { Log.d(TAG, "successfully added peer for other user") }
                                    .addOnFailureListener { Log.d(TAG, "error adding peer for other user") } }
                            .addOnFailureListener { Log.d(TAG, "error adding peer for local user") }
                        Log.d(TAG, peerList.toString())
                    }
                    snackbar.setActionTextColor(Color.YELLOW)
                    snackbar.show()
                }
            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    .addSwipeLeftBackgroundColor(ContextCompat.getColor(this@PeersActivity, R.color.red))
                    .addSwipeLeftActionIcon(R.drawable.ic_delete)
                    .create()
                    .decorate()
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }).attachToRecyclerView(binding.peersRecyclerView)
        fetch_peer_list()
        refresh_recycler_view()
    }

    fun fetch_peer_list(){
        val ref = FirebaseDatabase.getInstance().getReference("/users/${FirebaseAuth.getInstance().uid}/user-peers")
        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val peer_uid = snapshot.key ?: return
                Log.d(TAG, "onChildAdded: peer uid update: $peer_uid")
                //adapter.add(RecyclerViewPeerItem(peer_uid.toString()))
                peerList.add(peer_uid)
                refresh_recycler_view()
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val peer_uid = snapshot.key ?: return
                Log.d(TAG, "onChildChanged: peer uid update: $peer_uid")
                //adapter.add(RecyclerViewPeerItem(peer_uid.toString()))
                peerList.add(peer_uid)
                refresh_recycler_view()
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {
                val peer_uid = snapshot.key ?: return
                Log.d(TAG, "onChildRemoved: peer uid update: $peer_uid")
                //adapter.add(RecyclerViewPeerItem(peer_uid.toString()))
                peerList.remove(peer_uid)
                refresh_recycler_view()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    fun refresh_recycler_view(){
        adapter.clear()
        peerList.forEach {
            adapter.add(RecyclerViewPeerItem(it))
        }
    }
}