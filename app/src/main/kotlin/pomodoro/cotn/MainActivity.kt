package pomodoro.cotn

import android.os.Bundle
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import pomodoro.cotn.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: TaskAdapter
    private val tasks = mutableListOf<Task>()
    private val gson = Gson()
    private lateinit var listId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listId = intent.getStringExtra("list_id") ?: ""
        binding.toolbar.title = intent.getStringExtra("list_name") ?: ""
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        loadTasks()
        setupRecyclerView()
        binding.fab.setOnClickListener { showTaskDialog() }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun setupRecyclerView() {
        adapter = TaskAdapter(
            tasks,
            onToggle = { task ->
                val all = loadAllTasks()
                val i = all.indexOfFirst { it.id == task.id }
                if (i != -1) {
                    all[i] = task.copy(isDone = !task.isDone)
                    saveAllTasks(all)
                    val li = tasks.indexOfFirst { it.id == task.id }
                    if (li != -1) { tasks[li] = all[i]; adapter.notifyItemChanged(li) }
                }
            },
            onEdit = { task, position -> showTaskDialog(task, position) },
            onDelete = { task, position ->
                tasks.removeAt(position)
                adapter.notifyItemRemoved(position)
                val all = loadAllTasks().also { it.removeAll { t -> t.id == task.id } }
                saveAllTasks(all)
                Snackbar.make(binding.root, R.string.task_deleted, Snackbar.LENGTH_LONG)
                    .setAction(R.string.undo) {
                        tasks.add(position, task)
                        adapter.notifyItemInserted(position)
                        val restored = loadAllTasks().toMutableList().also { it.add(task) }
                        saveAllTasks(restored)
                    }.show()
            }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun showTaskDialog(task: Task? = null, position: Int = -1) {
        val input = EditText(this).apply {
            hint = getString(R.string.task_hint)
            setPadding(48, 32, 48, 32)
            task?.let { setText(it.title) }
        }
        AlertDialog.Builder(this)
            .setTitle(if (task == null) R.string.new_task else R.string.edit_task)
            .setView(input)
            .setPositiveButton(if (task == null) R.string.add else R.string.save) { _, _ ->
                val title = input.text.toString().trim()
                if (title.isEmpty()) return@setPositiveButton
                if (task == null) {
                    val newTask = Task(listId = listId, title = title)
                    tasks.add(newTask)
                    adapter.notifyItemInserted(tasks.size - 1)
                    val all = loadAllTasks().also { it.add(newTask) }
                    saveAllTasks(all)
                } else {
                    val updated = task.copy(title = title)
                    tasks[position] = updated
                    adapter.notifyItemChanged(position)
                    val all = loadAllTasks()
                    val i = all.indexOfFirst { it.id == task.id }
                    if (i != -1) { all[i] = updated; saveAllTasks(all) }
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun loadAllTasks(): MutableList<Task> {
        val json = getSharedPreferences("mytasks", MODE_PRIVATE).getString("tasks", null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<Task>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun saveAllTasks(all: List<Task>) {
        getSharedPreferences("mytasks", MODE_PRIVATE).edit().putString("tasks", gson.toJson(all)).apply()
    }

    private fun loadTasks() {
        tasks.addAll(loadAllTasks().filter { it.listId == listId })
    }
}
