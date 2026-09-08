package pomodoro.cotn

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import pomodoro.cotn.databinding.ActivityListsBinding
import pomodoro.cotn.databinding.ItemListBinding

class ListsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListsBinding
    private val lists = mutableListOf<TaskList>()
    private val gson = Gson()
    private lateinit var adapter: RecyclerView.Adapter<*>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadLists()
        setupRecyclerView()
        binding.fab.setOnClickListener { showListDialog() }
    }

    override fun onResume() {
        super.onResume()
        adapter.notifyDataSetChanged()
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
                val openList = {
                    startActivity(Intent(this@ListsActivity, MainActivity::class.java).apply {
                        putExtra("list_id", list.id)
                        putExtra("list_name", list.name)
                    })
                }
                b.root.setOnClickListener { openList() }
                b.btnView.setOnClickListener { openList() }
                b.btnEdit.setOnClickListener { showListDialog(list, position) }
                b.btnDelete.setOnClickListener {
                    AlertDialog.Builder(this@ListsActivity)
                        .setTitle(R.string.delete_list)
                        .setMessage(getString(R.string.delete_list_confirm, list.name))
                        .setPositiveButton(R.string.delete) { _, _ ->
                            lists.removeAt(position)
                            notifyItemRemoved(position)
                            saveLists()
                        }
                        .setNegativeButton(R.string.cancel, null)
                        .show()
                }
            }
        }

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun taskCountText(listId: String): String {
        val json = getSharedPreferences("mytasks", MODE_PRIVATE).getString("tasks", null) ?: return ""
        val type = object : TypeToken<MutableList<Task>>() {}.type
        val all: List<Task> = gson.fromJson(json, type)
        val count = all.count { it.listId == listId && !it.isDone }
        return if (count > 0) "$count tarefa(s)" else ""
    }

    private fun showListDialog(list: TaskList? = null, position: Int = -1) {
        val input = EditText(this).apply {
            hint = getString(R.string.list_hint)
            setPadding(48, 32, 48, 32)
            list?.let { setText(it.name) }
        }
        AlertDialog.Builder(this)
            .setTitle(if (list == null) R.string.new_list else R.string.edit_list)
            .setView(input)
            .setPositiveButton(if (list == null) R.string.add else R.string.save) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton
                if (list == null) {
                    lists.add(TaskList(name = name))
                    adapter.notifyItemInserted(lists.size - 1)
                } else {
                    lists[position] = list.copy(name = name)
                    adapter.notifyItemChanged(position)
                }
                saveLists()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun saveLists() {
        getSharedPreferences("mytasks", MODE_PRIVATE).edit().putString("lists", gson.toJson(lists)).apply()
    }

    private fun loadLists() {
        val json = getSharedPreferences("mytasks", MODE_PRIVATE).getString("lists", null) ?: return
        val type = object : TypeToken<MutableList<TaskList>>() {}.type
        lists.addAll(gson.fromJson(json, type))
    }
}
