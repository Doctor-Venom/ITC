package com.venom.itc.utils


import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.venom.itc.databinding.QrImageDialogBinding


class QRImageDialog : Activity() {

    private lateinit var binding: QrImageDialogBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = QrImageDialogBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        val data = intent.getStringExtra("DATA")
        val QR_bitmap = generateQRCode(data!!)
        binding.QRImage.setImageBitmap(QR_bitmap)
        binding.QRImage.setOnClickListener { finish() }
    }

    private fun generateQRCode(text: String): Bitmap {
        val width = 750
        val height = 750
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val codeWriter = MultiFormatWriter()
        try {
            val bitMatrix = codeWriter.encode(text, BarcodeFormat.QR_CODE, width, height)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
                }
            }
        } catch (e: WriterException) {
            Log.d("QR image dialog", "generateQRCode: ${e.message}")
        }
        return bitmap
    }
}