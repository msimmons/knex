package net.contrapt.knex.example

import net.contrapt.knex.example.service.*
import org.springframework.context.support.beans

object ServiceConfig {

    fun context() = beans {
        bean() { SimpleService.Impl() }
        bean() { ResultService(ref()) }
        bean() { SignupService(ref()) }
        bean() { VerifyService(ref()) }
        bean() { LoginService(ref()) }
    }
}