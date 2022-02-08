package com.venom.itc.ui.foreignKeys

import android.app.Activity
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.zxing.integration.android.IntentIntegrator
import com.journeyapps.barcodescanner.ScanOptions
import com.venom.itc.KeyManagementActivity
import com.venom.itc.PeersActivity
import com.venom.itc.R
import com.venom.itc.databinding.FragmentForeignKeysBinding
import com.venom.itc.databinding.FragmentMyKeysBinding
import com.venom.itc.ui.myKeys.MyKeysFragment
import com.venom.itc.utils.QRImageDialog
import com.venom.itc.utils.RecyclerViewForeignKeyItem
import com.venom.itc.utils.RecyclerViewMyKeyItem
import com.xwray.groupie.GroupieAdapter
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator
import org.json.JSONObject
import java.io.File

class ForeignKeysFragment : Fragment() {

    companion object{
        const val TAG = "ForeignKeysFragment"
    }

    private var _binding: FragmentForeignKeysBinding? = null
    private val binding get() = _binding!! // This property is only valid between onCreateView and onDestroyView.
    private val adapter = GroupieAdapter()
    private val foreign_keys_list = mutableSetOf<JSONObject>()//FIXME: WHY THE FUCK DOES THIS SET ALLOWS DUPLICATE JSONOBJECTS!?!??!?!?
    private lateinit var foreign_keys_file: File
    private lateinit var mQrScannerLauncher : ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentForeignKeysBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.addNewForeignKeyBtn.setOnClickListener {
            val scanner = IntentIntegrator(activity).setDesiredBarcodeFormats(ScanOptions.QR_CODE).setPrompt("Scan QR code to add key.")
            mQrScannerLauncher.launch(scanner.createScanIntent())
        }
        binding.deleteAllForeignKeysBtn.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            builder
                .setTitle("Delete All Foreign Keys")
                .setMessage("Are you sure you want to delete ALL foreign cryptographic keys?")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setCancelable(false)
                .setPositiveButton("Yes") { dialog, id ->
                    foreign_keys_list.clear()
                    adapter.clear()
                    foreign_keys_file.delete()
                    Log.d(MyKeysFragment.TAG, "all foreign keys were deleted")
                }
                .setNegativeButton("No") { dialog, id ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        }

        mQrScannerLauncher  = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            if(it.resultCode == Activity.RESULT_OK) {
                val intentResult = IntentIntegrator.parseActivityResult(it.resultCode, it.data)
                if(intentResult.contents != null) {
                    foreign_keys_list.add(JSONObject(intentResult.contents))
                    save_all_foreign_keys()
                    adapter.add(RecyclerViewForeignKeyItem(JSONObject(intentResult.contents)))
                    Toast.makeText(requireContext(), "Foreign Key Successfully Added!", Toast.LENGTH_SHORT).show()
                    Log.d(TAG, "Foreign key ${intentResult.contents} has been added")
                }
            }
        }

        foreign_keys_file = File(KeyManagementActivity.mFilesDir, "${KeyManagementActivity.my_uid}-foreign_keys")
        load_foreign_keys()

        binding.foreignKeysRecyclerView.adapter = adapter
        foreign_keys_list.forEach { adapter.add(RecyclerViewForeignKeyItem(it)) }
        binding.foreignKeysRecyclerView.scrollToPosition(adapter.itemCount - 1)
        binding.foreignKeysRecyclerView.addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(/*ItemTouchHelper.UP or ItemTouchHelper.DOWN*/ 0, ItemTouchHelper.LEFT) {

            override fun onMove(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder): Boolean {
                //this function doesnt work at the moment, you have to uncomment /*ItemTouchHelper.UP or ItemTouchHelper.DOWN*/ above
                val fromPos = viewHolder.adapterPosition
                val toPos = target.adapterPosition
                val item = adapter.getItem(fromPos)
                adapter.removeGroupAtAdapterPosition(fromPos)
                adapter.add(toPos, item)
                Log.d(MyKeysFragment.TAG, "item is moved from $fromPos to $toPos!")
                return true // true if moved, false otherwise
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                    if (direction == ItemTouchHelper.LEFT) {
                    val position = viewHolder.adapterPosition
                    val item = adapter.getItem(position)
                    val key_tag_textview = viewHolder.itemView.findViewById<TextView>(R.id.foreign_key_tag)
                    var removed_key_json: JSONObject? = null
                    adapter.removeGroupAtAdapterPosition(position)
                    foreign_keys_list.forEach {
                        if (it.getString("key_tag") == key_tag_textview.text.toString()){
                            removed_key_json = it
                            return@forEach
                        }
                    }
                    foreign_keys_list.remove(removed_key_json)

                    //TODO: THIS IS DANGEROUS, IF SOMETHING GOES WRONG AT THIS MOMENT THE KEYS ARE LOST, SO YOU BETTER FIND A BETTER SOLUTION
                    save_all_foreign_keys()
                    Log.d(MyKeysFragment.TAG, "item at $position is removed!!")

                    val snackbar: Snackbar = Snackbar.make(viewHolder.itemView, "The key has been deleted permanently.", Snackbar.LENGTH_LONG)
                    snackbar.setAction("UNDO") {
                        Log.d(MyKeysFragment.TAG, "item $removed_key_json is restored to the position $position")
                        adapter.add(position, item)
                        binding.foreignKeysRecyclerView.scrollToPosition(position)
                        foreign_keys_list.add(removed_key_json!!)
                        Log.d(MyKeysFragment.TAG, foreign_keys_list.toString())
                        save_all_foreign_keys()
                    }
                    snackbar.setActionTextColor(Color.YELLOW)
                    snackbar.show()
                }

            }

            override fun onChildDraw(c: Canvas, recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, dX: Float, dY: Float, actionState: Int, isCurrentlyActive: Boolean) {
                RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    .addSwipeLeftBackgroundColor(ContextCompat.getColor(requireContext(), R.color.red))
                    .addSwipeLeftActionIcon(R.drawable.ic_delete)
                    .create()
                    .decorate()
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        }).attachToRecyclerView(binding.foreignKeysRecyclerView)

        return view
    }

    fun load_foreign_keys(){
        //if keys file exists, load existing keys, otherwise create keys and store them
        if(foreign_keys_file.exists()) {
            Log.d(TAG, "loading foreign keys from local private storage")
            foreign_keys_file.useLines { lines -> lines.forEach { foreign_keys_list.add(JSONObject(it)) }}
            Log.d(TAG, "${foreign_keys_list.size} foreign keys loaded successfully")
        } else { Log.d(TAG, "foreign_keys file for user ${FirebaseAuth.getInstance().currentUser!!.uid} does not exist in local private storage")}
    }

    fun save_all_foreign_keys(){
        foreign_keys_file.delete()
        //requireContext().openFileOutput(my_keys_file.name, AppCompatActivity.MODE_PRIVATE).use{it.write("".toByteArray())}
        foreign_keys_list.forEach { foreign_key ->
            Log.d(TAG, foreign_key.toString())
            requireContext().openFileOutput(foreign_keys_file.name, AppCompatActivity.MODE_PRIVATE or AppCompatActivity.MODE_APPEND).use { it.write((foreign_key.toString() + "\n").toByteArray()) }
        }
        Log.d(TAG, "all foreign keys saved to my keys file")
    }
}