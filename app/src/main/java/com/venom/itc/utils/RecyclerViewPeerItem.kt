package com.venom.itc.utils

import android.util.Log
import android.view.View
import com.google.firebase.database.*
import com.venom.itc.MainActivity
import com.venom.itc.R
import com.venom.itc.databinding.RecyclerViewPeerRowBinding
import com.xwray.groupie.viewbinding.BindableItem


//represents a row in the latest messages activity recycler view
class RecyclerViewPeerItem(val other_uid: String): BindableItem<RecyclerViewPeerRowBinding>() {
    override fun getLayout(): Int {
        return R.layout.recycler_view_peer_row
    }
    override fun initializeViewBinding(view: View): RecyclerViewPeerRowBinding {
        return RecyclerViewPeerRowBinding.bind(view)
    }
    override fun bind(viewBinding: RecyclerViewPeerRowBinding, position: Int) {
        val ref = FirebaseDatabase.getInstance().getReference("/users/")
        ref.child(other_uid).get().addOnSuccessListener {
            viewBinding.peerUid.text = other_uid
            viewBinding.peerPseudonym.text = it.child("pseudonym").value.toString()
        }.addOnFailureListener{
            Log.e(MainActivity.TAG, "Error getting data from fbrtdb", it)
            viewBinding.peerUid.text = other_uid
            viewBinding.peerPseudonym.text = "||||||||||"
        }
    }
}