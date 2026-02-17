package com.example.schedify

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var mainLayout: androidx.coordinatorlayout.widget.CoordinatorLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var tvCountdown: TextView
    private lateinit var fabAddSchedule: FloatingActionButton

    val viewModel: ScheduleViewModel by viewModels()
    private var allSchedules = listOf<Schedule>()
    private var initialDaySelected = false

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) scheduleAllReminders()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mainLayout = findViewById(R.id.mainLayout)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        tvCountdown = findViewById(R.id.tvCountdown)
        fabAddSchedule = findViewById(R.id.fabAddSchedule)

        // Setup pager adapter
        val pagerAdapter = DayPagerAdapter(this)
        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = 1

        // Attach TabLayout to ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = pagerAdapter.getDayAt(position)
        }.attach()

        fabAddSchedule.setOnClickListener {
            AddScheduleDialog(this, onSave = { schedule ->
                if (schedule.id == 0) {
                    viewModel.insertSchedule(schedule)
                    showSnackbar(getString(R.string.msg_saved))
                } else {
                    viewModel.updateSchedule(schedule)
                    showSnackbar(getString(R.string.msg_updated))
                }
            }).show()
        }

        // Observe data dari ViewModel
        lifecycleScope.launch {
            viewModel.allSchedules.collect { schedules ->
                allSchedules = schedules

                // Update countdown and reminders on every change
                updateCountdown()
                scheduleAllReminders()

                // On first data arrival, auto-select the current day once
                if (!initialDaySelected) {
                    selectCurrentDay()
                    initialDaySelected = true
                }
            }
        }

        checkNotificationPermission()
    }

    fun deleteWithUndo(schedule: Schedule) {
        // remove immediately
        viewModel.deleteSchedule(schedule)

        val snackbar = Snackbar.make(mainLayout, getString(R.string.msg_deleted), Snackbar.LENGTH_LONG)
        snackbar.setAction(getString(R.string.action_undo)) {
            // re-insert schedule
            viewModel.insertSchedule(schedule)
            showSnackbar(getString(R.string.msg_updated))
        }
        snackbar.show()
    }

    fun showSnackbar(message: String) {
        Snackbar.make(mainLayout, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                scheduleAllReminders()
            }
        } else {
            scheduleAllReminders()
        }
    }

    private fun scheduleAllReminders() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                return
            }
        }

        val calendar = Calendar.getInstance()
        val currentDayName = getCurrentDayName(calendar.get(Calendar.DAY_OF_WEEK))

        cancelAllReminders()

        allSchedules.filter { it.day == currentDayName }.forEach { schedule ->
            val startTime = schedule.time.substringBefore(" –")
            val hour = startTime.substringBefore(":").toInt()
            val minute = startTime.substringAfter(":").toInt()

            val baseCal = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
            }

            val reminderTimes = listOf(30, 20, 10)
            reminderTimes.forEach { reminderMinute ->
                val targetCal = baseCal.clone() as Calendar
                targetCal.add(Calendar.MINUTE, -reminderMinute)

                if (targetCal.timeInMillis > System.currentTimeMillis()) {
                    val intent = Intent(this, ScheduleReminderReceiver::class.java).apply {
                        putExtra("title", schedule.title)
                        putExtra("time", schedule.time)
                        putExtra("minutes_before", reminderMinute)
                    }

                    val requestCode = schedule.id * 100 + reminderMinute

                    val pendingIntent = PendingIntent.getBroadcast(
                        this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetCal.timeInMillis, pendingIntent)
                }
            }
        }
    }

    private fun cancelAllReminders() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        allSchedules.forEach { schedule ->
            listOf(30, 20, 10).forEach { reminderMinute ->
                val requestCode = schedule.id * 100 + reminderMinute
                val intent = Intent(this, ScheduleReminderReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                }
            }
        }
    }

    private fun updateCountdown() {
        val calendar = Calendar.getInstance()
        val currentDayName = getCurrentDayName(calendar.get(Calendar.DAY_OF_WEEK))
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTimeStr = sdf.format(calendar.time)

        val nextSchedule = allSchedules
            .filter { it.day == currentDayName }
            .filter { it.time.substringBefore(" –") > currentTimeStr }
            .minByOrNull { it.time.substringBefore(" –") }

        if (nextSchedule != null) {
            // Try to extract a clean start time; fallback to whole time string if unexpected format
            val startRaw = try {
                nextSchedule.time.substringBefore(" –").trim()
            } catch (e: Exception) {
                nextSchedule.time.trim()
            }

            // Truncate title to avoid overflow in the header (keep it readable)
            val title = nextSchedule.title.trim()
            val maxTitleLen = 30
            val displayTitle = if (title.length > maxTitleLen) title.substring(0, maxTitleLen - 1).trimEnd() + "…" else title

            tvCountdown.text = getString(R.string.next_activity, displayTitle, startRaw)
        } else {
            tvCountdown.text = getString(R.string.no_more_activities)
        }
    }

    private fun getCurrentDayName(dayOfWeek: Int): String {
        return when (dayOfWeek) {
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            Calendar.SUNDAY -> "Minggu"
            else -> ""
        }
    }

    private fun selectCurrentDay() {
        val calendar = Calendar.getInstance()
        val currentDayName = getCurrentDayName(calendar.get(Calendar.DAY_OF_WEEK))
        for (i in 0 until tabLayout.tabCount) {
            val tab = tabLayout.getTabAt(i)
            if (tab?.text == currentDayName) {
                tab.select()
                // also move viewPager
                viewPager.setCurrentItem(i, false)
                break
            }
        }
    }

    private fun getCurrentSelectedDay(): String {
        val pos = viewPager.currentItem
        val adapter = viewPager.adapter as? DayPagerAdapter
        return adapter?.getDayAt(pos) ?: getCurrentDayName(Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
    }

}
