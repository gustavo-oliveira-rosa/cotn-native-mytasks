package mytasks.cotn

import java.util.UUID

data class Task(
    val id: String = UUID.randomUUID().toString(),
    val listId: String,
    val title: String,
    val isDone: Boolean = false,
    val dueDate: Long? = null
)
