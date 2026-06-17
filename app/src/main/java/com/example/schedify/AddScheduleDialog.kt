package com.example.schedify

import android.app.TimePickerDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import android.widget.*
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.util.*
import java.text.SimpleDateFormat

class AddScheduleDialog(
    private val context: Context,
    private val onSave: (Schedule) -> Unit,
    private val onDelete: (Schedule) -> Unit = {}
) {
    private var selectedColor: Int = 0xFF2563EB.toInt() // Default brand blue

    fun show(scheduleToEdit: Schedule? = null, preselectedDay: String? = null) {
        val dialog = BottomSheetDialog(context, R.style.AppBottomSheetDialogTheme)
        val view = View.inflate(context, R.layout.dialog_add_schedule, null)
        dialog.setContentView(view)

        val tvDialogTitle = view.findViewById<TextView>(R.id.tvDialogTitle)
        val headerDivider = view.findViewById<View>(R.id.headerDivider)
        val etTitle = view.findViewById<TextInputEditText>(R.id.etTitle)
        val etStartTime = view.findViewById<TextInputEditText>(R.id.etStartTime)
        val etEndTime = view.findViewById<TextInputEditText>(R.id.etEndTime)
        val etLocation = view.findViewById<TextInputEditText>(R.id.etLocation)
        val acDay = view.findViewById<AutoCompleteTextView>(R.id.acDay)
        val colorPickerContainer = view.findViewById<LinearLayout>(R.id.colorPickerContainer)
        val btnDelete = view.findViewById<MaterialButton>(R.id.btnDelete)
        val btnCancel = view.findViewById<MaterialButton>(R.id.btnCancel)
        val btnSave = view.findViewById<MaterialButton>(R.id.btnSave)

        // Setup colors
        val colors = listOf(
            0xFF2563EB.toInt(), // Brand Blue
            0xFF00C9B8.toInt(), // Brand Teal
            0xFFEF4444.toInt(), // Red
            0xFF10B981.toInt(), // Green
            0xFFF59E0B.toInt(), // Amber
            0xFF8B5CF6.toInt(), // Violet
            0xFFEC4899.toInt()  // Pink
        )

        selectedColor = scheduleToEdit?.color ?: colors[0]

        fun createColorCircle(color: Int): View {
            val density = context.resources.displayMetrics.density
            val size = (44 * density).toInt()
            val margin = (6 * density).toInt()
            
            val frame = FrameLayout(context)
            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins(margin, 0, margin, 0)
            frame.layoutParams = params

            val circle = View(context)
            val isSelected = color == selectedColor
            
            val circleSize = if (isSelected) (44 * density).toInt() else (36 * density).toInt()
            val circleParams = FrameLayout.LayoutParams(circleSize, circleSize)
            circleParams.gravity = android.view.Gravity.CENTER
            circle.layoutParams = circleParams
            
            val shape = GradientDrawable()
            shape.shape = GradientDrawable.OVAL
            shape.setColor(color)
            
            if (isSelected) {
                shape.setStroke((3 * density).toInt(), Color.WHITE)
            }
            
            circle.background = shape
            frame.addView(circle)
            
            // Add checkmark if selected
            if (isSelected) {
                val check = ImageView(context)
                val checkSize = (20 * density).toInt()
                val checkParams = FrameLayout.LayoutParams(checkSize, checkSize)
                checkParams.gravity = android.view.Gravity.CENTER
                check.layoutParams = checkParams
                check.setImageResource(android.R.drawable.checkbox_on_background)
                check.setColorFilter(Color.WHITE)
                frame.addView(check)
            }
            
            frame.setOnClickListener {
                selectedColor = color
                colorPickerContainer.removeAllViews()
                colors.forEach { c -> colorPickerContainer.addView(createColorCircle(c)) }
            }
            
            return frame
        }

        fun refreshColors() {
            colorPickerContainer.removeAllViews()
            colors.forEach { color ->
                colorPickerContainer.addView(createColorCircle(color))
            }
        }
        refreshColors()

        val days = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu", "Minggu")
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, days)
        acDay.setAdapter(adapter)

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

        if (scheduleToEdit != null) {
            tvDialogTitle.text = context.getString(R.string.title_edit_schedule)
            headerDivider.visibility = View.VISIBLE
            etTitle.setText(scheduleToEdit.title)
            val parts = scheduleToEdit.time.split(" – ")
            if (parts.size >= 2) {
                etStartTime.setText(parts[0].trim())
                etEndTime.setText(parts[1].trim())
            }
            etLocation.setText(scheduleToEdit.location)
            acDay.setText(scheduleToEdit.day, false)
            btnDelete.visibility = View.VISIBLE
        } else {
            preselectedDay?.let { pd -> acDay.setText(pd, false) }
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnDelete.setOnClickListener {
            MaterialAlertDialogBuilder(context)
                .setTitle(context.getString(R.string.confirm_delete_title))
                .setMessage(context.getString(R.string.confirm_delete_message))
                .setPositiveButton(context.getString(R.string.btn_delete)) { _, _ ->
                    onDelete(scheduleToEdit!!)
                    dialog.dismiss()
                }
                .setNegativeButton(context.getString(R.string.btn_cancel), null)
                .show()
        }

        btnSave.setOnClickListener {
            val title = etTitle.text.toString().trim()
            val start = etStartTime.text.toString().trim()
            val end = etEndTime.text.toString().trim()
            val location = etLocation.text.toString().trim()
            val day = acDay.text.toString()

            if (title.isEmpty() || start.isEmpty() || end.isEmpty()) {
                Toast.makeText(context, context.getString(R.string.msg_fill_fields), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val timeCombined = "$start – $end"
            onSave(Schedule(scheduleToEdit?.id ?: 0, title, timeCombined, day, location, selectedColor))
            dialog.dismiss()
        }

        // Apply gradient to Save button
        val gd = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, intArrayOf(0xFF2563EB.toInt(), 0xFF00C9B8.toInt()))
        gd.cornerRadius = 12 * context.resources.displayMetrics.density
        btnSave.background = gd

        dialog.show()
    }
}
