package com.sidhbaba.quotemaker.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import java.io.File

/** Shown right after a PDF is generated+saved: View / Save (copy elsewhere) / Share. */
object ExportResultDialog {
    fun show(context: Context, pdfFile: File) {
        val options = arrayOf("View Quotation", "Save (copy to another location)", "Share")
        AlertDialog.Builder(context)
            .setTitle("Saved: ${pdfFile.name}")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> view(context, pdfFile)
                    1 -> saveAs(context, pdfFile)
                    2 -> share(context, pdfFile)
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private fun view(context: Context, file: File) {
        try {
            val uri = uriFor(context, file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "No PDF viewer app found", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveAs(context: Context, file: File) {
        Toast.makeText(context, "Already saved to: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        share(context, file) // let the user pick "Save to Drive" / a file manager from the chooser too
    }

    private fun share(context: Context, file: File) {
        val uri = uriFor(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share quotation PDF"))
    }
}
