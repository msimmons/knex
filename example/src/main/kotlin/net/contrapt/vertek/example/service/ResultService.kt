package net.contrapt.vertek.example.service

import io.vertx.core.logging.Logger
import io.vertx.core.logging.LoggerFactory
import net.contrapt.vertek.example.model.User
import net.contrapt.vertek.example.repository.UserRepository

class ResultService(val userRepository: UserRepository) {

    val logger: Logger = LoggerFactory.getLogger(javaClass)
    val foo : String = ""

    fun doSomething() : User {
        logger.info("In the service doing something")
        val user = User()
        userRepository.insert(user)
        return user
    }

}