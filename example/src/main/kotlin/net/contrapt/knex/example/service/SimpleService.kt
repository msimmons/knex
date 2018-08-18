package net.contrapt.knex.example.service

import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory

interface SimpleService {

    val logger: Logger
    val foo : String

    fun doSomething() {
        logger.info("In the service doing something")
    }

    class Impl : SimpleService {
        override val logger = LoggerFactory.getLogger(javaClass)
        override lateinit var foo: String
    }
}