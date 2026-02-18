package com.example.schedify

import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*
import java.text.SimpleDateFormat

class AddScheduleDialog(
    private val context: Context,
    private val onSave: (Schedule) -> Unit,
    private val onDelete: (Schedule) -> Unit = {}
) {
    private var selectedColor: Int = 0xFF2196F3.toInt() // Default blue

    fun show(scheduleToEdit: Schedule? = null, preselectedDay: String? = null) {
        val inflater = android.view.LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_add_schedule, null)

        val etTitle = view.findViewById<EditText>(R.id.etTitle)
        val etStartTime = view.findViewById<EditText>(R.id.etStartTime)
        val etEndTime = view.findViewById<EditText>(R.id.etEndTime)
        val etLocation = view.findViewById<EditText>(R.id.etLocation)
        val spinnerDay = view.findViewById<Spinner>(R.id.spinnerDay)
        val colorPickerContainer = view.findViewById<LinearLayout>(R.id.colorPickerContainer)

        // Setup colors
        val colors = listOf(
            ContextCompat.getColor(context, R.color.blue_500),
            ContextCompat.getColor(context, R.color.red_500),
            ContextCompat.getColor(context, R.color.green_500),
            ContextCompat.getColor(context, R.color.orange_500),
            ContextCompat.getColor(context, R.color.purple_500),
            ContextCompat.getColor(context, R.color.teal_500),
            ContextCompat.getColor(context, R.color.pink_500)
        )

        selectedColor = scheduleToEdit?.color ?: colors[0]

        // Function to create colored circle
        fun createColorCircle(color: Int): View {
            val size = (40 * context.resources.displayMetrics.density).toInt()
            val margin = (8 * context.resources.displayMetrics.density).toInt()
            
            val frame = FrameLayout(context)
            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins(margin, margin, margin, margin)
            frame.layoutParams = params

            val circle = View(context)
            val circleParams = FrameLayout.LayoutParams(size, size)
            circle.layoutParams = circleParams
            
            val shape = GradientDrawable()
            shape.shape = GradientDrawable.OVAL
            shape.setColor(color)
            
            // Add border if selected
            if (color == selectedColor) {
                shape.setStroke(6, Color.DKGRAY)
            }
            
            circle.background = shape
            frame.addView(circle)
            
            frame.setOnClickListener {
                selectedColor = color
                // Refresh all circles
                colorPickerContainer.removeAllViews()
                colors.forEach { c -> colorPickerContainer.addView(createColorCircle(c)) }
            }
            
            return frame
        }

        colors.forEach { color ->
            colorPickerContainer.addView(createColorCircle(color))
        }

        // Setup spinner with full week
        val days = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, days)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDay.adapter = adapter

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun showTimePicker(target: EditText) {
            val now = Calendar.getInstance()
            val h = now.get(Calendar.HOUR_OF_DAY)
            val m = now.get(Calendar.MINUTE)
            TimePickerDialog(context, { _, hourOfDay, minute ->
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                    set(Calendar.MINUTE, minute)
                }
                target.setText(timeFormat.format(cal.time))
            }, h, m, true).show()
        }

        etStartTime.setOnClickListener { showTimePicker(etStartTime) }
        etEndTime.setOnClickListener { showTimePicker(etEndTime) }

        // Jika edit, isi data yang ada
        if (scheduleToEdit != null) {
            etTitle.setText(scheduleToEdit.title)
            val parts = scheduleToEdit.time.split(" – ")
            if (parts.size >= 2) {
                etStartTime.setText(parts[0].trim())
                etEndTime.setText(parts[1].trim())
            } else {
                etStartTime.setText(scheduleToEdit.time)
            }
            etLocation.setText(scheduleToEdit.location)
            spinnerDay.setSelection(days.indexOf(scheduleToEdit.day))
        } else {
            preselectedDay?.let { pd ->
                val idx = days.indexOf(pd)
                if (idx >= 0) spinnerDay.setSelection(idx)
            }
        }

        val builder = MaterialAlertDialogBuilder(context)
            .setTitle(if (scheduleToEdit != null) context.getString(R.string.title_edit_schedule) else context.getString(R.string.title_add_schedule))
            .setView(view)
            .setPositiveButton(context.getString(R.string.btn_save)) { _, _ ->
                val title = etTitle.text.toString().trim()
                val start = etStartTime.text.toString().trim()
                val end = etEndTime.text.toString().trim()
                val location = etLocation.text.toString().trim()
                val day = spinnerDay.selectedItem.toString()

                if (title.isEmpty() || start.isEmpty() || end.isEmpty()) {
                    Toast.makeText(context, context.getString(R.string.msg_fill_fields), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                try {
                    val s = timeFormat.parse(start)!!
                    val e = timeFormat.parse(end)!!
                    if (s.after(e) || s == e) {
                        Toast.makeText(context, context.getString(R.string.msg_time_invalid), Toast.LENGTH_SHORT).show()
                        return@setPositiveButton
                    }
                } catch (ex: Exception) {
                    Toast.makeText(context, context.getString(R.string.msg_time_format_invalid), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val timeCombined = "$start – $end"
                val newSchedule = Schedule(
                    id = scheduleToEdit?.id ?: 0,
                    title = title,
                    time = timeCombined,
                    day = day,
                    location = location,
                    color = selectedColor
                )
                onSave(newSchedule)

            }
            .setNegativeButton(context.getString(R.string.btn_cancel), null)

        if (scheduleToEdit != null) {
            builder.setNeutralButton(context.getString(R.string.btn_delete)) { dialog, _ ->
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.confirm_delete_title))
                    .setMessage(context.getString(R.string.confirm_delete_message))
                    .setPositiveButton(context.getString(R.string.btn_delete)) { _, _ ->
                        onDelete(scheduleToEdit)
                    }
                    .setNegativeButton(context.getString(R.string.btn_cancel), null)
                    .show()
            }
        }

        val dialog = builder.show()

        if (scheduleToEdit != null) {
            val deleteBtn = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            try {
                val color = ContextCompat.getColor(context, com.google.android.material.R.color.m3_ref_palette_error50)
                deleteBtn.setTextColor(color)
            } catch (e: Exception) {}
        }
    }
}