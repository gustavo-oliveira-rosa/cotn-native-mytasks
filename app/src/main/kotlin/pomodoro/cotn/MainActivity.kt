package pomodoro.cotn

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import pomodoro.cotn.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {

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
        updateEmptyAndProgress()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_help, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_help) {
            showHelpTour(
                this,
                listOf(
                    HelpStep(R.string.tour_task_1_title, R.string.tour_task_1_body, R.drawable.ic_add_circle),
                    HelpStep(R.string.tour_task_2_title, R.string.tour_task_2_body, R.drawable.ic_check_circle),
                    HelpStep(R.string.tour_task_3_title, R.string.tour_task_3_body, R.drawable.ic_delete),
                    HelpStep(R.string.tour_task_4_title, R.string.tour_task_4_body, R.drawable.ic_poll)
                )
            )
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupRecyclerView() {
        adapter = TaskAdapter(
            tasks,
            onToggle = { task -> toggleTask(task) },
            onEdit = { task, position -> showTaskDialog(task, position) },
            onDelete = { task, position -> deleteTask(position) }
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) deleteTask(position)
            }
        }).attachToRecyclerView(binding.recyclerView)
    }

    private fun toggleTask(task: Task) {
        val all = loadAllTasks()
        val i = all.indexOfFirst { it.id == task.id }
        if (i != -1) {
            all[i] = task.copy(isDone = !task.isDone)
            saveAllTasks(all)
            val li = tasks.indexOfFirst { it.id == task.id }
            if (li != -1) {
                tasks[li] = all[i]
                adapter.notifyItemChanged(li)
            }
            updateEmptyAndProgress()
        }
    }

    private fun deleteTask(position: Int) {
        if (position < 0 || position >= tasks.size) {
            adapter.notifyDataSetChanged()
            return
        }
        val task = tasks.removeAt(position)
        adapter.notifyItemRemoved(position)
        val all = loadAllTasks().also { it.removeAll { t -> t.id == task.id } }
        saveAllTasks(all)
        updateEmptyAndProgress()
        Snackbar.make(binding.root, R.string.task_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) {
                tasks.add(position, task)
                adapter.notifyItemInserted(position)
                val restored = loadAllTasks().toMutableList().also { it.add(task) }
                saveAllTasks(restored)
                updateEmptyAndProgress()
                Toast.makeText(this, R.string.toast_restored, Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun showTaskDialog(task: Task? = null, position: Int = -1) {
        showInputDialog(
            context = this,
            titleRes = if (task == null) R.string.new_task else R.string.edit_task,
            hint = getString(R.string.task_hint),
            initialText = task?.title.orEmpty(),
            positiveRes = if (task == null) R.string.add else R.string.save
        ) { title ->
            if (task == null) {
                val newTask = Task(listId = listId, title = title)
                tasks.add(newTask)
                adapter.notifyItemInserted(tasks.size - 1)
                val all = loadAllTasks().also { it.add(newTask) }
                saveAllTasks(all)
                Toast.makeText(this, R.string.toast_task_created, Toast.LENGTH_SHORT).show()
            } else {
                val updated = task.copy(title = title)
                tasks[position] = updated
                adapter.notifyItemChanged(position)
                val all = loadAllTasks()
                val i = all.indexOfFirst { it.id == task.id }
                if (i != -1) {
                    all[i] = updated
                    saveAllTasks(all)
                }
                Toast.makeText(this, R.string.toast_task_updated, Toast.LENGTH_SHORT).show()
            }
            updateEmptyAndProgress()
        }
    }

    private fun updateEmptyAndProgress() {
        binding.tvEmpty.isVisible = tasks.isEmpty()
        binding.tvProgress.isVisible = tasks.isNotEmpty()
        binding.tvProgress.text =
            getString(R.string.progress_concluded, tasks.count { it.isDone }, tasks.size)
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