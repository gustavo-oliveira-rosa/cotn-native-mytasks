package pomodoro.cotn

import java.util.UUID

data class TaskList(
    val id: String = UUID.randomUUID().toString(),
    val name: String
)
