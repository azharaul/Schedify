package com.example.schedify

import android.os.Bundle
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.tabs.TabLayout
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var rvSchedules: RecyclerView
    private lateinit var tabLayout: TabLayout
    private lateinit var tvCountdown: TextView

    private val allSchedules = listOf(
        // Senin
        Schedule(1, "Pemrosesan Data Terdistribusi", "07:55 – 10:35", "Senin"),
        Schedule(2, "Pemrograman Berbasis Platform", "13:00 – 16:35", "Senin"),
        // Selasa
        Schedule(3, "Riset dan Pengembangan", "07:55 – 11:30", "Selasa"),
        Schedule(4, "Kecerdasan Buatan", "13:00 – 16:35", "Selasa"),
        // Rabu
        Schedule(5, "Pemrosesan Data Terdistribusi", "07:55 – 10:30", "Rabu"),
        Schedule(6, "Dasar-Dasar Jaringan", "13:00 – 14:40", "Rabu"),
        // Kamis
        Schedule(7, "Bahasa Inggris 4", "08:00 – 09:40", "Kamis"),
        Schedule(8, "Agama 4", "10:40 – 11:30", "Kamis"),
        Schedule(9, "Dasar-Dasar Jaringan", "13:00 – 14:40", "Kamis"),
        Schedule(10, "Pemrograman Berorientasi Objek", "14:50 – 15:40", "Kamis"),
        // Jumat
        Schedule(11, "Kewirausahaan Teknologi Informasi", "13:00 – 14:45", "Jumat")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mainLayout = findViewById<androidx.coordinatorlayout.widget.CoordinatorLayout>(R.id.mainLayout)
        ViewCompat.setOnApplyWindowInsetsListener(mainLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        rvSchedules = findViewById(R.id.rvSchedules)
        tabLayout = findViewById(R.id.tabLayout)
        tvCountdown = findViewById(R.id.tvCountdown)

        rvSchedules.layoutManager = LinearLayoutManager(this)

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                val day = tab?.text.toString()
                updateList(day)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        selectCurrentDay()
        updateCountdown()
    }

    private fun updateCountdown() {
        val calendar = Calendar.getInstance()
        val currentDay = getCurrentDayName(calendar.get(Calendar.DAY_OF_WEEK))
        val currentTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(calendar.time)

        val nextSchedule = allSchedules
            .filter { it.day == currentDay }
            .filter { it.time.substringBefore(" –") > currentTime }
            .minByOrNull { it.time.substringBefore(" –") }

        if (nextSchedule != null) {
            tvCountdown.text = "Kuliah berikutnya: ${nextSchedule.title} jam ${nextSchedule.time.substringBefore(" –")}"
        } else {
            tvCountdown.text = "Tidak ada kuliah lagi hari ini"
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
            else -> "Senin"
        }
    }

    private fun selectCurrentDay() {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        val tabIndex = when (dayOfWeek) {
            Calendar.MONDAY -> 0
            Calendar.TUESDAY -> 1
            Calendar.WEDNESDAY -> 2
            Calendar.THURSDAY -> 3
            Calendar.FRIDAY -> 4
            Calendar.SATURDAY -> 5
            Calendar.SUNDAY -> 6
            else -> 0
        }
        tabLayout.getTabAt(tabIndex)?.select()
    }

    private fun updateList(day: String) {
        val filteredList = allSchedules.filter { it.day == day }
        rvSchedules.adapter = ScheduleAdapter(filteredList)
    }
}