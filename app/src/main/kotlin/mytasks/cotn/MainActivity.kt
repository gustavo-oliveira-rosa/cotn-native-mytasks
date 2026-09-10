package mytasks.cotn

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
import mytasks.cotn.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: TaskAdapter
    private val allTasksForList = mutableListOf<Task>()
    private val displayedTasks = mutableListOf<Task>()
    private val gson = Gson()
    private lateinit var listId: String
    private var currentFilter = FilterMode.ALL

    enum class FilterMode { ALL, PENDING, COMPLETED, SORT_DATE }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listId = intent.getStringExtra("list_id") ?: ""
        binding.toolbar.title = intent.getStringExtra("list_name") ?: ""
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        setupFilterChips()
        loadTasks()
        binding.fab.setOnClickListener { showTaskDialog() }
        updateEmptyAndProgress()
    }

    private fun setupFilterChips() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when (checkedIds.firstOrNull()) {
                R.id.chipPending -> FilterMode.PENDING
                R.id.chipCompleted -> FilterMode.COMPLETED
                R.id.chipSortDate -> FilterMode.SORT_DATE
                else -> FilterMode.ALL
            }
            applyFilter()
        }
    }

    private fun applyFilter() {
        displayedTasks.clear()
        val filtered = when (currentFilter) {
            FilterMode.ALL -> allTasksForList
            FilterMode.PENDING -> allTasksForList.filter { !it.isDone }
            FilterMode.COMPLETED -> allTasksForList.filter { it.isDone }
            FilterMode.SORT_DATE -> allTasksForList.sortedBy { it.dueDate ?: Long.MAX_VALUE }
        }
        displayedTasks.addAll(filtered)
        adapter.notifyDataSetChanged()
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

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_language)?.title = LanguageHelper.getFlagEmoji(this)
        return super.onPrepareOptionsMenu(menu)
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
        if (item.itemId == R.id.action_language) {
            LanguageHelper.showLanguagePicker(this)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupRecyclerView() {
        adapter = TaskAdapter(
            displayedTasks,
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
        val all = loadAllTasksFromStorage()
        val i = all.indexOfFirst { it.id == task.id }
        if (i != -1) {
            val updated = all[i].copy(isDone = !all[i].isDone)
            all[i] = updated
            saveAllTasksToStorage(all)
            
            val localIndex = allTasksForList.indexOfFirst { it.id == task.id }
            if (localIndex != -1) allTasksForList[localIndex] = updated
            
            applyFilter()
        }
    }

    private fun deleteTask(position: Int) {
        if (position < 0 || position >= displayedTasks.size) return
        
        val task = displayedTasks[position]
        val all = loadAllTasksFromStorage().also { it.removeAll { t -> t.id == task.id } }
        saveAllTasksToStorage(all)
        
        allTasksForList.removeAll { it.id == task.id }
        applyFilter()
        
        Snackbar.make(binding.root, R.string.task_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) {
                val restored = loadAllTasksFromStorage().toMutableList().also { it.add(task) }
                saveAllTasksToStorage(restored)
                allTasksForList.add(task)
                applyFilter()
                Toast.makeText(this, R.string.toast_restored, Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun showTaskDialog(task: Task? = null, position: Int = -1) {
        TaskDialogHelper.showTaskDialog(this, task) { title, dueDate ->
            if (task == null) {
                val newTask = Task(listId = listId, title = title, dueDate = dueDate)
                allTasksForList.add(newTask)
                val all = loadAllTasksFromStorage().also { it.add(newTask) }
                saveAllTasksToStorage(all)
                Toast.makeText(this, R.string.toast_task_created, Toast.LENGTH_SHORT).show()
            } else {
                val updated = task.copy(title = title, dueDate = dueDate)
                val localIndex = allTasksForList.indexOfFirst { it.id == task.id }
                if (localIndex != -1) allTasksForList[localIndex] = updated
                
                val all = loadAllTasksFromStorage()
                val i = all.indexOfFirst { it.id == task.id }
                if (i != -1) {
                    all[i] = updated
                    saveAllTasksToStorage(all)
                }
                Toast.makeText(this, R.string.toast_task_updated, Toast.LENGTH_SHORT).show()
            }
            applyFilter()
        }
    }

    private fun updateEmptyAndProgress() {
        binding.tvEmpty.isVisible = displayedTasks.isEmpty()
        binding.tvProgress.isVisible = allTasksForList.isNotEmpty()
        binding.tvProgress.text =
            getString(R.string.progress_concluded, allTasksForList.count { it.isDone }, allTasksForList.size)
    }

    private fun loadAllTasksFromStorage(): MutableList<Task> {
        val json = getSharedPreferences("mytasks", MODE_PRIVATE).getString("tasks", null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<Task>>() {}.type
        return gson.fromJson(json, type)
    }

    private fun saveAllTasksToStorage(all: List<Task>) {
        getSharedPreferences("mytasks", MODE_PRIVATE).edit().putString("tasks", gson.toJson(all)).apply()
    }

    private fun loadTasks() {
        allTasksForList.clear()
        allTasksForList.addAll(loadAllTasksFromStorage().filter { it.listId == listId })
        applyFilter()
    }
}
