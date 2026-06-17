package com.example.schedify

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import java.text.SimpleDateFormat
import java.util.*

class ScheduleListAdapter(
    private val onEditClick: (Schedule) -> Unit = {},
    private val onDeleteClick: (Schedule) -> Unit = {}
) : ListAdapter<Schedule, ScheduleListAdapter.ScheduleViewHolder>(DIFF) {

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Schedule>() {
            override fun areItemsTheSame(oldItem: Schedule, newItem: Schedule): Boolean = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Schedule, newItem: Schedule): Boolean = oldItem == newItem
        }
    }

    class ScheduleViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvScheduleTitle)
        val tvLocation: TextView = view.findViewById(R.id.tvScheduleLocation)
        val tvTime: TextView = view.findViewById(R.id.tvScheduleTime)
        val viewColorTag: View = view.findViewById(R.id.viewColorTag)
        val cardView: MaterialCardView = view.findViewById(R.id.cardView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScheduleViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_schedule, parent, false)
        return ScheduleViewHolder(view)
    }

    override fun onBindViewHolder(holder: ScheduleViewHolder, position: Int) {
        val schedule = getItem(position)
        holder.tvTitle.text = schedule.title
        if (schedule.location.isNotBlank()) {
            holder.tvLocation.visibility = View.VISIBLE
            holder.tvLocation.text = schedule.location
        } else {
            holder.tvLocation.visibility = View.GONE
        }
        holder.tvTime.text = schedule.time

        // Set left accent bar color
        val accentColor = schedule.color
        holder.viewColorTag.setBackgroundColor(accentColor)

        // Bonus Fix: Add subtle card glow matching accent bar color
        if (isCurrentSchedule(schedule)) {
            holder.cardView.strokeColor = accentColor
            holder.cardView.strokeWidth = 6
            holder.cardView.cardElevation = 12f
        } else {
            // Add subtle stroke matching accent color at low alpha
            val strokeColor = ColorUtils.setAlphaComponent(accentColor, 40) // ~15% alpha
            holder.cardView.strokeColor = strokeColor
            holder.cardView.strokeWidth = 4
            holder.cardView.cardElevation = 0f
        }

        holder.cardView.isClickable = true
        holder.cardView.setOnClickListener { onEditClick(schedule) }
    }

    private fun isCurrentSchedule(schedule: Schedule): Boolean {
        try {
            val calendar = Calendar.getInstance()
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            val currentTimeStr = sdf.format(calendar.time)

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

            val times = schedule.time.split(" – ")
            if (times.size < 2) return false

            val startTime = times[0].trim()
            val endTime = times[1].trim()

            return currentTimeStr in startTime..endTime
        } catch (e: Exception) {
            return false
        }
    }
}
