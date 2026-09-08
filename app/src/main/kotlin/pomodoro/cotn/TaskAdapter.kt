package pomodoro.cotn

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.recyclerview.widget.RecyclerView
import pomodoro.cotn.databinding.ItemTaskBinding

class TaskAdapter(
    private val tasks: MutableList<Task>,
    private val onToggle: (Task) -> Unit,
    private val onEdit: (Task, Int) -> Unit,
    private val onDelete: (Task, Int) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(val binding: ItemTaskBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val binding = ItemTaskBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return TaskViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = tasks[position]
        holder.binding.cbTask.setOnCheckedChangeListener(null)
        holder.binding.cbTask.isChecked = task.isDone
        holder.binding.tvTaskTitle.text = task.title
        holder.binding.tvTaskTitle.paintFlags = if (task.isDone)
            holder.binding.tvTaskTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        else
            holder.binding.tvTaskTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        holder.binding.tvTaskTitle.alpha = if (task.isDone) 0.5f else 1f
        holder.binding.cbTask.alpha = if (task.isDone) 0.6f else 1f

        animateItem(holder)

        holder.binding.cbTask.setOnCheckedChangeListener { _, _ -> onToggle(task) }
        holder.binding.btnEdit.setOnClickListener { onEdit(task, holder.adapterPosition) }
        holder.binding.btnDelete.setOnClickListener { onDelete(task, holder.adapterPosition) }
    }

    private fun animateItem(holder: TaskViewHolder) {
        val view = holder.itemView
        view.translationX = 60f
        view.alpha = 0f
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "translationX", 60f, 0f),
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
            )
            duration = 250
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    override fun getItemCount() = tasks.size

    fun removeAt(position: Int): Task {
        val removed = tasks.removeAt(position)
        notifyItemRemoved(position)
        return removed
    }
}
