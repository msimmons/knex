package net.contrapt.vertek.example.route

import io.kotlintest.matchers.shouldBe
import io.kotlintest.mock.`when`
import io.kotlintest.mock.mock
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import net.contrapt.vertek.endpoints.mock.MockConnector
import net.contrapt.vertek.example.service.SimpleService
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(VertxUnitRunner::class)
class SimpleConsumerTest {

    lateinit var consumer: SimpleConsumer
    lateinit var connector: MockConnector
    lateinit var errorConnector: MockConnector
    lateinit var simpleService: SimpleService

    @get:Rule
    var rule = RunTestOnContext()

    @Before
    fun before(context: TestContext) {
        rule.vertx().exceptionHandler(context.exceptionHandler())
        simpleService = mock<SimpleService>()
        connector = MockConnector("simple.test")
        errorConnector = MockConnector("simple.error")
        consumer = SimpleConsumer(connector, errorConnector, simpleService)
    }

    @Test(timeout = 10000)
    fun testSuccess(context: TestContext) {
        Mockito.`when`(simpleService.doSomething()).then {}
        rule.vertx().deployVerticle(consumer, context.asyncAssertSuccess(){
            val message = JsonObject().put("meta", JsonObject().put("profileId", "123"))
            consumer.send(message, context.asyncAssertSuccess(){
                verify(simpleService, times(1)).doSomething()
                connector.messages.size shouldBe 1
                errorConnector.messages.size shouldBe 0
            })
            // test stuff
        })
    }

    @Test(timeout = 10000)
    fun testError(context: TestContext) {
        `when`(simpleService.doSomething()).thenThrow(RuntimeException("you can't do this"))
        rule.vertx().deployVerticle(consumer, context.asyncAssertSuccess(){
            val message = JsonObject().put("meta", JsonObject().put("profileId", "123"))
            consumer.send(message, context.asyncAssertSuccess(){
                verify(simpleService, times(1)).doSomething()
                connector.messages.size shouldBe 1
                errorConnector.messages.size shouldBe 1
            })
            // test stuff
        })
    }
}