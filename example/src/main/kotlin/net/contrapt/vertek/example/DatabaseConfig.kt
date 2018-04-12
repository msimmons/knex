package net.contrapt.vertek.example

import io.vertx.core.Vertx
import org.springframework.context.ApplicationContext
import org.springframework.context.support.beans

object DatabaseConfig {

    fun startup(vertx: Vertx, context: ApplicationContext) {
    }

    fun context() = beans {
    }
}