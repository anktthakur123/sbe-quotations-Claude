package com.sidhbaba.quotemaker.util

import java.text.DecimalFormat
import java.util.Locale

/** Formats a number as ₹ X,XXX.XX using the Indian digit grouping (e.g. ₹ 2,600.00, ₹ 1,25,000.00). */
object Rupee {
    fun format(amount: Double): String {
        val symbols = java.text.DecimalFormatSymbols(Locale("en", "IN"))
        val df = DecimalFormat("##,##,##0.00", symbols)
        return "₹ " + df.format(amount)
    }

    fun parse(text: String): Double {
        val cleaned = text.replace("₹", "").replace(",", "").trim()
        return cleaned.toDoubleOrNull() ?: 0.0
    }
}
