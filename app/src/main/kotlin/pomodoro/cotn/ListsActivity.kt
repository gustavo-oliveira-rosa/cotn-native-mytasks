package pomodoro.cotn

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import pomodoro.cotn.databinding.ActivityListsBinding
import pomodoro.cotn.databinding.ItemListBinding

class ListsActivity : BaseActivity() {

    private lateinit var binding: ActivityListsBinding
    private val lists = mutableListOf<TaskList>()
    private val gson = Gson()
    private lateinit var adapter: RecyclerView.Adapter<*>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        loadLists()
        setupRecyclerView()
        binding.fab.setOnClickListener { showListDialog() }
        updateEmptyState()
        if (!LanguageHelper.isSelected(this)) {
            LanguageHelper.showLanguagePicker(this)
        }
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
        updateEmptyState()
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
                    HelpStep(R.string.tour_list_1_title, R.string.tour_list_1_body, R.drawable.ic_playlist_add),
                    HelpStep(R.string.tour_list_2_title, R.string.tour_list_2_body, R.drawable.ic_edit),
                    HelpStep(R.string.tour_list_3_title, R.string.tour_list_3_body, R.drawable.ic_delete)
                )
            )
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun setupRecyclerView() {
        adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            inner class ListViewHolder(val b: ItemListBinding) : RecyclerView.ViewHolder(b.root)

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
                ListViewHolder(ItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false))

            override fun getItemCount() = lists.size

            override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
                val b = (holder as ListViewHolder).b
                val list = lists[position]
                b.tvListName.text = list.name
                b.tvTaskCount.text = taskCountText(list.id)
                animateItem(holder.itemView, position)
                val openList = {
                    startActivity(Intent(this@ListsActivity, MainActivity::class.java).apply {
                        putExtra("list_id", list.id)
                        putExtra("list_name", list.name)
                    })
                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out)
                }
                b.root.setOnClickListener { openList() }
                b.btnEdit.setOnClickListener { showListDialog(list, position) }
                b.btnDelete.setOnClickListener { deleteList(position) }
            }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter

        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {
            override fun onMove(rv: RecyclerView, vh: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder) = false
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                if (position != RecyclerView.NO_POSITION) deleteList(position)
            }
        }).attachToRecyclerView(binding.recyclerView)
    }

    private fun animateItem(view: View, position: Int) {
        view.translationY = 50f
        view.alpha = 0f
        AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "translationY", 50f, 0f),
                ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
            )
            duration = 280
            startDelay = (position * 50L).coerceAtMost(300L)
            interpolator = DecelerateInterpolator()
            start()
        }
    }

    private fun taskCountText(listId: String): String {
        val count = loadAllTasks().count { it.listId == listId && !it.isDone }
        return if (count > 0) resources.getQuantityString(R.plurals.task_pending_count, count, count) else ""
    }

    private fun showListDialog(list: TaskList? = null, position: Int = -1) {
        showInputDialog(
            context = this,
            titleRes = if (list == null) R.string.new_list else R.string.edit_list,
            hint = getString(R.string.list_hint),
            initialText = list?.name.orEmpty(),
            positiveRes = if (list == null) R.string.add else R.string.save
        ) { name ->
            if (list == null) {
                lists.add(TaskList(name = name))
                adapter.notifyItemInserted(lists.size - 1)
                Toast.makeText(this, R.string.toast_list_created, Toast.LENGTH_SHORT).show()
            } else {
                lists[position] = list.copy(name = name)
                adapter.notifyItemChanged(position)
                Toast.makeText(this, R.string.toast_list_updated, Toast.LENGTH_SHORT).show()
            }
            saveLists()
            updateEmptyState()
        }
    }

    private fun deleteList(position: Int) {
        val removed = lists.removeAt(position)
        adapter.notifyItemRemoved(position)
        val removedTasks = loadAllTasks().filter { it.listId == removed.id }
        saveLists()
        saveTasks(loadAllTasks() - removedTasks.toSet())
        updateEmptyState()
        Snackbar.make(binding.root, R.string.list_deleted, Snackbar.LENGTH_LONG)
            .setAction(R.string.undo) {
                lists.add(position, removed)
                adapter.notifyItemInserted(position)
                saveLists()
                val restored = loadAllTasks().toMutableList().also { it.addAll(removedTasks) }
                saveTasks(restored)
                updateEmptyState()
                Toast.makeText(this, R.string.toast_restored, Toast.LENGTH_SHORT).show()
            }.show()
    }

    private fun updateEmptyState() {
        binding.tvEmpty.isVisible = lists.isEmpty()
    }

    private fun saveLists() {
        getSharedPreferences("mytasks", MODE_PRIVATE).edit().putString("lists", gson.toJson(lists)).apply()
    }

    private fun saveTasks(all: List<Task>) {
        getSharedPreferences("mytasks", MODE_PRIVATE).edit().putString("tasks", gson.toJson(all)).apply()
    }

    private fun loadLists() {
        val json = getSharedPreferences("mytasks", MODE_PRIVATE).getString("lists", null) ?: return
        val type = object : TypeToken<MutableList<TaskList>>() {}.type
        lists.addAll(gson.fromJson(json, type))
    }

    private fun loadAllTasks(): List<Task> {
        val json = getSharedPreferences("mytasks", MODE_PRIVATE).getString("tasks", null) ?: return emptyList()
        val type = object : TypeToken<MutableList<Task>>() {}.type
        return gson.fromJson(json, type)
    }
}
