package com.example.schedify

import android.app.TimePickerDialog
import android.content.Context
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
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
    fun show(scheduleToEdit: Schedule? = null) {
        val inflater = android.view.LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_add_schedule, null)

        val etTitle = view.findViewById<EditText>(R.id.etTitle)
        val etStartTime = view.findViewById<EditText>(R.id.etStartTime)
        val etEndTime = view.findViewById<EditText>(R.id.etEndTime)
        val etLocation = view.findViewById<EditText>(R.id.etLocation)
        val spinnerDay = view.findViewById<Spinner>(R.id.spinnerDay)

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
            // parsing time "HH:mm – HH:mm"
            val parts = scheduleToEdit.time.split(" – ")
            if (parts.size >= 2) {
                etStartTime.setText(parts[0].trim())
                etEndTime.setText(parts[1].trim())
            } else {
                etStartTime.setText(scheduleToEdit.time)
            }
            etLocation.setText(scheduleToEdit.location)
            spinnerDay.setSelection(days.indexOf(scheduleToEdit.day))
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
                    location = location
                )
                onSave(newSchedule)

            }
            .setNegativeButton(context.getString(R.string.btn_cancel), null)

        // Show Delete button (neutral) only if editing existing schedule
        if (scheduleToEdit != null) {
            builder.setNeutralButton(context.getString(R.string.btn_delete)) { dialog, _ ->
                // show confirmation
                AlertDialog.Builder(context)
                    .setTitle(context.getString(R.string.confirm_delete_title))
                    .setMessage(context.getString(R.string.confirm_delete_message))
                    .setPositiveButton(context.getString(R.string.btn_delete)) { _, _ ->
                        // perform delete via callback
                        onDelete(scheduleToEdit)
                    }
                    .setNegativeButton(context.getString(R.string.btn_cancel), null)
                    .show()
            }
        }

        val dialog = builder.show()

        // If delete button exists, color it with error color to emphasize destructive action
        if (scheduleToEdit != null) {
            val deleteBtn = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)
            try {
                val color = ContextCompat.getColor(context, com.google.android.material.R.color.m3_ref_palette_error50) // fallback to material error
                deleteBtn.setTextColor(color)
            } catch (e: Exception) {
                // ignore if color not found
            }
        }
    }
}
