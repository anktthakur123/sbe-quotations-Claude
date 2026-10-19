package com.sidhbaba.quotemaker.util

import android.content.Context

/** Small SharedPreferences wrapper for app-wide settings (quotation numbering, update check timestamp). */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("quotemaker_prefs", Context.MODE_PRIVATE)

    var quotePrefix: String
        get() = sp.getString("quote_prefix", "SB") ?: "SB"
        set(v) = sp.edit().putString("quote_prefix", v).apply()

    var nextSerial: Int
        get() = sp.getInt("next_serial", 1)
        set(v) = sp.edit().putInt("next_serial", v).apply()

    var lastUpdateCheckMillis: Long
        get() = sp.getLong("last_update_check", 0L)
        set(v) = sp.edit().putLong("last_update_check", v).apply()

    var skippedVersionTag: String
        get() = sp.getString("skipped_version_tag", "") ?: ""
        set(v) = sp.edit().putString("skipped_version_tag", v).apply()

    /** Reserves and returns the next quotation number (e.g. "SB329"), then advances the counter. */
    fun takeNextQuoteNumber(): String {
        val n = nextSerial
        val number = "$quotePrefix$n"
        nextSerial = n + 1
        return number
    }

    companion object {
        // Fixed on purpose: the user does not want this editable from inside the app.
        const val GITHUB_OWNER = "anktthakur123"
        const val GITHUB_REPO = "sbe-quotations-Claude"
    }
}
