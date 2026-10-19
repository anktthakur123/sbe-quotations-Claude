package com.sidhbaba.quotemaker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sidhbaba.quotemaker.data.Customer
import com.sidhbaba.quotemaker.databinding.ItemCustomerBinding

class CustomerAdapter(
    private var items: List<Customer>,
    private val onClick: (Customer) -> Unit
) : RecyclerView.Adapter<CustomerAdapter.VH>() {

    inner class VH(val binding: ItemCustomerBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemCustomerBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.binding.tvCompany.text = c.companyName.ifBlank { "(No company name)" }
        holder.binding.tvName.text = c.name
        holder.binding.tvName.visibility = if (c.name.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
        holder.binding.tvAddress.text = c.address
        holder.binding.tvAddress.visibility = if (c.address.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
        holder.binding.tvGstin.text = if (c.gstin.isNotBlank()) "GSTIN: ${c.gstin}" else ""
        holder.binding.tvGstin.visibility = if (c.gstin.isBlank()) android.view.View.GONE else android.view.View.VISIBLE
        holder.binding.root.setOnClickListener { onClick(c) }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<Customer>) {
        items = newItems
        notifyDataSetChanged()
    }
}
