package com.sidhbaba.quotemaker.ui

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.sidhbaba.quotemaker.QuotationItemAdapter
import com.sidhbaba.quotemaker.data.AppDatabase
import com.sidhbaba.quotemaker.data.Customer
import com.sidhbaba.quotemaker.data.Quotation
import com.sidhbaba.quotemaker.data.QuotationItem
import com.sidhbaba.quotemaker.databinding.FragmentCreateBinding
import com.sidhbaba.quotemaker.pdf.PdfGenerator
import com.sidhbaba.quotemaker.util.ItemPasteParser
import com.sidhbaba.quotemaker.util.PdfPaths
import com.sidhbaba.quotemaker.util.Prefs
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit

class CreateFragment : Fragment() {
    private var _binding: FragmentCreateBinding? = null
    private val binding get() = _binding!!

    private lateinit var prefs: Prefs
    private lateinit var itemAdapter: QuotationItemAdapter
    private val itemList = mutableListOf<QuotationItem>()
    private var customers: List<Customer> = emptyList()
    private var selectedCustomerId: Long = 0L

    private var editingQuotationId: Long = 0L
    private var quoteNo: String = ""
    private var quoteDateMillis: Long = System.currentTimeMillis()
    private var validUntilMillis: Long = System.currentTimeMillis()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())

        itemAdapter = QuotationItemAdapter(itemList) { updateItemsEmptyState() }
        binding.recyclerItems.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerItems.adapter = itemAdapter

        binding.etQuoteDate.setOnClickListener { pickDate(isQuoteDate = true) }
        binding.etValidUntil.setOnClickListener { pickDate(isQuoteDate = false) }

        binding.spinnerCustomer.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, v: View?, position: Int, id: Long) {
                if (position <= 0) {
                    selectedCustomerId = 0L
                    return
                }
                val c = customers[position - 1]
                selectedCustomerId = c.id
                binding.etCustName.setText(c.name)
                binding.etCustCompany.setText(c.companyName)
                binding.etCustAddress.setText(c.address)
                binding.etCustGstin.setText(c.gstin)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        binding.btnClearCustomer.setOnClickListener {
            binding.spinnerCustomer.setSelection(0)
            selectedCustomerId = 0L
            binding.etCustName.setText("")
            binding.etCustCompany.setText("")
            binding.etCustAddress.setText("")
            binding.etCustGstin.setText("")
        }

        binding.btnSaveCustomer.setOnClickListener { saveCustomerFromFields() }

        binding.btnParse.setOnClickListener { parsePastedItems() }
        binding.btnClearPaste.setOnClickListener { binding.etPaste.setText("") }
        binding.btnAddItem.setOnClickListener { addBlankItem() }

        binding.btnNewQuotation.setOnClickListener { resetForNewQuotation() }

        binding.btnSavePdf.setOnClickListener { saveAndExport(showDialog = false) }
        binding.btnGenerateShare.setOnClickListener { saveAndExport(showDialog = true) }

        viewLifecycleOwner.lifecycleScope.launch {
            loadCustomers()
            val pendingEdit = EditBus.pendingEditQuotationId
            if (pendingEdit != null) {
                EditBus.pendingEditQuotationId = null
                loadExistingQuotation(pendingEdit)
            } else if (editingQuotationId == 0L && quoteNo.isBlank()) {
                startFreshQuotation()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Coming back from History with a pending edit (e.g. tab switched while fragment was already created).
        val pendingEdit = EditBus.pendingEditQuotationId
        if (pendingEdit != null && _binding != null) {
            EditBus.pendingEditQuotationId = null
            viewLifecycleOwner.lifecycleScope.launch { loadExistingQuotation(pendingEdit) }
        }
    }

    private fun startFreshQuotation() {
        editingQuotationId = 0L
        quoteDateMillis = System.currentTimeMillis()
        validUntilMillis = quoteDateMillis + TimeUnit.DAYS.toMillis(90)
        quoteNo = "${prefs.quotePrefix}${prefs.nextSerial}" // peeked, not consumed yet
        binding.etQuoteNo.setText(quoteNo)
        binding.etQuoteDate.setText(formatDate(quoteDateMillis))
        binding.etValidUntil.setText(formatDate(validUntilMillis))
        binding.etSubject.setText("")
        binding.btnClearCustomer.performClick()
        itemList.clear()
        itemAdapter.refresh()
        updateItemsEmptyState()
        binding.etPaste.setText("")
    }

    private fun resetForNewQuotation() {
        startFreshQuotation()
        Toast.makeText(requireContext(), "Started a new quotation: $quoteNo", Toast.LENGTH_SHORT).show()
    }

    private suspend fun loadCustomers() {
        if (_binding == null) return
        customers = AppDatabase.get(requireContext()).customerDao().getAll()
        val labels = mutableListOf("Select saved customer...")
        labels.addAll(customers.map { c ->
            if (c.companyName.isNotBlank()) c.companyName else c.name.ifBlank { "Unnamed customer" }
        })
        binding.spinnerCustomer.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, labels)
    }

    private suspend fun loadExistingQuotation(id: Long) {
        val dao = AppDatabase.get(requireContext()).quotationDao()
        val q = dao.getById(id) ?: return
        if (_binding == null) return
        editingQuotationId = q.id
        quoteNo = q.quoteNo
        quoteDateMillis = q.dateMillis
        validUntilMillis = q.validUntilMillis
        binding.etQuoteNo.setText(quoteNo)
        binding.etQuoteDate.setText(formatDate(quoteDateMillis))
        binding.etValidUntil.setText(formatDate(validUntilMillis))
        binding.etSubject.setText(q.subject)

        selectedCustomerId = q.customerId
        val idx = customers.indexOfFirst { it.id == q.customerId }
        binding.spinnerCustomer.setSelection(if (idx >= 0) idx + 1 else 0)
        binding.etCustName.setText(q.customerNameSnapshot)
        binding.etCustCompany.setText(q.customerCompanySnapshot)
        binding.etCustAddress.setText(q.customerAddressSnapshot)
        binding.etCustGstin.setText(q.customerGstinSnapshot)

        val items = dao.getItems(id)
        itemList.clear()
        itemList.addAll(items)
        itemAdapter.refresh()
        updateItemsEmptyState()
    }

    private fun pickDate(isQuoteDate: Boolean) {
        val cal = Calendar.getInstance()
        cal.timeInMillis = if (isQuoteDate) quoteDateMillis else validUntilMillis
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val c = Calendar.getInstance()
                c.set(year, month, day, 0, 0, 0)
                if (isQuoteDate) {
                    quoteDateMillis = c.timeInMillis
                    binding.etQuoteDate.setText(formatDate(quoteDateMillis))
                } else {
                    validUntilMillis = c.timeInMillis
                    binding.etValidUntil.setText(formatDate(validUntilMillis))
                }
            },
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun formatDate(millis: Long): String =
        java.text.SimpleDateFormat("dd-MMM-yyyy", java.util.Locale.getDefault()).format(millis)

    private fun saveCustomerFromFields() {
        val customer = Customer(
            id = selectedCustomerId,
            name = binding.etCustName.text?.toString()?.trim().orEmpty(),
            companyName = binding.etCustCompany.text?.toString()?.trim().orEmpty(),
            address = binding.etCustAddress.text?.toString()?.trim().orEmpty(),
            gstin = binding.etCustGstin.text?.toString()?.trim().orEmpty()
        )
        viewLifecycleOwner.lifecycleScope.launch {
            val newId = AppDatabase.get(requireContext()).customerDao().upsert(customer)
            selectedCustomerId = newId
            loadCustomers()
            val idx = customers.indexOfFirst { it.id == newId }
            if (idx >= 0 && _binding != null) binding.spinnerCustomer.setSelection(idx + 1)
            if (_binding != null) Toast.makeText(requireContext(), "Saved to Customer Master", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parsePastedItems() {
        val raw = binding.etPaste.text?.toString().orEmpty()
        val parsed = ItemPasteParser.parse(raw)
        if (parsed.isEmpty()) {
            Toast.makeText(requireContext(), "Could not detect items. Check the pasted text.", Toast.LENGTH_LONG).show()
            return
        }
        parsed.forEach { p ->
            itemList.add(
                QuotationItem(
                    srNo = itemList.size + 1,
                    description = p.description,
                    itemCode = p.itemCode,
                    quantityText = "01nos",
                    priceRupees = 0.0
                )
            )
        }
        itemAdapter.refresh()
        updateItemsEmptyState()
        binding.etPaste.setText("")
        Toast.makeText(requireContext(), "Added ${parsed.size} item(s)", Toast.LENGTH_SHORT).show()
    }

    private fun addBlankItem() {
        itemList.add(QuotationItem(srNo = itemList.size + 1, quantityText = "01nos"))
        itemAdapter.refresh()
        updateItemsEmptyState()
        binding.recyclerItems.post { binding.recyclerItems.smoothScrollToPosition(itemList.size - 1) }
    }

    private fun updateItemsEmptyState() {
        if (_binding == null) return
        binding.tvNoItems.visibility = if (itemList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun saveAndExport(showDialog: Boolean) {
        binding.root.clearFocus()
        // Give any focused EditText a chance to commit its onFocusChangeListener before we read values.
        binding.recyclerItems.post { doSave(showDialog) }
    }

    private fun doSave(showDialog: Boolean) {
        if (_binding == null) return
        if (itemList.isEmpty()) {
            Toast.makeText(requireContext(), "Add at least one item first", Toast.LENGTH_SHORT).show()
            return
        }

        val finalQuoteNo = if (editingQuotationId == 0L) prefs.takeNextQuoteNumber() else quoteNo
        quoteNo = finalQuoteNo

        val quotation = Quotation(
            id = editingQuotationId,
            quoteNo = finalQuoteNo,
            customerId = selectedCustomerId,
            customerNameSnapshot = binding.etCustName.text?.toString()?.trim().orEmpty(),
            customerCompanySnapshot = binding.etCustCompany.text?.toString()?.trim().orEmpty(),
            customerAddressSnapshot = binding.etCustAddress.text?.toString()?.trim().orEmpty(),
            customerGstinSnapshot = binding.etCustGstin.text?.toString()?.trim().orEmpty(),
            subject = binding.etSubject.text?.toString()?.trim().orEmpty(),
            dateMillis = quoteDateMillis,
            validUntilMillis = validUntilMillis
        )

        viewLifecycleOwner.lifecycleScope.launch {
            val dao = AppDatabase.get(requireContext()).quotationDao()
            val id = dao.upsert(quotation)
            editingQuotationId = id
            dao.deleteItemsForQuotation(id)
            val finalItems = itemList.mapIndexed { index, it -> it.copy(quotationId = id, srNo = index + 1) }
            dao.insertItems(finalItems)

            val saved = quotation.copy(id = id)
            val pdfFile = PdfPaths.resolveOutputFile(requireContext(), finalQuoteNo)
            try {
                PdfGenerator.generate(requireContext(), saved, finalItems, pdfFile)
                if (_binding == null) return@launch
                binding.etQuoteNo.setText(finalQuoteNo)
                if (showDialog) {
                    ExportResultDialog.show(requireContext(), pdfFile)
                } else {
                    Toast.makeText(requireContext(), "Saved: ${pdfFile.name} in ${pdfFile.parentFile?.name}", Toast.LENGTH_LONG).show()
                }
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
