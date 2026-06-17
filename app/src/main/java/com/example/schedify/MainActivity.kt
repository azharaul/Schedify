package com.example.schedify

import android.Manifest
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    lateinit var mainLayout: androidx.coordinatorlayout.widget.CoordinatorLayout
    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var tvTitle: TextView
    private lateinit var tvSubtitle: TextView
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
        tvTitle = findViewById(R.id.tvTitle)
        tvSubtitle = findViewById(R.id.tvCountdown)
        fabAddSchedule = findViewById(R.id.fabAddSchedule)

        setupTitleGradient()

        // Setup pager adapter
        val pagerAdapter = DayPagerAdapter(this)
        viewPager.adapter = pagerAdapter
        viewPager.offscreenPageLimit = 1

        // Attach TabLayout to ViewPager2
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = pagerAdapter.getDayAt(position)
        }.attach()

        setupTabStyle()

        fabAddSchedule.setOnClickListener {
            val currentDay = getCurrentSelectedDay()
            AddScheduleDialog(this, onSave = { schedule ->
                if (schedule.id == 0) {
                    viewModel.insertSchedule(schedule)
                    showSnackbar(getString(R.string.msg_saved))
                } else {
                    viewModel.updateSchedule(schedule)
                    showSnackbar(getString(R.string.msg_updated))
                }
            }, onDelete = { schedule ->
                deleteWithUndo(schedule)
            }).show(preselectedDay = currentDay)
        }

        // Initialize data if empty
        viewModel.initializeDefaultSchedules()

        // Observe data dari ViewModel
        lifecycleScope.launch {
            viewModel.allSchedules.collect { schedules ->
                allSchedules = schedules

                // Update subtitle and reminders on every change
                updateSubtitle(allSchedules, getCurrentSelectedDay())
                scheduleAllReminders()

                // On first data arrival, auto-select the current day once
                if (!initialDaySelected) {
                    selectCurrentDay()
                    initialDaySelected = true
                }
            }
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                updateSubtitle(allSchedules, getCurrentSelectedDay())
            }
        })

        checkNotificationPermission()
    }

    private fun setupTitleGradient() {
        tvTitle.post {
            val width = tvTitle.paint.measureText(tvTitle.text.toString())
            
            val shader = LinearGradient(
                0f, 0f, width, 0f,
                intArrayOf(
                    Color.parseColor("#2563EB"),
                    Color.parseColor("#00C9B8")
                ),
                null,
                Shader.TileMode.CLAMP
            )
            tvTitle.paint.shader = shader
            tvTitle.invalidate()
        }
    }

    private fun updateSubtitle(scheduleList: List<Schedule>, selectedDay: String) {
        val todaySchedules = scheduleList.filter { it.day == selectedDay }
        
        tvSubtitle.text = when {
            todaySchedules.isEmpty() -> "Tidak ada jadwal hari ini"
            todaySchedules.size == 1 -> "1 kegiatan hari ini"
            else -> "${todaySchedules.size} kegiatan hari ini"
        }
    }

    private fun setupTabStyle() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.view?.apply {
                    val gd = GradientDrawable()
                    gd.setColor(Color.WHITE)
                    gd.cornerRadius = 20f * resources.displayMetrics.density
                    background = gd
                    elevation = 2f * resources.displayMetrics.density
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.view?.background = null
                tab?.view?.elevation = 0f
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    fun deleteWithUndo(schedule: Schedule) {
        // remove immediately
        viewModel.deleteSchedule(schedule)

        val snackbar = Snackbar.make(mainLayout, getString(R.string.msg_deleted), Snackbar.LENGTH_LONG)
        snackbar.setAction(getString(R.string.action_undo)) {
            // re-insert schedule with original id
            viewModel.restoreSchedule(schedule)
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
                // Request permission if not granted
                val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                return
            }
        }

        cancelAllReminders()

        allSchedules.forEach { schedule ->
            val startTime = schedule.time.substringBefore(" –").trim()
            val parts = startTime.split(":")
            if (parts.size != 2) return@forEach
            val hour = parts[0].toIntOrNull() ?: return@forEach
            val minute = parts[1].toIntOrNull() ?: return@forEach

            val scheduleDayOfWeek = getDayOfWeek(schedule.day)
            if (scheduleDayOfWeek == -1) return@forEach

            val reminderTimes = listOf(30, 20, 10, 0)
            reminderTimes.forEach { reminderMinute ->
                val targetCal = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_WEEK, scheduleDayOfWeek)
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    add(Calendar.MINUTE, -reminderMinute)

                    // If the time has already passed this week, schedule for next week
                    if (timeInMillis <= System.currentTimeMillis()) {
                        add(Calendar.WEEK_OF_YEAR, 1)
                    }
                }

                val intent = Intent(this, ScheduleReminderReceiver::class.java).apply {
                    putExtra("title", schedule.title)
                    putExtra("time", schedule.time)
                    putExtra("minutes_before", reminderMinute)
                    putExtra("schedule_id", schedule.id)
                }

                val requestCode = schedule.id * 100 + reminderMinute

                val pendingIntent = PendingIntent.getBroadcast(
                    this, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, targetCal.timeInMillis, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, targetCal.timeInMillis, pendingIntent)
                }
            }
        }
    }

    private fun getDayOfWeek(dayName: String): Int {
        return when (dayName) {
            "Senin" -> Calendar.MONDAY
            "Selasa" -> Calendar.TUESDAY
            "Rabu" -> Calendar.WEDNESDAY
            "Kamis" -> Calendar.THURSDAY
            "Jumat" -> Calendar.FRIDAY
            "Sabtu" -> Calendar.SATURDAY
            "Minggu" -> Calendar.SUNDAY
            else -> -1
        }
    }

    private fun cancelAllReminders() {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        allSchedules.forEach { schedule ->
            listOf(30, 20, 10, 0).forEach { reminderMinute ->
                val requestCode = schedule.id * 100 + reminderMinute
                val intent = Intent(this, ScheduleReminderReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(this, requestCode, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
                if (pendingIntent != null) {
                    alarmManager.cancel(pendingIntent)
                }
            }
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
