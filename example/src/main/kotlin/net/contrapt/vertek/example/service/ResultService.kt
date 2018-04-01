package net.contrapt.vertek.example.service

import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory

interface ResultService {

    val logger: Logger
    val foo : String

    fun doSomething() {
        logger.info("In the service doing something")
    }

    class Impl : ResultService {
        override val logger = LoggerFactory.getLogger(javaClass)
        override lateinit var foo: String
    }
}