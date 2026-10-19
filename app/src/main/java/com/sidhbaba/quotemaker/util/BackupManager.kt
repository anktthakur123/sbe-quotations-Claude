package com.sidhbaba.quotemaker.util

import android.content.Context
import android.net.Uri
import com.sidhbaba.quotemaker.data.AppDatabase
import com.sidhbaba.quotemaker.data.Customer
import com.sidhbaba.quotemaker.data.Quotation
import com.sidhbaba.quotemaker.data.QuotationItem
import org.json.JSONArray
import org.json.JSONObject

/**
 * Exports/imports the app's data as small, human-readable JSON (not PDFs) so the whole
 * history + customer master can be backed up or moved to another phone in one small file.
 */
object BackupManager {

    suspend fun exportFullBackup(context: Context): String {
        val db = AppDatabase.get(context)
        val customers = db.customerDao().getAll()
        val quotations = db.quotationDao().getAll()

        val root = JSONObject()
        root.put("type", "quotemaker_full_backup")
        root.put("exportedAt", System.currentTimeMillis())

        val customersArr = JSONArray()
        customers.forEach { customersArr.put(customerToJson(it)) }
        root.put("customers", customersArr)

        val quotationsArr = JSONArray()
        for (q in quotations) {
            val items = db.quotationDao().getItems(q.id)
            val qJson = quotationToJson(q)
            val itemsArr = JSONArray()
            items.forEach { itemsArr.put(itemToJson(it)) }
            qJson.put("items", itemsArr)
            quotationsArr.put(qJson)
        }
        root.put("quotations", quotationsArr)
        return root.toString(2)
    }

    suspend fun exportCustomersOnly(context: Context): String {
        val db = AppDatabase.get(context)
        val customers = db.customerDao().getAll()
        val root = JSONObject()
        root.put("type", "quotemaker_customers_backup")
        root.put("exportedAt", System.currentTimeMillis())
        val arr = JSONArray()
        customers.forEach { arr.put(customerToJson(it)) }
        root.put("customers", arr)
        return root.toString(2)
    }

    suspend fun writeToUri(context: Context, uri: Uri, content: String) {
        context.contentResolver.openOutputStream(uri)?.use { out ->
            out.write(content.toByteArray(Charsets.UTF_8))
        }
    }

    suspend fun readFromUri(context: Context, uri: Uri): String {
        context.contentResolver.openInputStream(uri)?.use { input ->
            return input.bufferedReader().readText()
        }
        throw IllegalStateException("Could not open the selected file")
    }

    /** Returns (customersImported, quotationsImported). Existing rows with the same id are overwritten. */
    suspend fun importBackup(context: Context, jsonText: String): Pair<Int, Int> {
        val db = AppDatabase.get(context)
        val root = JSONObject(jsonText)
        var customersImported = 0
        var quotationsImported = 0

        root.optJSONArray("customers")?.let { arr ->
            for (i in 0 until arr.length()) {
                val c = customerFromJson(arr.getJSONObject(i))
                db.customerDao().upsert(c)
                customersImported++
            }
        }

        root.optJSONArray("quotations")?.let { arr ->
            for (i in 0 until arr.length()) {
                val qObj = arr.getJSONObject(i)
                val q = quotationFromJson(qObj)
                val newId = db.quotationDao().upsert(q)
                db.quotationDao().deleteItemsForQuotation(newId)
                val itemsArr = qObj.optJSONArray("items")
                if (itemsArr != null) {
                    val items = (0 until itemsArr.length()).map {
                        itemFromJson(itemsArr.getJSONObject(it)).copy(quotationId = newId, id = 0)
                    }
                    db.quotationDao().insertItems(items)
                }
                quotationsImported++
            }
        }
        return customersImported to quotationsImported
    }

    private fun customerToJson(c: Customer) = JSONObject().apply {
        put("id", c.id); put("name", c.name); put("companyName", c.companyName)
        put("address", c.address); put("gstin", c.gstin)
    }

    private fun customerFromJson(o: JSONObject) = Customer(
        id = o.optLong("id", 0),
        name = o.optString("name", ""),
        companyName = o.optString("companyName", ""),
        address = o.optString("address", ""),
        gstin = o.optString("gstin", "")
    )

    private fun quotationToJson(q: Quotation) = JSONObject().apply {
        put("id", q.id); put("quoteNo", q.quoteNo); put("customerId", q.customerId)
        put("customerNameSnapshot", q.customerNameSnapshot)
        put("customerCompanySnapshot", q.customerCompanySnapshot)
        put("customerAddressSnapshot", q.customerAddressSnapshot)
        put("customerGstinSnapshot", q.customerGstinSnapshot)
        put("subject", q.subject); put("dateMillis", q.dateMillis); put("validUntilMillis", q.validUntilMillis)
    }

    private fun quotationFromJson(o: JSONObject) = Quotation(
        id = o.optLong("id", 0),
        quoteNo = o.optString("quoteNo", ""),
        customerId = o.optLong("customerId", 0),
        customerNameSnapshot = o.optString("customerNameSnapshot", ""),
        customerCompanySnapshot = o.optString("customerCompanySnapshot", ""),
        customerAddressSnapshot = o.optString("customerAddressSnapshot", ""),
        customerGstinSnapshot = o.optString("customerGstinSnapshot", ""),
        subject = o.optString("subject", ""),
        dateMillis = o.optLong("dateMillis", System.currentTimeMillis()),
        validUntilMillis = o.optLong("validUntilMillis", System.currentTimeMillis())
    )

    private fun itemToJson(i: QuotationItem) = JSONObject().apply {
        put("id", i.id); put("quotationId", i.quotationId); put("srNo", i.srNo)
        put("description", i.description); put("itemCode", i.itemCode)
        put("quantityText", i.quantityText); put("priceRupees", i.priceRupees)
    }

    private fun itemFromJson(o: JSONObject) = QuotationItem(
        id = o.optLong("id", 0),
        quotationId = o.optLong("quotationId", 0),
        srNo = o.optInt("srNo", 0),
        description = o.optString("description", ""),
        itemCode = o.optString("itemCode", ""),
        quantityText = o.optString("quantityText", "01nos"),
        priceRupees = o.optDouble("priceRupees", 0.0)
    )
}
