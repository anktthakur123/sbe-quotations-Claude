package com.sidhbaba.quotemaker

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.sidhbaba.quotemaker.data.AppDatabase
import com.sidhbaba.quotemaker.data.Customer
import com.sidhbaba.quotemaker.databinding.ActivityCustomerEditBinding
import kotlinx.coroutines.launch

class CustomerEditActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCustomerEditBinding
    private var customerId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerEditBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }

        customerId = intent.getLongExtra("customerId", 0L)
        if (customerId != 0L) {
            binding.toolbar.title = "Edit Customer"
            binding.btnDelete.visibility = android.view.View.VISIBLE
            lifecycleScope.launch {
                AppDatabase.get(this@CustomerEditActivity).customerDao().getById(customerId)?.let { c ->
                    binding.etName.setText(c.name)
                    binding.etCompany.setText(c.companyName)
                    binding.etAddress.setText(c.address)
                    binding.etGstin.setText(c.gstin)
                }
            }
        } else {
            binding.toolbar.title = "New Customer"
        }

        binding.btnSave.setOnClickListener { save() }
        binding.btnDelete.setOnClickListener { delete() }
    }

    private fun save() {
        // Customer name is optional by design: blank name must never block saving.
        val customer = Customer(
            id = customerId,
            name = binding.etName.text?.toString()?.trim().orEmpty(),
            companyName = binding.etCompany.text?.toString()?.trim().orEmpty(),
            address = binding.etAddress.text?.toString()?.trim().orEmpty(),
            gstin = binding.etGstin.text?.toString()?.trim().orEmpty()
        )
        lifecycleScope.launch {
            AppDatabase.get(this@CustomerEditActivity).customerDao().upsert(customer)
            Toast.makeText(this@CustomerEditActivity, "Saved", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun delete() {
        lifecycleScope.launch {
            AppDatabase.get(this@CustomerEditActivity).customerDao().getById(customerId)?.let {
                AppDatabase.get(this@CustomerEditActivity).customerDao().delete(it)
            }
            finish()
        }
    }
}
