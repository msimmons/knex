package net.contapt.knex.example.service

import io.kotlintest.matchers.shouldNotBe
import net.contrapt.knex.example.repository.UserRepository
import net.contrapt.knex.example.service.LoginService
import org.junit.Test

class LoginServiceTest : AbstractServiceTest() {

    @Test
    fun testLoginSuccess() {
        val loginService = autowire<LoginService>()
        val repo = autowire<UserRepository>()
        val response = loginService(LoginService.Request("foo", "bar"))
        response.profileId shouldNotBe null
    }

    @Test
    fun testLoginFailure() {
    }
}