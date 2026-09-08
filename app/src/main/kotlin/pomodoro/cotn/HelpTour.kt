package pomodoro.cotn

import android.content.Context
import android.content.DialogInterface
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.util.Locale

data class HelpStep(val titleRes: Int, val bodyRes: Int, val iconRes: Int)

fun showHelpTour(context: Context, steps: List<HelpStep>) {
    if (steps.isEmpty()) return

    val root = LayoutInflater.from(context).inflate(R.layout.dialog_help, null)
    val tvStep = root.findViewById<TextView>(R.id.tvHelpStep)
    val progress = root.findViewById<LinearProgressIndicator>(R.id.helpProgress)
    val ivIcon = root.findViewById<ImageView>(R.id.ivHelpIcon)
    val tvTitle = root.findViewById<TextView>(R.id.tvHelpTitle)
    val tvBody = root.findViewById<TextView>(R.id.tvHelpBody)

    val dialog: AlertDialog = MaterialAlertDialogBuilder(context)
        .setView(root)
        .setNegativeButton(R.string.help_skip, null)
        .setPositiveButton(R.string.help_next, null)
        .create()

    var index = 0
    progress.max = steps.size

    fun render() {
        tvStep.text = context.getString(R.string.help_step_of, index + 1, steps.size)
            .uppercase(Locale.getDefault())
        progress.setProgressCompat(index + 1, true)
        ivIcon.setImageResource(steps[index].iconRes)
        tvTitle.text = context.getString(steps[index].titleRes)
        tvBody.text = context.getString(steps[index].bodyRes)
    }

    dialog.setOnShowListener {
        val positive = dialog.getButton(DialogInterface.BUTTON_POSITIVE)
        val negative = dialog.getButton(DialogInterface.BUTTON_NEGATIVE)
        render()
        negative.setOnClickListener { dialog.dismiss() }
        positive.setOnClickListener {
            index++
            if (index < steps.size) {
                render()
                positive.text = if (index == steps.size - 1)
                    context.getString(R.string.help_done)
                else
                    context.getString(R.string.help_next)
            } else {
                dialog.dismiss()
            }
        }
    }
    dialog.show()
}