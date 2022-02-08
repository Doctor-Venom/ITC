package com.venom.itc.utils

import android.util.Log
import android.view.View
import com.google.firebase.database.FirebaseDatabase
import com.venom.itc.MainActivity
import com.venom.itc.R
import com.venom.itc.databinding.RecyclerViewChatRowBinding
import com.xwray.groupie.viewbinding.BindableItem

class RecyclerViewChatItem (val other_uid:String) : BindableItem<RecyclerViewChatRowBinding>() {
    override fun getLayout(): Int {
        return R.layout.recycler_view_chat_row
    }
    override fun initializeViewBinding(view: View): RecyclerViewChatRowBinding {
        return RecyclerViewChatRowBinding.bind(view)
    }
    override fun bind(viewBinding: RecyclerViewChatRowBinding, position: Int) {
        val ref = FirebaseDatabase.getInstance().getReference("/users/")
        ref.child(other_uid).get().addOnSuccessListener {
            viewBinding.userPseudonym.text = it.child("pseudonym").value.toString()
        }.addOnFailureListener{
            Log.e(MainActivity.TAG, "Error getting data from fbrtdb", it)
            viewBinding.userPseudonym.text = "||||||||||"
        }
    }
}