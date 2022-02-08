package com.venom.itc.ui.myKeys

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.venom.itc.KeyManagementActivity
import com.venom.itc.R
import com.venom.itc.databinding.FragmentMyKeysBinding
import com.venom.itc.utils.*
import com.xwray.groupie.GroupieAdapter
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import org.json.JSONObject
import java.io.File



class MyKeysFragment : Fragment() {

    companion object{
        const val TAG = "MyKeysFragment"
    }

    private var _binding: FragmentMyKeysBinding? = null
    private val binding get() = _binding!! // This property is only valid between onCreateView and onDestroyView.
    private val adapter = GroupieAdapter()
    private val my_keys_list = mutableSetOf<JSONObject>()//FIXME: WHY THE FUCK DOES THIS SET ALLOWS DUPLICATE JSONOBJECTS!?!??!?!?
    private lateinit var my_keys_file: File

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMyKeysBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.generateNewKeyBtn.setOnClickListener {
            Log.d(TAG, "generating new key...")
            showKeyGenerationDialog(requireContext())
        }

        binding.deleteAllKeysBtn.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            builder
                .setTitle("Delete All Your Keys")
                .setMessage("Are you sure you want to delete ALL your cryptographic keys?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, id ->
                    my_keys_list.clear()
                    adapter.clear()
                    my_keys_file.delete()
                    Log.d(TAG, "all user keys were deleted")
                }
                .setNegativeButton("No") { dialog, id ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        }

        binding.myKeysRecyclerView.adapter = adapter

        my_keys_file = File(KeyManagementActivity.mFilesDir, "${KeyManagementActivity.my_uid}-my_keys")
        load_my_keys()

