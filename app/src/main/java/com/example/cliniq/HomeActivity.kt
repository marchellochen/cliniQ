package com.example.cliniq

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.viewpager2.widget.ViewPager2
import android.content.Intent
import com.example.cliniq.databinding.ActivityHomeBinding
import kotlin.math.abs

/**
 * The main screen after login: a swipeable pager with a bottom bar.
 * (Still called HomeActivity so Login, Signup and goHome() keep working.)
 */
class HomeActivity : BaseActivity() {

    private lateinit var binding: ActivityHomeBinding

    // Same order as the pages in MainPagerAdapter
    private val tabs = listOf(
        R.id.nav_home,
        R.id.nav_appointments,
        R.id.nav_history,
        R.id.nav_profile,
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupBar("cliniQ", showBack = false)

        supportActionBar?.apply {
            setDisplayShowTitleEnabled(false)   // hide the plain-text title
            setDisplayShowCustomEnabled(true)
            setCustomView(R.layout.actionbar_logo)
        }

        binding.pager.adapter = MainPagerAdapter(this)
        binding.pager.offscreenPageLimit = tabs.size - 1   // keep every tab alive

        // Tap on the bottom bar -> change page
        binding.bottomNav.setOnItemSelectedListener { item ->
            val page = tabs.indexOf(item.itemId)

            // A swipe already moved the pager; only react to real taps
            if (page != binding.pager.currentItem) {
                val smooth = abs(page - binding.pager.currentItem) == 1
                binding.pager.setCurrentItem(page, smooth)
            }
            true
        }

        // On any other tab, Back goes to Home first (and only then leaves the app)
        val backToHome = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                binding.pager.currentItem = 0
            }
        }
        onBackPressedDispatcher.addCallback(this, backToHome)

        // Swipe -> update the bottom bar
        binding.pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                binding.bottomNav.selectedItemId = tabs[position]
                backToHome.isEnabled = position != 0
            }
        })
    }

    /** Lets a fragment jump to another tab. */
    fun showTab(itemId: Int) {
        binding.bottomNav.selectedItemId = itemId
    }

    // Another screen asked for a specific tab (see goHome in BaseActivity)
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)

        val tab = intent.getIntExtra(EXTRA_TAB, 0)
        showTab(if (tab != 0) tab else R.id.nav_home)
    }

    companion object {
        const val EXTRA_TAB = "tab"
    }
}
