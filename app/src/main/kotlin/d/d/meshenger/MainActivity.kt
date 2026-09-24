/*
* Copyright (C) 2025 Meshenger Contributors
* SPDX-License-Identifier: GPL-3.0-or-later
*/

package d.d.meshenger

import android.app.Dialog
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.drawable.Drawable
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.ContactsContract
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomnavigation.BottomNavigationView
import d.d.meshenger.MainService.MainBinder
import java.util.Locale
import androidx.core.graphics.drawable.toDrawable

// the main view with tabs
class MainActivity : BaseActivity(), ServiceConnection {
    internal lateinit var binder: MainBinder
    private lateinit var viewPager: ViewPager2

    private fun initToolbar() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar.apply {
            setNavigationOnClickListener {
                finish()
            }
            popupTheme = R.style.ThemeOverlay_Meshenger_Popup
        }
        setSupportActionBar(toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(false)
            setDisplayShowTitleEnabled(false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(this, "onCreate()")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initToolbar()

        instance = this

        viewPager = findViewById(R.id.container)

        // start MainService and call back via onServiceConnected()
        MainService.start(applicationContext)

        bindService(Intent(applicationContext, MainService::class.java), this, 0)
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
        unbindService(this)
    }

    private fun isWifiConnected(): Boolean {
        val connectivityManager = applicationContext.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                //activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                else -> false
            }
        } else {
            val connManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
            @Suppress("DEPRECATION")
            val mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI) ?: return false
            @Suppress("DEPRECATION")
            return mWifi.isConnected
        }
    }

    private fun showInvalidAddressSettingsWarning() {
        Handler(Looper.getMainLooper()).postDelayed({
            val storedAddresses = Database.getSettings().addresses
            val storedIPAddresses = storedAddresses.filter { AddressUtils.isIPAddress(it) }
            if (storedAddresses.isNotEmpty() && storedIPAddresses.isEmpty()) {
                // ignore, we only have domains configured
            } else if (storedAddresses.isEmpty()) {
                // no addresses configured at all
                Toast.makeText(this, R.string.warning_no_addresses_configured, Toast.LENGTH_LONG).show()
            } else {
                if (isWifiConnected()) {
                    val systemAddresses = AddressUtils.collectAddresses().map { it.address }
                    if (storedIPAddresses.intersect(systemAddresses.toSet()).isEmpty()) {
                        // none of the configured addresses are used in the system
                        // addresses might have changed!
                        Toast.makeText(applicationContext, R.string.warning_no_addresses_found, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }, 700)
    }

    override fun onServiceConnected(componentName: ComponentName, iBinder: IBinder) {
        Log.d(this, "onServiceConnected()")
        this.binder = iBinder as MainBinder
        MainActivity.binder = this.binder

        val adapter = ViewPagerFragmentAdapter(this)

        this.viewPager.adapter = adapter

        val settings = Database.getSettings()

        // data source for the views was not ready before
        adapter.let {
            it.ready = true
            it.disableCallHistory = settings.disableCallHistory
            it.notifyDataSetChanged()
        }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
        if (settings.disableCallHistory) {
            bottomNav.visibility = View.GONE
            bottomNav.menu.findItem(R.id.nav_calls)?.isVisible = false
        } else {
            bottomNav.visibility = View.VISIBLE
            bottomNav.menu.findItem(R.id.nav_calls)?.isVisible = true
        }
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_contacts -> viewPager.currentItem = 0
                R.id.nav_calls -> viewPager.currentItem = 1
            }
            true
        }
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val id = if (position == 0) R.id.nav_contacts else R.id.nav_calls
                if (bottomNav.selectedItemId != id) bottomNav.selectedItemId = id
            }
        })
        updateCallsBadge()

        val toolbarLabel = findViewById<TextView>(R.id.toolbar_label)
        if (settings.showUsernameAsLogo) {
            toolbarLabel.visibility = View.VISIBLE
            toolbarLabel.text = settings.username
        } else {
            toolbarLabel.visibility = View.GONE
        }

        MainService.refreshEvents(applicationContext)
        MainService.refreshContacts(applicationContext)
        showInvalidAddressSettingsWarning()
    }

    override fun onServiceDisconnected(componentName: ComponentName) {
        // nothing to do
    }

    private fun menuAction(itemId: Int) {
        when (itemId) {
            R.string.menu_settings -> {
                startActivity(Intent(applicationContext, SettingsActivity::class.java))
            }
            R.string.menu_backup -> {
                startActivity(Intent(applicationContext, BackupActivity::class.java))
            }
            R.string.menu_about -> {
                startActivity(Intent(applicationContext, AboutActivity::class.java))
            }
            R.string.menu_shutdown -> {
                MainService.stop(applicationContext)
                finish()
            }
        }
    }

    // request password for setting activity
    private fun showMenuPasswordDialog(itemId: Int, menuPassword: String) {
        val dialog = Dialog(applicationContext)
        dialog.setContentView(R.layout.dialog_enter_database_password)
        dialog.setCancelable(false)
        dialog.setCanceledOnTouchOutside(false)

        val passwordEditText = dialog.findViewById<EditText>(R.id.change_password_edit_textview)
        val exitButton = dialog.findViewById<Button>(R.id.change_password_cancel_button)
        val okButton = dialog.findViewById<Button>(R.id.change_password_ok_button)
        okButton.setOnClickListener {
            val password = passwordEditText.text.toString()
            if (menuPassword == password) {
                // start menu action
                menuAction(itemId)
            } else {
                Toast.makeText(applicationContext, R.string.wrong_password, Toast.LENGTH_SHORT).show()
            }

            // close dialog
            dialog.dismiss()
        }

        exitButton.setOnClickListener {
            // close dialog
            dialog.dismiss()
        }

        dialog.show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Log.d(this, "onOptionsItemSelected()")

        val settings = Database.getSettings()
        if (settings.menuPassword.isEmpty()) {
            menuAction(item.itemId)
        } else {
            showMenuPasswordDialog(item.itemId, settings.menuPassword)
        }

        return super.onOptionsItemSelected(item)
    }

    fun updateEventTabTitle() {
        Log.d(this, "updateEventTabTitle()")
        (viewPager.adapter as ViewPagerFragmentAdapter?)?.notifyDataSetChanged()
        updateCallsBadge()
    }

    private fun updateCallsBadge() {
        try {
            val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_nav)
            val missed = try { Database.getEvents().eventsMissed } catch (_: Exception) { 0 }
            if (missed > 0 && bottomNav.visibility == View.VISIBLE) {
                val badge = bottomNav.getOrCreateBadge(R.id.nav_calls)
                badge.isVisible = true
                badge.number = missed
            } else {
                bottomNav.removeBadge(R.id.nav_calls)
            }
        } catch (e: Exception) {
            Log.e(this, "updateCallsBadge: ${e.message}")
        }
    }

    override fun onResume() {
        Log.d(this, "onResume()")
        super.onResume()

        updateEventTabTitle()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        Log.d(this, "onCreateOptionsMenu()")

        val hideMenus = Database.getSettings().hideMenus
        val titles =  if (hideMenus) {
            mutableListOf(R.string.menu_settings)
        } else {
            mutableListOf(
                R.string.menu_settings, R.string.menu_backup,
                R.string.menu_about, R.string.menu_shutdown)
        }

        for (title in titles) {
            menu.add(0, title, 0, title)
        }

        return true
    }

    class ViewPagerFragmentAdapter(fm: FragmentActivity) : FragmentStateAdapter(fm) {
        var ready = false
        var disableCallHistory = false

        override fun getItemCount(): Int {
            return if (ready) {
                if (disableCallHistory) {
                    1
                } else {
                    2
                }
            } else {
                0
            }
        }

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> ContactListFragment()
                else -> EventListFragment()
            }
        }
    }

    companion object {
        private var addressWarningShown = false
        var instance: MainActivity? = null
        // to be used by the fragments
        var binder: MainBinder? = null
    }
}
