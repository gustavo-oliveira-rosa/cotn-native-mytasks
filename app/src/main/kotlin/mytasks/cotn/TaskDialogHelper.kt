package mytasks.cotn

import android.app.DatePickerDialog
import android.content.Context
import android.view.LayoutInflater
import android.widget.Button
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object TaskDialogHelper {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    fun showTaskDialog(
        context: Context,
        task: Task? = null,
        onSave: (String, Long?) -> Unit
    ) {
        val view = LayoutInflater.from(context).inflate(R.layout.dialog_task_input, null)
        val etTitle = view.findViewById<TextInputEditText>(R.id.etTaskTitle)
        val tilTitle = view.findViewById<TextInputLayout>(R.id.tilTaskTitle)
        val btnDate = view.findViewById<Button>(R.id.btnPickDate)

        var selectedDate: Long? = task?.dueDate
        
        fun updateDateButton() {
            btnDate.text = if (selectedDate != null) {
                dateFormat.format(Date(selectedDate!!))
            } else {
                context.getString(R.string.no_due_date)
            }
        }

        etTitle.setText(task?.title.orEmpty())
        updateDateButton()

        btnDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            selectedDate?.let { calendar.timeInMillis = it }

            DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    selectedDate = calendar.timeInMillis
                    updateDateButton()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).apply {
                setButton(DatePickerDialog.BUTTON_NEUTRAL, context.getString(R.string.no_due_date)) { _, _ ->
                    selectedDate = null
                    updateDateButton()
                }
                show()
            }
        }

        MaterialAlertDialogBuilder(context)
            .setTitle(if (task == null) R.string.new_task else R.string.edit_task)
            .setView(view)
            .setPositiveButton(if (task == null) R.string.add else R.string.save) { _, _ ->
                val title = etTitle.text.toString().trim()
                if (title.isNotEmpty()) {
                    onSave(title, selectedDate)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
