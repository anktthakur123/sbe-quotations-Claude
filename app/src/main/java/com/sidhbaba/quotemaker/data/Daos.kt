package com.sidhbaba.quotemaker.data

import androidx.room.*

@Dao
interface CustomerDao {
    @Query("SELECT * FROM customers ORDER BY companyName, name")
    suspend fun getAll(): List<Customer>

    @Query("SELECT * FROM customers WHERE id = :id")
    suspend fun getById(id: Long): Customer?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(customer: Customer): Long

    @Delete
    suspend fun delete(customer: Customer)
}

@Dao
interface QuotationDao {
    @Query("SELECT * FROM quotations ORDER BY id DESC")
    suspend fun getAll(): List<Quotation>

    @Query("SELECT * FROM quotations WHERE id = :id")
    suspend fun getById(id: Long): Quotation?

    @Query("SELECT COUNT(*) FROM quotations")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(quotation: Quotation): Long

    @Delete
    suspend fun delete(quotation: Quotation)

    @Query("SELECT * FROM quotation_items WHERE quotationId = :quotationId ORDER BY srNo")
    suspend fun getItems(quotationId: Long): List<QuotationItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(items: List<QuotationItem>)

    @Query("DELETE FROM quotation_items WHERE quotationId = :quotationId")
    suspend fun deleteItemsForQuotation(quotationId: Long)
}
