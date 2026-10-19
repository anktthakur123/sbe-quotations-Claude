package com.sidhbaba.quotemaker

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.sidhbaba.quotemaker.databinding.ActivityPdfPreviewBinding
import java.io.File

class PdfPreviewActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPdfPreviewBinding
    private lateinit var pdfFile: File

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPdfPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }

        val path = intent.getStringExtra("pdfPath") ?: return
        pdfFile = File(path)
        binding.tvPath.text = pdfFile.name

        binding.btnOpen.setOnClickListener { openPdf() }
        binding.btnShare.setOnClickListener { sharePdf() }
    }

    private fun uriFor(file: File): Uri =
        FileProvider.getUriForFile(this, "$packageName.fileprovider", file)

    private fun openPdf() {
        try {
            val uri = uriFor(pdfFile)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "No PDF viewer app found", Toast.LENGTH_LONG).show()
        }
    }

    private fun sharePdf() {
        val uri = uriFor(pdfFile)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, "Share quotation PDF"))
    }
}
