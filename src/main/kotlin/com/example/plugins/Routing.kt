package com.example.plugins

import com.example.model.Priority
import com.example.model.Task
import com.example.model.TaskRepository
import com.example.model.tasksAsTable
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.JsonConvertException
import io.ktor.server.application.*
import io.ktor.server.http.content.staticResources
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.request.receiveParameters
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    routing { // 页面路由
        get("/") { // 首页
            call.respondText("Hello World!") // 响应字符串文本
        }
        get("/test1") { // 测试页1
            call.respondText("<h1>OK</h1>", ContentType.parse("text/html")) // 响应THML文本
        }
        staticResources(
            "/content",
            "mycontent"
        ) // 静态页面，第二个参数是资源目录，加载目录中的资源来显示静态页面(http://0.0.0.0:9292/content/sample.html)
        get("/error-test") { // 触发错误的页面，导致下面install(StatusPages)中的状态页显示
            throw IllegalStateException("Test error") // 触发IllegalStateException异常，对应状态页的异常处理
        }
        get("/tasks") {
            call.respondText(
                contentType = ContentType.parse("text/html"),
                text = TaskRepository.allTasks().tasksAsTable()
            )
        }
        get("/tasks/byPriority/{priority}") { // ${priority}是路径参数
            val priorityAsText = call.parameters["priority"] // 获取路径参数
            if (priorityAsText == null) {
                call.respond(HttpStatusCode.BadRequest)
                return@get
            }
            try {
                val priority = Priority.valueOf(priorityAsText)
                val tasks = TaskRepository.tasksByPriority(priority)
                if (tasks.isEmpty()) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                call.respondText(
                    contentType = ContentType.parse("text/html"),
                    text = tasks.tasksAsTable()
                )
            } catch(ex: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest)
            }
        }
        staticResources("/task-ui", "task-ui")
        post("/tasks") {
            val formContent = call.receiveParameters() // 获取表单参数
            val params = Triple(
                formContent["name"] ?: "",
                formContent["description"] ?: "",
                formContent["priority"] ?: ""
            ) // 获取表单参数内容，三个参数可以用Triple来获取
            if (params.toList().any { it.isEmpty() }) { // 判空
                call.respond(HttpStatusCode.BadRequest)
                return@post
            }
            try {
                val priority = Priority.valueOf(params.third)
                TaskRepository.addTask(
                    Task(
                        params.first,
                        params.second,
                        priority
                    )
                ) // 添加任务
                call.respond(HttpStatusCode.NoContent)
            } catch (ex: IllegalArgumentException) {
                call.respond(HttpStatusCode.BadRequest)
            } catch (ex: IllegalStateException) {
                call.respond(HttpStatusCode.BadRequest)
            }
        }
        route("/tasks") { // 子路由，类似文件夹
            get { // 等于31行的get("/tasks")路由
                // ...
            }
            post { // 等于59行的post路由
                // ...
            }
            get("/byName/{taskName}") {
                val name = call.parameters["taskName"]
                if (name == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@get
                }

                val task = TaskRepository.taskByName(name)
                if (task == null) {
                    call.respond(HttpStatusCode.NotFound)
                    return@get
                }
                call.respond(task)
            }
            post("/addTask") {
                try {
                    val task = call.receive<Task>()
                    TaskRepository.addTask(task)
                    call.respond(HttpStatusCode.NoContent)
                } catch (ex: IllegalStateException) {
                    call.respond(HttpStatusCode.BadRequest)
                } catch (ex: JsonConvertException) {
                    call.respond(HttpStatusCode.BadRequest)
                }
            }
            delete("/{taskName}") {
                val name = call.parameters["taskName"]
                if (name == null) {
                    call.respond(HttpStatusCode.BadRequest)
                    return@delete
                }

                if (TaskRepository.removeTask(name)) {
                    call.respond(HttpStatusCode.NoContent)
                } else {
                    call.respond(HttpStatusCode.NotFound)
                }
            }
        }
        staticResources("static", "static")
        get("/tasks-json") {
            call.respond(
                TaskRepository.allTasks()
            )
        }
    }

    install(StatusPages) { // 添加错误处理的状态页
        exception<IllegalStateException> { call, cause ->
            call.respondText("App in illegal state as ${cause.message}")
        }
    }
}
