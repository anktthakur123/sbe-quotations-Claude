package com.sidhbaba.quotemaker.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sidhbaba.quotemaker.HistoryAdapter
import com.sidhbaba.quotemaker.MainActivity
import com.sidhbaba.quotemaker.PdfPreviewActivity
import com.sidhbaba.quotemaker.data.AppDatabase
import com.sidhbaba.quotemaker.databinding.FragmentHistoryBinding
import com.sidhbaba.quotemaker.pdf.PdfGenerator
import com.sidhbaba.quotemaker.util.PdfPaths
import kotlinx.coroutines.launch

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: HistoryAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = HistoryAdapter(
            emptyList(),
            onEdit = { q ->
                EditBus.pendingEditQuotationId = q.id
                (requireActivity() as? MainActivity)?.switchToTab(0)
            },
            onOpenPdf = { q -> regenerateAndOpen(q.id) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        viewLifecycleOwner.lifecycleScope.launch {
            val list = AppDatabase.get(requireContext()).quotationDao().getAll()
            if (_binding == null) return@launch
            adapter.update(list)
            binding.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    private fun regenerateAndOpen(quotationId: Long) {
        viewLifecycleOwner.lifecycleScope.launch {
            val dao = AppDatabase.get(requireContext()).quotationDao()
            val q = dao.getById(quotationId) ?: return@launch
            val items = dao.getItems(quotationId)
            val pdfFile = PdfPaths.resolveOutputFile(requireContext(), q.quoteNo)
            try {
                PdfGenerator.generate(requireContext(), q, items, pdfFile)
                val i = Intent(requireContext(), PdfPreviewActivity::class.java)
                i.putExtra("pdfPath", pdfFile.absolutePath)
                startActivity(i)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Could not generate PDF: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
