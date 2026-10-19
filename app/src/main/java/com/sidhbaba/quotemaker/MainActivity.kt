package com.sidhbaba.quotemaker

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayoutMediator
import com.sidhbaba.quotemaker.databinding.ActivityMainBinding
import com.sidhbaba.quotemaker.util.GithubUpdateChecker
import com.sidhbaba.quotemaker.util.Prefs
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.viewPager.adapter = MainPagerAdapter(this)
        binding.viewPager.offscreenPageLimit = 3
        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            tab.text = MainPagerAdapter.TITLES[position]
        }.attach()

        // Quiet check-on-launch for a newer release; never interrupts if nothing is found or it fails.
        checkForUpdatesSilently()

        binding.btnCheckUpdateTop.setOnClickListener { checkForUpdatesManually() }
    }

    private fun checkForUpdatesManually() {
        val prefs = Prefs(this)
        android.widget.Toast.makeText(this, "Checking GitHub for updates…", android.widget.Toast.LENGTH_SHORT).show()
        lifecycleScope.launch {
            val result = GithubUpdateChecker.checkLatest()
            prefs.lastUpdateCheckMillis = System.currentTimeMillis()
            result.onSuccess { info ->
                if (info == null) {
                    android.widget.Toast.makeText(this@MainActivity, "You're on the latest version", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    com.sidhbaba.quotemaker.ui.UpdateDialog.show(this@MainActivity, lifecycleScope, info, prefs)
                }
            }.onFailure {
                android.widget.Toast.makeText(this@MainActivity, "Could not check for updates: ${it.message}", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    }

    fun switchToTab(index: Int) {
        binding.viewPager.setCurrentItem(index, true)
    }

    private fun checkForUpdatesSilently() {
        val prefs = Prefs(this)
        lifecycleScope.launch {
            val result = GithubUpdateChecker.checkLatest()
            prefs.lastUpdateCheckMillis = System.currentTimeMillis()
            result.getOrNull()?.let { info ->
                if (info.tag != prefs.skippedVersionTag) {
                    com.sidhbaba.quotemaker.ui.UpdateDialog.show(this@MainActivity, lifecycleScope, info, prefs)
                }
            }
        }
    }
}
