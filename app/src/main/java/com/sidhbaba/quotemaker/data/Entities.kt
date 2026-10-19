package com.sidhbaba.quotemaker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String = "",          // optional, no error if blank
    val companyName: String = "",
    val address: String = "",
    val gstin: String = ""
)

@Entity(tableName = "quotations")
data class Quotation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val quoteNo: String = "",
    val customerId: Long = 0,
    val customerNameSnapshot: String = "",
    val customerCompanySnapshot: String = "",
    val customerAddressSnapshot: String = "",
    val customerGstinSnapshot: String = "",
    val subject: String = "",
    val dateMillis: Long = System.currentTimeMillis(),
    val validUntilMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "quotation_items",
    foreignKeys = [ForeignKey(
        entity = Quotation::class,
        parentColumns = ["id"],
        childColumns = ["quotationId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("quotationId")]
)
data class QuotationItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val quotationId: Long = 0,
    val srNo: Int = 0,
    val description: String = "",
    val itemCode: String = "",
    val quantityText: String = "01nos",
    val priceRupees: Double = 0.0
)
