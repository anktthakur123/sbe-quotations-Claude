package com.sidhbaba.quotemaker.ui

import android.app.Activity
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.sidhbaba.quotemaker.util.GithubUpdateChecker
import com.sidhbaba.quotemaker.util.Prefs
import com.sidhbaba.quotemaker.util.UpdateInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object UpdateDialog {
    fun show(activity: Activity, scope: CoroutineScope, info: UpdateInfo, prefs: Prefs) {
        if (activity.isFinishing) return
        AlertDialog.Builder(activity)
            .setTitle("Update available: ${info.tag}")
            .setMessage(
                (if (info.notes.isNotBlank()) info.notes.take(400) + "\n\n" else "") +
                    "Download and install the new version now?"
            )
            .setPositiveButton("Update") { _, _ -> startDownload(activity, scope, info) }
            .setNegativeButton("Later", null)
            .setNeutralButton("Skip this version") { _, _ -> prefs.skippedVersionTag = info.tag }
            .show()
    }

    private fun startDownload(activity: Activity, scope: CoroutineScope, info: UpdateInfo) {
        // Installing a downloaded APK needs the "install unknown apps" permission on API 26+.
        if (android.os.Build.VERSION.SDK_INT >= 26 && !activity.packageManager.canRequestPackageInstalls()) {
            Toast.makeText(activity, "Allow 'Install unknown apps' for this app, then try again.", Toast.LENGTH_LONG).show()
            val uri = Uri.parse("package:" + activity.packageName)
            activity.startActivity(android.content.Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, uri))
            return
        }
        Toast.makeText(activity, "Downloading update…", Toast.LENGTH_SHORT).show()
        scope.launch {
            val result = GithubUpdateChecker.downloadApk(activity, info.apkUrl)
            result.onSuccess { file ->
                GithubUpdateChecker.promptInstall(activity, file)
            }.onFailure {
                Toast.makeText(activity, "Download failed: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}