        my_keys_list.forEach {
            adapter.add(RecyclerViewMyKeyItem(it))
        }
        binding.myKeysRecyclerView.scrollToPosition(adapter.itemCount - 1)
        binding.myKeysRecyclerView.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(/*ItemTouchHelper.UP or ItemTouchHelper.DOWN*/ 0, ItemTouchHelper.RIGHT or ItemTouchHelper.LEFT) {

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                //this function doesnt work at the moment, you have to uncomment /*ItemTouchHelper.UP or ItemTouchHelper.DOWN*/ above
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                val item = adapter.getItem(fromPos)
                adapter.removeGroupAtAdapterPosition(fromPos)
                adapter.add(toPos, item)
                Log.d(TAG, "item is moved from $fromPos to $toPos!")
                return true // true if moved, false otherwise
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                if (direction == ItemTouchHelper.RIGHT) {
                    var key_to_share = JSONObject()
                    val key_tag_textview = viewHolder.itemView.findViewById<TextView>(R.id.my_key_tag)
                    Log.d(TAG, "item with key_tag ${key_tag_textview.text} was swiped to the right to share via QR code")
                    my_keys_list.forEach {
                        if (it.getString("key_tag") == key_tag_textview.text.toString()) {
                            key_to_share.put("key1", it["key1"])
                            key_to_share.put("key2", it["key2"])
                            key_to_share.put("key_tag", it["key_tag"])
                            return@forEach
                        }
                    }
                    key_to_share.put("uid", KeyManagementActivity.my_uid)
                    Log.d(TAG, "sharing $key_to_share")
                    val intent = Intent(requireContext(), QRImageDialog::class.java)
                    intent.putExtra("DATA", key_to_share.toString())
                    startActivity(intent)
                    //TODO do something to get the swiped item back to its position
                    //the following will get it back but better approach is required
                    val position = viewHolder.adapterPosition
                    val item = adapter.getItem(position)
                    adapter.add(position, item)
                    adapter.removeGroupAtAdapterPosition(position + 1)
                    //
                } else if (direction == ItemTouchHelper.LEFT) {
                    val position = viewHolder.adapterPosition
                    val item = adapter.getItem(position)
                    val key_tag_textview = viewHolder.itemView.findViewById<TextView>(R.id.my_key_tag)
                    var removed_key_json: JSONObject? = null
                    adapter.removeGroupAtAdapterPosition(position)
                    my_keys_list.forEach {
                        if (it.getString("key_tag") == key_tag_textview.text.toString()){
                            removed_key_json = it
                            return@forEach
                        }
                    }
                    my_keys_list.remove(removed_key_json)

                    //WARNING: THIS IS DANGEROUS, IF SOMETHING GOES WRONG AT THIS MOMENT THE KEYS ARE LOST, SO YOU BETTER FIND A BETTER SOLUTION
                    save_all_my_keys()
                    Log.d(TAG, "item at $position is removed!!")

                    val snackbar: Snackbar = Snackbar.make(viewHolder.itemView, "The key has been deleted permanently.", Snackbar.LENGTH_LONG)
                    snackbar.setAction("UNDO") {
                        Log.d(TAG, "item $removed_key_json is restored to the position $position")
                        adapter.add(position, item)
                        binding.myKeysRecyclerView.scrollToPosition(position)
                        my_keys_list.add(removed_key_json!!)
                        Log.d(TAG, my_keys_list.toString())
                        save_all_my_keys()
                    }
                    snackbar.setActionTextColor(Color.YELLOW)
                    snackbar.show()
                }

            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        .addSwipeRightBackgroundColor(ContextCompat.getColor(requireContext(), R.color.holo_green_dark))
                        .addSwipeRightActionIcon(R.drawable.ic_qr_code)
                        .addSwipeLeftBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red))
                        .addSwipeLeftActionIcon(R.drawable.ic_delete)
                        .create()
                        .decorate()
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }).attachToRecyclerView(binding.myKeysRecyclerView)

        return view
    }

    override fun onPause() {
        super.onPause()
        //this is needed because when you go to foreign keys fragment and press back button, all my_keys will be tripled or doubled
        adapter.clear()
        my_keys_list.clear()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun load_my_keys(){
        //if keys file exists, load existing keys, otherwise create keys and store them
        if(my_keys_file.exists()) {
            Log.d(TAG, "loading my keys from local private storage")
            my_keys_file.useLines { lines -> lines.forEach { my_keys_list.add(JSONObject(it)) }}
            Log.d(TAG, "${my_keys_list.size} keys loaded successfully")
        } else { Log.d(TAG, "my_keys file for user ${FirebaseAuth.getInstance().currentUser!!.uid} does not exist in local private storage")}
    }

    fun save_all_my_keys(){
        my_keys_file.delete()
        //requireContext().openFileOutput(my_keys_file.name, AppCompatActivity.MODE_PRIVATE).use{it.write("".toByteArray())}
        my_keys_list.forEach { my_key ->
            requireContext().openFileOutput(my_keys_file.name, AppCompatActivity.MODE_PRIVATE or AppCompatActivity.MODE_APPEND).use { it.write((my_key.toString() + "\n").toByteArray()) }
        }
        Log.d(TAG, "all keys saved to my keys file")
    }

    fun showKeyGenerationDialog(ctx: Context) {
        fun getEditTextLayout(context: Context): ConstraintLayout {
            val constraintLayout = ConstraintLayout(context)
            val layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            constraintLayout.layoutParams = layoutParams
            constraintLayout.id = View.generateViewId()

            val textInputLayout = TextInputLayout(context)
            textInputLayout.boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            layoutParams.setMargins(32.toDp(context), 8.toDp(context), 32.toDp(context), 8.toDp(context))
            textInputLayout.layoutParams = layoutParams
            //textInputLayout.hint = "keys name"
            textInputLayout.id = View.generateViewId()
            textInputLayout.tag = "textInputLayoutTag"

            val textInputEditText = TextInputEditText(context)
            textInputEditText.id = View.generateViewId()
            textInputEditText.tag = "textInputEditTextTag"

            textInputLayout.addView(textInputEditText)

            val constraintSet = ConstraintSet()
            constraintSet.clone(constraintLayout)

            constraintLayout.addView(textInputLayout)
            return constraintLayout
        }

        val builder = MaterialAlertDialogBuilder(ctx)
        builder.setTitle("Enter a name for the newly generated keys")// dialog title

        // dialog message view
        val constraintLayout = getEditTextLayout(ctx)
        builder.setView(constraintLayout)
        val textInputLayout = constraintLayout.findViewWithTag<TextInputLayout>("textInputLayoutTag")
        val textInputEditText = constraintLayout.findViewWithTag<TextInputEditText>("textInputEditTextTag")

        // alert dialog positive button
        builder.setPositiveButton("Generate"){ dialog, which->
            val name = textInputEditText.text.toString()
            my_keys_list.forEach {
                if (name == it["key_name"]) {
                    Toast.makeText(ctx, "A key with the same name already exist.", Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
            }
            val keys_json = generate_keys(name)
            activity?.openFileOutput(my_keys_file.name, AppCompatActivity.MODE_APPEND or AppCompatActivity.MODE_PRIVATE).use { it?.write((keys_json.toString() + "\n").toByteArray()) }
            my_keys_list.add(keys_json)
            adapter.add(RecyclerViewMyKeyItem(keys_json))
            binding.myKeysRecyclerView.scrollToPosition(adapter.itemCount - 1)
        }

        // alert dialog other buttons
        builder.setNeutralButton("Cancel", null)

        builder.setCancelable(false) // set dialog non cancelable
        val dialog = builder.create() // create the alert dialog
        dialog.show() // show alert dialog
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false // initially disable the positive button

        // edittext text change listener
        textInputEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {}
            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}
            override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
                if (p0.isNullOrBlank()){
                    textInputLayout.error = "Input Cant Be Empty."
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                } else if (false) { //TODO: check if key with same name already exist (i already do that in other place)
                    textInputLayout.error = "A Key With Same Name Already Exist."
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                } else if (!p0.all{it in "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.*$#@!?()[]<>"}) {
                    textInputLayout.error = "Name Contains Invalid Characters."
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
                } else {
                    textInputLayout.error = ""
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                }
            }
        })
    }
}