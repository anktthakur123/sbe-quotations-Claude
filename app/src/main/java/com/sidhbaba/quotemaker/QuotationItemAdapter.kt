package com.sidhbaba.quotemaker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sidhbaba.quotemaker.data.QuotationItem
import com.sidhbaba.quotemaker.databinding.ItemQuotationItemBinding

class QuotationItemAdapter(
    private val items: MutableList<QuotationItem>,
    private val onChanged: () -> Unit
) : RecyclerView.Adapter<QuotationItemAdapter.VH>() {

    inner class VH(val binding: ItemQuotationItemBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemQuotationItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.tvSrNo.text = (position + 1).toString()

        holder.binding.etDescription.setText(item.description)
        holder.binding.etItemCode.setText(item.itemCode)
        holder.binding.tvQty.text = item.quantityText.ifBlank { "01nos" }
        holder.binding.etPrice.setText(if (item.priceRupees == 0.0) "" else trimZero(item.priceRupees))

        holder.binding.etDescription.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) commit(holder) { it.copy(description = holder.binding.etDescription.text?.toString().orEmpty()) }
        }
        holder.binding.etItemCode.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) commit(holder) { it.copy(itemCode = holder.binding.etItemCode.text?.toString().orEmpty()) }
        }
        holder.binding.etPrice.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val value = holder.binding.etPrice.text?.toString()?.trim()?.toDoubleOrNull() ?: 0.0
                commit(holder) { it.copy(priceRupees = value) }
            }
        }

        holder.binding.btnDelete.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                items.removeAt(pos)
                notifyDataSetChanged()
                onChanged()
            }
        }
    }

    private fun commit(holder: VH, transform: (QuotationItem) -> QuotationItem) {
        val pos = holder.bindingAdapterPosition
        if (pos != RecyclerView.NO_POSITION && pos < items.size) {
            items[pos] = transform(items[pos])
            onChanged()
        }
    }

    private fun trimZero(d: Double): String =
        if (d == d.toLong().toDouble()) d.toLong().toString() else d.toString()

    override fun getItemCount() = items.size

    fun refresh() = notifyDataSetChanged()
}
