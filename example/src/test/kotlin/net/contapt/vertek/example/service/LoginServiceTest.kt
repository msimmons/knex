package net.contapt.vertek.example.service

import com.mongodb.client.MongoDatabase
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldNotBe
import net.contrapt.vertek.example.model.User
import net.contrapt.vertek.example.service.LoginService
import org.junit.Test
import java.util.*

class LoginServiceTest : AbstractServiceTest() {

    @Test
    fun testLoginSuccess() {
        val mongoDb = autowire<MongoDatabase>()
        mongoDb.getCollection("users", User::class.java).insertOne(User("mark@contrapt.net", "foobar", "mark", UUID.randomUUID().toString()))
        val loginService = autowire<LoginService>()
        val result = loginService(LoginService.Request("mark@contrapt.net", "foobar"))
        result.token shouldNotBe null
        result.profileId shouldNotBe null
        result.error shouldBe null
        println(result)
    }

    @Test
    fun testLoginFailure() {
        val mongoDb = autowire<MongoDatabase>()
        mongoDb.getCollection("users", User::class.java).insertOne(User("mark@contrapt.net", "foobar", "mark", UUID.randomUUID().toString()))
        val loginService = autowire<LoginService>()
        val result = loginService(LoginService.Request("mark@contrapt.net", "baz"))
        result.token shouldBe null
        result.profileId shouldBe null
        result.error shouldNotBe null
        println(result)
    }
}