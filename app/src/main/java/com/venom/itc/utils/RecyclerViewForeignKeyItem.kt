package com.venom.itc.utils

import android.view.View
import com.venom.itc.R
import com.venom.itc.databinding.RecyclerViewForeignKeysRowBinding
import com.xwray.groupie.viewbinding.BindableItem
import org.json.JSONObject

class RecyclerViewForeignKeyItem(val foreign_keys_obj: JSONObject) : BindableItem<RecyclerViewForeignKeysRowBinding>() {
    override fun getLayout(): Int {
        return R.layout.recycler_view_foreign_keys_row
    }
    override fun initializeViewBinding(view: View): RecyclerViewForeignKeysRowBinding {
        return RecyclerViewForeignKeysRowBinding.bind(view)
    }
    override fun bind(viewBinding: RecyclerViewForeignKeysRowBinding, position: Int) {
        viewBinding.foreignKeyOwnerUid.text = foreign_keys_obj["uid"].toString()
        viewBinding.foreignKeyTag.text = foreign_keys_obj["key_tag"].toString()
    }
}