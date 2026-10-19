package com.sidhbaba.quotemaker.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sidhbaba.quotemaker.CustomerAdapter
import com.sidhbaba.quotemaker.CustomerEditActivity
import com.sidhbaba.quotemaker.data.AppDatabase
import com.sidhbaba.quotemaker.databinding.FragmentCustomerMasterBinding
import kotlinx.coroutines.launch

class CustomerMasterFragment : Fragment() {
    private var _binding: FragmentCustomerMasterBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CustomerAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCustomerMasterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = CustomerAdapter(emptyList()) { customer ->
            val i = Intent(requireContext(), CustomerEditActivity::class.java)
            i.putExtra("customerId", customer.id)
            startActivity(i)
        }
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
        binding.fabAdd.setOnClickListener {
            startActivity(Intent(requireContext(), CustomerEditActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        reload()
    }

    private fun reload() {
        viewLifecycleOwner.lifecycleScope.launch {
            val list = AppDatabase.get(requireContext()).customerDao().getAll()
            if (_binding != null) adapter.update(list)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
