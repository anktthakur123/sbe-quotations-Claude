package com.sidhbaba.quotemaker.util

data class ParsedItem(val description: String, val itemCode: String)

/**
 * Smart parser for data copy-pasted from Excel (or typed) where each row holds an
 * ITEM CODE and a DESCRIPTION, in either column order:
 *   "FAGU10865<TAB>SPL TAPPER PLUG GAUGE OF..."   (code first)
 *   "SPL TAPPER PLUG GAUGE OF...<TAB>FAGU10865"   (description first)
 * Rows are separated by newlines; columns are usually separated by a TAB (Excel paste)
 * but comma / multiple-spaces are also accepted as a fallback.
 */
object ItemPasteParser {

    // Typical item-code shape seen in this company's data: letters followed by digits,
    // optionally with a short alphanumeric suffix, e.g. FAGU10865, FAFI02813, FAGU346.
    private val CODE_REGEX = Regex("(?i)^[A-Z]{2,6}[-]?[0-9]{3,8}[A-Z0-9-]*$")

    fun parse(raw: String): List<ParsedItem> {
        val lines = raw.split("\n", "\r\n")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        // Skip an obvious header row such as "Item Code   Description" / "Description  Item Code"
        val dataLines = lines.filterIndexed { index, line ->
            val lower = line.lowercase()
            if (index == 0 && (lower.contains("item code") || lower.contains("description")) &&
                lower.length < 40
            ) false else true
        }

        val result = mutableListOf<ParsedItem>()
        val looseTokenLines = mutableListOf<Pair<Int, String>>() // lines with no clear delimiter

        dataLines.forEachIndexed { idx, line ->
            val cols = splitColumns(line)
            if (cols.size >= 2) {
                val a = cols[0].trim()
                val b = cols.drop(1).joinToString(" ").trim()
                result.add(assign(a, b))
            } else {
                looseTokenLines.add(idx to line)
            }
        }

        // Fallback: if every line was a single token (two separate pasted blocks: all
        // codes then all descriptions, or vice-versa), pair first half with second half.
        if (result.isEmpty() && looseTokenLines.size >= 2) {
            val tokens = looseTokenLines.map { it.second }
            val codes = tokens.filter { CODE_REGEX.matches(it) }
            val descs = tokens.filterNot { CODE_REGEX.matches(it) }
            if (codes.isNotEmpty() && descs.isNotEmpty()) {
                val n = minOf(codes.size, descs.size)
                for (i in 0 until n) result.add(ParsedItem(descs[i], codes[i]))
            } else if (tokens.size % 2 == 0) {
                // can't tell which half is which -> assume first half = codes, second = descriptions
                val half = tokens.size / 2
                for (i in 0 until half) result.add(assign(tokens[i], tokens[half + i]))
            }
        }
        return result
    }

    private fun splitColumns(line: String): List<String> {
        if (line.contains('\t')) return line.split('\t').map { it.trim() }.filter { it.isNotEmpty() }
        if (Regex("\\s{2,}").containsMatchIn(line)) {
            return line.split(Regex("\\s{2,}")).map { it.trim() }.filter { it.isNotEmpty() }
        }
        if (line.contains(',')) return line.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        return listOf(line)
    }

    private fun assign(a: String, b: String): ParsedItem {
        val aIsCode = CODE_REGEX.matches(a)
        val bIsCode = CODE_REGEX.matches(b)
        return when {
            aIsCode && !bIsCode -> ParsedItem(description = b, itemCode = a)
            bIsCode && !aIsCode -> ParsedItem(description = a, itemCode = b)
            // both or neither matched the code pattern: fall back to "shorter token = code"
            a.length <= b.length -> ParsedItem(description = b, itemCode = a)
            else -> ParsedItem(description = a, itemCode = b)
        }
    }
}
