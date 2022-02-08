package com.venom.itc.utils

import android.app.AlertDialog
import android.content.Context
import android.nfc.FormatException
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.venom.itc.ui.foreignKeys.ForeignKeysFragment
import org.json.JSONObject
import java.io.File
import java.math.BigInteger
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.min


object CryptoUtils {
    fun getRandomNonce(numBytes: Int): ByteArray {//to get IVs
        val nonce = ByteArray(numBytes)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            SecureRandom.getInstanceStrong().nextBytes(nonce)
        }
        return nonce
    }

    // AES secret key
    fun getAESKey(keysize: Int): SecretKey {
        val keyGen = KeyGenerator.getInstance("AES")
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            keyGen.init(keysize, SecureRandom.getInstanceStrong())
        }
        return keyGen.generateKey()
    }

    // Password derived AES 256 bits secret key
    fun getAESKeyFromPassword(password: CharArray?, salt: ByteArray?): SecretKey {
        val factory: SecretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        // iterationCount = 65536
        // keyLength = 256
        val spec: KeySpec = PBEKeySpec(password, salt, 65536, 256)
        return SecretKeySpec(factory.generateSecret(spec).encoded, "AES")
    }

    // hex representation
    fun hex(bytes: ByteArray): String {
        val result = StringBuilder()
        for (b in bytes) {
            result.append(String.format("%02x", b))
        }
        return result.toString()
    }

    fun hexToASCII(hexValue: String): String? {
        val output = java.lang.StringBuilder("")
        var i = 0
        while (i < hexValue.length) {
            val str = hexValue.substring(i, i + 2)
            output.append(str.toInt(16).toChar())
            i += 2
        }
        return output.toString()
    }

    // print hex with block size split
    fun hexWithBlockSize(bytes: ByteArray, blockSize: Int): String {
        var blockSize = blockSize
        val hex = hex(bytes)

        // one hex = 2 chars
        blockSize = blockSize * 2

        // better idea how to print this?
        val result: MutableList<String> = ArrayList()
        var index = 0
        while (index < hex.length) {
            result.add(hex.substring(index, min(index + blockSize, hex.length)))
            index += blockSize
        }
        return result.toString()
    }

    fun isBase64(base64String: String): Boolean {
        //if ((base64String.replace(" ", "").length % 4) != 0)
        //    return false
        try {
            Base64.decode(base64String, Base64.DEFAULT)
            return true
        } catch (exception: FormatException) {}
        return false
    }

    //this is a deanless encryption function
    fun encrypt_msg(PT: String, keys_obj:JSONObject?, enc:Boolean): String{
        //TODO(" maybe add message authentication ")
        //one block will be prepended to the message in each phase and discarded at decryption because we dont provide the iv for the other side
        //each message will have an new IV for each phase of encryption
        if (keys_obj == null || !enc) {
            Log.d("encrypt_msg", "key_obj is $keys_obj and enc is $enc, hence no encryption will be done")
            return PT
        }

        val key1 = SecretKeySpec(Base64.decode(keys_obj["key1"].toString(), Base64.DEFAULT), "AES")
        val key2 = SecretKeySpec(Base64.decode(keys_obj["key2"].toString(), Base64.DEFAULT), "AES")
        val key_tag = keys_obj["key_tag"].toString()

        var PT_bytes_1: ByteArray = PT.toByteArray(Charsets.UTF_8)
        /* PHASE 1: AES-256 CBC */
        val random_block_1: ByteArray = getRandomNonce(16)
        PT_bytes_1 = random_block_1 + PT_bytes_1
        val iv_1: ByteArray = getRandomNonce(16)
        val ivSpec_1 = IvParameterSpec(iv_1)
        val cipher_1 = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher_1.init(Cipher.ENCRYPT_MODE, key1, ivSpec_1)
        val CT_1: ByteArray = cipher_1.doFinal(PT_bytes_1)
        Log.d("encrypt_msg", "encrypted msg ${PT_bytes_1.toString(Charsets.UTF_8)} in phase1 length=${CT_1.size} message is: [base64]-> ${Base64.encodeToString(CT_1, Base64.DEFAULT)}")
        /* END OF PHASE 1 */

        var PT_bytes_2: ByteArray = CT_1.reversed().toByteArray()

        /* PHASE 2: AES-256 CBC */
        val random_block_2: ByteArray = getRandomNonce(16)
        PT_bytes_2 = random_block_2 + PT_bytes_2
        val iv_2: ByteArray = getRandomNonce(16)
        val ivSpec_2 = IvParameterSpec(iv_2)
        val cipher_2 = Cipher.getInstance("AES/CBC/PKCS7Padding")
        cipher_2.init(Cipher.ENCRYPT_MODE, key2, ivSpec_2)
        val CT_2: ByteArray = cipher_2.doFinal(PT_bytes_2)
        Log.d("encrypt_msg", "encrypted message phase2 is: [base64]-> ${Base64.encodeToString(CT_2, Base64.DEFAULT)}")
        /* END OF PHASE 2 */
        return "$key_tag~%EN<CRYP73D>%"+Base64.encodeToString(CT_2, Base64.DEFAULT)
        /*https://mkyong.com/java/java-aes-encryption-and-decryption/
            val random_block_2 = getRandomNonce(16)
            PT_bytes_2 = random_block_2 + PT_bytes_2
            val iv_2 = getRandomNonce(16)
            val cipher_2 = Cipher.getInstance("AES/GCM/NoPadding")
            cipher_2.init(Cipher.ENCRYPT_MODE, key2, GCMParameterSpec(128, iv_2, 0, 16))
            val CT_2 = cipher_2.doFinal(PT_bytes_2)
        */
    }

    //govno jopa barebuh cyka
    fun decrypt_msg(CT: String, keys_list: Set<JSONObject>, dec:Boolean): String{
        if (dec){
            Log.d("decrypt_msg", "CT -> $CT")
            if ("~%EN<CRYP73D>%" !in CT) return CT
            val supplied_key_tag = CT.take(32)

            var keys_obj:JSONObject? = null
            keys_list.forEach { if (supplied_key_tag == it["key_tag"]) keys_obj = it }

            if (keys_obj == null){
                Log.d("decrypt_msg", "No Decryption keys found with key_tag $supplied_key_tag")
                return "❌NO KEY TO DECRYPT!"
            }
            val key1 = SecretKeySpec(Base64.decode(keys_obj!!.getString("key1"), Base64.DEFAULT), "AES")
            val key2 = SecretKeySpec(Base64.decode(keys_obj!!.getString("key2"), Base64.DEFAULT), "AES")
            Log.d("decrypt_msg", "the following keys will be used to decrypt this message: key1: ${key1.toString().sha256()}, key2: ${key2.toString().sha256()}")
            try{
                /* REVERSE PHASE 2 */
                val CT_2: ByteArray = Base64.decode(CT.replace("~%EN<CRYP73D>%", "").drop(32), Base64.DEFAULT)
                val iv_2: ByteArray = getRandomNonce(16)//fake (invalid) iv
                val ivSpec_2 = IvParameterSpec(iv_2)
                val cipher_2 = Cipher.getInstance("AES/CBC/PKCS7Padding")
                cipher_2.init(Cipher.DECRYPT_MODE, key2, ivSpec_2)
                val PT_bytes_2: ByteArray = cipher_2.doFinal(CT_2).drop(16).toByteArray()
                /* END OF REVERSE PHASE 2 */

                val CT_1: ByteArray = PT_bytes_2.reversed().toByteArray()
                Log.d("decrypt_msg", "ciphertext $CT decoded from base64 to $CT_1")

                /* REVERSE PHASE 1 */
                val iv_1: ByteArray = getRandomNonce(16)//fake (invalid) iv
                Log.d("decrypt_msg", "fake iv created $iv_1")
                val ivSpec_1 = IvParameterSpec(iv_1)
                val cipher_1 = Cipher.getInstance("AES/CBC/PKCS7Padding")
                cipher_1.init(Cipher.DECRYPT_MODE, key1, ivSpec_1)
                val PT_bytes_1: ByteArray = cipher_1.doFinal(CT_1).drop(16).toByteArray()
                Log.d("decrypt_msg", "decryption done: $PT_bytes_1 - ${PT_bytes_1.size}")
                /* END OF REVERSE PHASE 1 */
                return "\uD83D\uDD13"+PT_bytes_1.toString(Charsets.UTF_8)
            } catch (e: java.lang.Exception) {
                Log.d("decrypt_msg", e.toString())
                return CT.replace("~%EN<CRYP73D>%", "")}
        } else {
            if ("~%EN<CRYP73D>%" in CT) return "\uD83D\uDD12EN<CRYP73D>_M3\$S4G3"
            else return CT
        }
    }

}

