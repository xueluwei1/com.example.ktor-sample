package com.example.plugins

import com.example.model.Priority
import com.example.model.Task
import com.example.model.TaskRepository
import io.ktor.serialization.kotlinx.KotlinxWebsocketSerializationConverter
import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import java.time.Duration
import java.util.Collections

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
        contentConverter = KotlinxWebsocketSerializationConverter(Json)
    }
    routing {
        route("/ws") {
            webSocket("/tasks") {
                sendAllTasks()
                close(CloseReason(CloseReason.Codes.NORMAL, "All done"))
            }
            val sessions = Collections.synchronizedList<WebSocketServerSession>(ArrayList())
            webSocket("/tasks2") {
                sessions.add(this)
                sendAllTasks()
                while(true) {
                    val newTask = receiveDeserialized<Task>()
                    TaskRepository.addTask(newTask)
                    for(session in sessions) {
                        session.sendSerialized(newTask)
                    }
                }
            }
        }
    }
}

private suspend fun DefaultWebSocketServerSession.sendAllTasks() {
    for (task in TaskRepository.allTasks()) {
        sendSerialized(task)
        delay(1000)
    }
}