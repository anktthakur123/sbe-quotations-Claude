package com.sidhbaba.quotemaker.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.sidhbaba.quotemaker.BuildConfig
import com.sidhbaba.quotemaker.databinding.FragmentSettingsBinding
import com.sidhbaba.quotemaker.util.BackupManager
import com.sidhbaba.quotemaker.util.GithubUpdateChecker
import com.sidhbaba.quotemaker.util.PdfPaths
import com.sidhbaba.quotemaker.util.Prefs
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var prefs: Prefs
    private var exportMode = ExportMode.FULL

    private enum class ExportMode { FULL, CUSTOMERS_ONLY }

    private val createDocLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) doExport(uri)
    }
    private val openDocLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) doImport(uri, customersOnly = false)
    }
    private val openDocLauncherCustomers = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) doImport(uri, customersOnly = true)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefs = Prefs(requireContext())

        binding.etPrefix.setText(prefs.quotePrefix)
        binding.etNextSerial.setText(prefs.nextSerial.toString())
        binding.tvGithubRepo.text = "${Prefs.GITHUB_OWNER} / ${Prefs.GITHUB_REPO}"
        refreshAppInfo()

        binding.btnSaveNumbering.setOnClickListener {
            val prefix = binding.etPrefix.text?.toString()?.trim().orEmpty().ifBlank { "SB" }
            val serial = binding.etNextSerial.text?.toString()?.trim()?.toIntOrNull() ?: 1
            prefs.quotePrefix = prefix
            prefs.nextSerial = serial
            Toast.makeText(requireContext(), "Numbering saved", Toast.LENGTH_SHORT).show()
        }

        binding.btnCheckUpdate.setOnClickListener { checkUpdateManually() }

        binding.btnFolderAccess.setOnClickListener { requestFolderAccess() }

        binding.btnExportBackup.setOnClickListener {
            exportMode = ExportMode.FULL
            createDocLauncher.launch("quotemaker_backup_${timestamp()}.json")
        }
        binding.btnImportBackup.setOnClickListener { openDocLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
        binding.btnExportCustomers.setOnClickListener {
            exportMode = ExportMode.CUSTOMERS_ONLY
            createDocLauncher.launch("quotemaker_customers_${timestamp()}.json")
        }
        binding.btnImportCustomers.setOnClickListener { openDocLauncherCustomers.launch(arrayOf("application/json", "text/plain", "*/*")) }
    }

    private fun timestamp() = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(System.currentTimeMillis())

    private fun refreshAppInfo() {
        val storageText = if (PdfPaths.hasAllFilesAccess()) "enabled" else "not enabled"
        binding.tvAppInfo.text =
            "Quote Maker v${BuildConfig.VERSION_NAME} · Android ${Build.VERSION.RELEASE} · Storage access: $storageText"
    }

    private fun requestFolderAccess() {
        if (Build.VERSION.SDK_INT >= 30) {
            try {
                val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                intent.data = Uri.parse("package:${requireContext().packageName}")
                startActivity(intent)
            } catch (e: Exception) {
                startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
            }
        } else {
            Toast.makeText(requireContext(), "Already have storage access on this Android version", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) refreshAppInfo()
    }

    private fun checkUpdateManually() {
        Toast.makeText(requireContext(), "Checking GitHub for updates…", Toast.LENGTH_SHORT).show()
        viewLifecycleOwner.lifecycleScope.launch {
            val result = GithubUpdateChecker.checkLatest()
            if (_binding == null) return@launch
            result.onSuccess { info ->
                if (info == null) {
                    Toast.makeText(requireContext(), "You're on the latest version", Toast.LENGTH_SHORT).show()
                } else {
                    UpdateDialog.show(requireActivity(), viewLifecycleOwner.lifecycleScope, info, prefs)
                }
            }.onFailure {
                Toast.makeText(requireContext(), "Could not check for updates: ${it.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun doExport(uri: Uri) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val json = if (exportMode == ExportMode.FULL) BackupManager.exportFullBackup(requireContext())
                else BackupManager.exportCustomersOnly(requireContext())
                BackupManager.writeToUri(requireContext(), uri, json)
                Toast.makeText(requireContext(), "Backup saved", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Export failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun doImport(uri: Uri, customersOnly: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val text = BackupManager.readFromUri(requireContext(), uri)
                val (customers, quotations) = BackupManager.importBackup(requireContext(), text)
                Toast.makeText(requireContext(), "Imported $customers customers, $quotations quotations", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
