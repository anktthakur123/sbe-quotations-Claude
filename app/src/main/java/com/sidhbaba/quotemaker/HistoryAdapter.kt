package com.sidhbaba.quotemaker

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.sidhbaba.quotemaker.data.Quotation
import com.sidhbaba.quotemaker.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryAdapter(
    private var items: List<Quotation>,
    private val onEdit: (Quotation) -> Unit,
    private val onOpenPdf: (Quotation) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.VH>() {

    private val dateFmt = SimpleDateFormat("dd-MMM-yyyy", Locale.getDefault())

    inner class VH(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val b = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(b)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val q = items[position]
        holder.binding.tvQuoteNo.text = q.quoteNo
        val customerLabel = q.customerCompanySnapshot.ifBlank { q.customerNameSnapshot.ifBlank { "No customer" } }
        holder.binding.tvCustomer.text = "$customerLabel  •  ${q.subject}"
        holder.binding.tvDate.text = dateFmt.format(q.dateMillis)
        holder.binding.root.setOnClickListener { onEdit(q) }
        holder.binding.btnOpenPdf.setOnClickListener { onOpenPdf(q) }
    }

    override fun getItemCount() = items.size

    fun update(newItems: List<Quotation>) {
        items = newItems
        notifyDataSetChanged()
    }
}
