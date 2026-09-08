package pomodoro.cotn

import android.content.Context
import android.content.DialogInterface
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlin.math.roundToInt

fun Context.dp(value: Int): Int = (value * resources.displayMetrics.density).roundToInt()

fun showInputDialog(
    context: Context,
    titleRes: Int,
    hint: String,
    initialText: String,
    positiveRes: Int,
    onPositive: (String) -> Unit
) {
    val inputLayout = TextInputLayout(context).apply {
        this.hint = hint
        setPadding(context.dp(24), context.dp(8), context.dp(24), 0)
    }
    val input = TextInputEditText(context).apply {
        setSingleLine(true)
        imeOptions = EditorInfo.IME_ACTION_DONE
        if (initialText.isNotBlank()) setText(initialText)
    }
    inputLayout.addView(input)

    val dialog = MaterialAlertDialogBuilder(context)
        .setTitle(titleRes)
        .setView(inputLayout)
        .setPositiveButton(positiveRes, null)
        .setNegativeButton(R.string.cancel, null)
        .create()

    dialog.setOnShowListener {
        val positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        positive.isEnabled = initialText.isNotBlank()
        positive.setOnClickListener {
            val text = input.text?.toString()?.trim().orEmpty()
            if (text.isEmpty()) {
                inputLayout.error = context.getString(R.string.error_required)
            } else {
                onPositive(text)
                dialog.dismiss()
            }
        }
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                positive.isEnabled = !s.toString().trim().isEmpty()
                if (positive.isEnabled) inputLayout.error = null
            }
        })
    }
    dialog.show()
}