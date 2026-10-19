package com.sidhbaba.quotemaker.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.content.FileProvider
import com.sidhbaba.quotemaker.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val tag: String, val notes: String, val apkUrl: String)

/**
 * Checks GitHub Releases (repo is fixed in Prefs, never user-editable) for a newer version than
 * the one currently installed, and can download + launch the installer for the new APK.
 * Call only from a coroutine (it does blocking network I/O on Dispatchers.IO).
 */
object GithubUpdateChecker {

    suspend fun checkLatest(): Result<UpdateInfo?> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/${Prefs.GITHUB_OWNER}/${Prefs.GITHUB_REPO}/releases/latest"
            val conn = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                setRequestProperty("Accept", "application/vnd.github+json")
                connectTimeout = 15000
                readTimeout = 15000
            }
            val code = conn.responseCode
            if (code == 404) {
                // No releases published yet on the repo.
                return@withContext Result.success(null)
            }
            if (code !in 200..299) {
                return@withContext Result.failure(Exception("GitHub returned HTTP $code"))
            }
            val body = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(body)
            val tag = json.optString("tag_name", "")
            val notes = json.optString("body", "")
            val assets = json.optJSONArray("assets")
            var apkUrl = ""
            if (assets != null) {
                for (i in 0 until assets.length()) {
                    val a = assets.getJSONObject(i)
                    val name = a.optString("name", "")
                    if (name.endsWith(".apk")) {
                        apkUrl = a.optString("browser_download_url", "")
                        break
                    }
                }
            }
            if (tag.isBlank() || apkUrl.isBlank()) return@withContext Result.success(null)

            val currentVersion = BuildConfig.VERSION_NAME
            if (isNewer(tag, currentVersion)) {
                Result.success(UpdateInfo(tag, notes, apkUrl))
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isNewer(remoteTag: String, currentVersion: String): Boolean {
        val remote = remoteTag.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        val current = currentVersion.removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        for (i in 0 until maxOf(remote.size, current.size)) {
            val r = remote.getOrElse(i) { 0 }
            val c = current.getOrElse(i) { 0 }
            if (r != c) return r > c
        }
        return false
    }

    suspend fun downloadApk(context: Context, apkUrl: String): Result<File> = withContext(Dispatchers.IO) {
        try {
            val conn = (URL(apkUrl).openConnection() as HttpURLConnection).apply {
                instanceFollowRedirects = true
                connectTimeout = 15000
                readTimeout = 60000
            }
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            val outFile = File(dir, "quote-maker-update.apk")
            conn.inputStream.use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
            Result.success(outFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun promptInstall(context: Context, apkFile: File) {
        val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
