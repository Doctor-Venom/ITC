package com.venom.itc.utils

import android.view.View
import com.venom.itc.R
import com.venom.itc.databinding.RecyclerViewMySideChatRowBinding
import com.xwray.groupie.viewbinding.BindableItem

class RecyclerVIewMySideMsgItem(val uid: String, val txt: String) : BindableItem<RecyclerViewMySideChatRowBinding>() {
    override fun getLayout(): Int {
        return R.layout.recycler_view_my_side_chat_row
    }
    override fun initializeViewBinding(view: View): RecyclerViewMySideChatRowBinding {
        return RecyclerViewMySideChatRowBinding.bind(view)
    }
    override fun bind(viewBinding: RecyclerViewMySideChatRowBinding, position: Int) {
            viewBinding.myMsgText.text = txt
        }
}