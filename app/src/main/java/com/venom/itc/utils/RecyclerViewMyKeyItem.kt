package com.venom.itc.utils

import android.view.View
import com.venom.itc.R
import com.venom.itc.databinding.RecyclerViewMyKeysRowBinding
import com.xwray.groupie.viewbinding.BindableItem
import org.json.JSONObject

class RecyclerViewMyKeyItem(val keys_obj: JSONObject) : BindableItem<RecyclerViewMyKeysRowBinding>() {
    override fun getLayout(): Int {
        return R.layout.recycler_view_my_keys_row
    }
    override fun initializeViewBinding(view: View): RecyclerViewMyKeysRowBinding {
        return RecyclerViewMyKeysRowBinding.bind(view)
    }
    override fun bind(viewBinding: RecyclerViewMyKeysRowBinding, position: Int) {
        viewBinding.keyName.text = keys_obj["key_name"].toString()
        viewBinding.myKeyTag.text = keys_obj["key_tag"].toString()
        viewBinding.key1Hash.text = keys_obj["key1"].toString().md5()
        viewBinding.key2Hash.text = keys_obj["key2"].toString().md5()
    }
}