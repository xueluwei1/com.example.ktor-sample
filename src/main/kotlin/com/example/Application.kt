package com.example

import com.example.model.FakeTaskRepository
import com.example.model.PostgresTaskRepository
import com.example.plugins.*
import io.ktor.server.application.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
//    val repository = FakeTaskRepository()
    val repository = PostgresTaskRepository()

    configureRouting() // 配置路由
    configureSerialization() // 配置序列化
    configureTemplating() // 配置thymeleaf模板
    configureSockets() // 配置websockets
    configureDatabases(environment.config, repository) // 配置数据库
}
