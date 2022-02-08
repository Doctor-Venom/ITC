package com.venom.itc.utils

import android.view.View
import com.venom.itc.R
import com.venom.itc.databinding.RecyclerViewOtherSideChatRowBinding
import com.xwray.groupie.viewbinding.BindableItem

class RecyclerViewOtherSideMsgItem(val uid: String, val txt: String) : BindableItem<RecyclerViewOtherSideChatRowBinding>() {
    override fun getLayout(): Int {
        return R.layout.recycler_view_other_side_chat_row
    }
    override fun initializeViewBinding(view: View): RecyclerViewOtherSideChatRowBinding {
        return RecyclerViewOtherSideChatRowBinding.bind(view)
    }
    override fun bind(viewBinding: RecyclerViewOtherSideChatRowBinding, position: Int) {
        viewBinding.otherMsgText.text = txt
    }
}