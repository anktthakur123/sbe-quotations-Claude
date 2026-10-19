package com.sidhbaba.quotemaker.util

import android.content.Context
import android.os.Build
import android.os.Environment
import java.io.File

/**
 * Where generated quotation PDFs are saved. When the user has granted "All files access"
 * (Settings > Enable Quotation Folder Access), PDFs go straight into a "Quotations Form"
 * folder at the root of the phone's storage, as asked. Without that permission (or on a
 * device that refuses it), we fall back to this app's own external folder so saving never
 * fails — the Settings screen explains the difference.
 */
object PdfPaths {

    fun hasAllFilesAccess(): Boolean =
        Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager()

    fun quotationsFolder(context: Context): File {
        return if (hasAllFilesAccess()) {
            File(Environment.getExternalStorageDirectory(), "Quotations Form").apply { mkdirs() }
        } else {
            File(context.getExternalFilesDir(null), "Quotations Form").apply { mkdirs() }
        }
    }

    /** File naming convention: the quotation number, e.g. "SB329.pdf". */
    fun resolveOutputFile(context: Context, quoteNo: String): File {
        val safeName = quoteNo.replace(Regex("[\\\\/:*?\"<>|]"), "-").ifBlank { "Quotation" }
        return File(quotationsFolder(context), "$safeName.pdf")
    }
}
