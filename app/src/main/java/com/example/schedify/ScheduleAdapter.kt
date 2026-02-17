package com.example.schedify

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class ScheduleAdapter(private val schedules: List<Schedule>) :
    RecyclerView.Adapter<ScheduleAdapter.ScheduleViewHolder>() {

    class ScheduleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvScheduleTitle)
        val tvTime: TextView = view.findViewById(R.id.tvScheduleTime)
        val viewColorTag: View = view.findViewById(R.id.viewColorTag)
        val cardView: MaterialCardView = view as MaterialCardView
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val schedule = schedules[position]
        holder.tvTitle.text = schedule.title
        holder.tvTime.text = schedule.time

        // Palet warna yang lebih ramah buta warna
        val color = when (schedule.day) {
            "Senin" -> "#0072B2"
            "Selasa" -> "#E69F00"
            "Rabu" -> "#009E73"
            "Kamis" -> "#CC79A7"
            "Jumat" -> "#D55E00"
            else -> "#56B4E9"
        }
        holder.viewColorTag.setBackgroundColor(Color.parseColor(color))

        // Cek apakah jadwal sedang berlangsung
        if (isCurrentSchedule(schedule)) {
            // Beri efek menyala (stroke lebih tebal dan warna berbeda)
            holder.cardView.strokeColor = Color.parseColor(color)
            holder.cardView.strokeWidth = 6
            holder.cardView.cardElevation = 12f
        } else {
            // Reset ke tampilan normal
            holder.cardView.strokeColor = Color.parseColor("#2C2C2C")
            holder.cardView.strokeWidth = 2
            holder.cardView.cardElevation = 0f
        }
    }

    private fun isCurrentSchedule(schedule: Schedule): Boolean {
        try {
            val calendar = Calendar.getInstance()
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val currentTimeStr = sdf.format(calendar.time)
            
            // Ambil nama hari sekarang (sesuai format data kita)
            val currentDayName = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Senin"
                Calendar.TUESDAY -> "Selasa"
                Calendar.WEDNESDAY -> "Rabu"
                Calendar.THURSDAY -> "Kamis"
                Calendar.FRIDAY -> "Jumat"
                Calendar.SATURDAY -> "Sabtu"
                Calendar.SUNDAY -> "Minggu"
                else -> ""
            }

            if (schedule.day != currentDayName) return false

            // Parsing waktu: "07:55 – 10:35" -> start: 07:55, end: 10:35
            val times = schedule.time.split(" – ")
            if (times.size < 2) return false
            
            val startTime = times[0].trim()
            val endTime = times[1].trim()

            return currentTimeStr in startTime..endTime
        } catch (e: Exception) {
            return false
        }
    }

    override fun getItemCount(): Int = schedules.size
}