package pomodoro.cotn

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
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

        holder.binding.cbTask.setOnCheckedChangeListener { _, _ -> onToggle(task) }
        holder.binding.btnEdit.setOnClickListener { onEdit(task, holder.adapterPosition) }
        holder.binding.btnDelete.setOnClickListener { onDelete(task, holder.adapterPosition) }
    }

    override fun getItemCount() = tasks.size

    fun removeAt(position: Int): Task {
        val removed = tasks.removeAt(position)
        notifyItemRemoved(position)
        return removed
    }
}