fun String.md5(): String {
    val md = MessageDigest.getInstance("MD5")
    return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
}

fun String.sha256(): String {
    val md = MessageDigest.getInstance("SHA-256")
    return BigInteger(1, md.digest(toByteArray())).toString(16).padStart(32, '0')
}

//FIXME: this funciton sometimes crashes because of "index out of range exception length=63 index =63"
infix fun String.xor(that: String) = mapIndexed { index, c ->
    that[index].toInt().xor(c.toInt())
}.joinToString(separator = "") {
    it.toChar().toString()
}

// extension method to convert pixels to dp
fun Int.toDp(context: Context):Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,this.toFloat(),context.resources.displayMetrics).toInt()

fun generate_keys(key_name:String): JSONObject {
    Log.d("CRYPTO", "Generate_keys()")
    val key1 = CryptoUtils.getAESKey(256)
    val key2 = CryptoUtils.getAESKey(256)
    val key_tag = (key1.toString().sha256() xor key2.toString().sha256() xor (FirebaseAuth.getInstance().uid!!).sha256()).md5()
    val keys_json = JSONObject()
    keys_json.put("key1", Base64.encodeToString(key1.encoded, Base64.NO_WRAP))
    keys_json.put("key2", Base64.encodeToString(key2.encoded, Base64.NO_WRAP))
    keys_json.put("key_tag", key_tag)
    keys_json.put("key_name", key_name)
    Log.d("CRYPTO", "keys Generated ${keys_json}. key lengths ${key1.encoded.size}, ${key2.encoded.size}")
    return keys_json
}
