package net.contapt.vertek.example.service

import com.mongodb.client.MongoDatabase
import io.kotlintest.matchers.shouldBe
import io.kotlintest.matchers.shouldNotBe
import net.contrapt.vertek.example.model.User
import net.contrapt.vertek.example.service.SignupService
import org.bson.Document
import org.junit.Test

class SignupServiceTest : AbstractServiceTest() {

    @Test
    fun testSignupSuccess() {
        val users = autowire<MongoDatabase>().getCollection("users", User::class.java)
        val signupService = autowire<SignupService>()
        val result = signupService(SignupService.Request("mark@contrapt.net", "foobar", "mark"))
        result.user shouldNotBe null
        result.error shouldBe null
        val user = users.find(Document().append("email", "mark@contrapt.net")).first()
        user.firstName shouldBe "mark"
        user.email shouldBe "mark@contrapt.net"
        user.password shouldBe "foobar"
    }

    @Test
    fun testSignupFailure() {
        val signupService = autowire<SignupService>()
        val succeed = signupService(SignupService.Request("mark@contrapt.net", "foobar", "mark"))
        succeed.error shouldBe null
        succeed.user shouldNotBe null
        val fail = signupService(SignupService.Request("mark@contrapt.net", "foobar", "mark"))
        fail.error shouldNotBe null
        fail.user shouldBe null
    }
}