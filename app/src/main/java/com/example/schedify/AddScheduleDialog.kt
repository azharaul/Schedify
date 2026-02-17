package com.example.schedify

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AddScheduleDialog(
    private val context: Context,
    private val onSave: (Schedule) -> Unit
) {
    fun show(scheduleToEdit: Schedule? = null) {
        val inflater = android.view.LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dialog_add_schedule, null)

        val etTitle = view.findViewById<EditText>(R.id.etTitle)
        val etTime = view.findViewById<EditText>(R.id.etTime)
        val spinnerDay = view.findViewById<Spinner>(R.id.spinnerDay)

        // Setup spinner
        val days = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
        val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, days)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDay.adapter = adapter

        // Jika edit, isi data yang ada
        if (scheduleToEdit != null) {
            etTitle.setText(scheduleToEdit.title)
            etTime.setText(scheduleToEdit.time)
            spinnerDay.setSelection(days.indexOf(scheduleToEdit.day))
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(if (scheduleToEdit != null) "Edit Jadwal" else "Tambah Jadwal Baru")
            .setView(view)
            .setPositiveButton("Simpan") { _, _ ->
                val title = etTitle.text.toString().trim()
                val time = etTime.text.toString().trim()
                val day = spinnerDay.selectedItem.toString()

                if (title.isNotEmpty() && time.isNotEmpty()) {
                    val newSchedule = Schedule(
                        id = scheduleToEdit?.id ?: 0,
                        title = title,
                        time = time,
                        day = day
                    )
                    onSave(newSchedule)
                } else {
                    Toast.makeText(context, "Isi semua field", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }
}



