package net.contrapt.vertek.example.route

import io.kotlintest.matchers.shouldBe
import io.kotlintest.mock.mock
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import net.contrapt.vertek.endpoints.mock.MockConnector
import net.contrapt.vertek.example.service.ResultService
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.Mockito.verify

@RunWith(VertxUnitRunner::class)
class ResultConsumerTest {

    lateinit var consumer: ResultConsumer
    lateinit var connector: MockConnector
    lateinit var errorConnector: MockConnector
    lateinit var resultConnector: MockConnector
    lateinit var resultService: ResultService

    @get:Rule
    var rule = RunTestOnContext()

    @Before
    fun before(context: TestContext) {
        rule.vertx().exceptionHandler(context.exceptionHandler())
        resultService = mock<ResultService>()
        connector = MockConnector("result.test")
        errorConnector = MockConnector("result.error")
        resultConnector = MockConnector("result.result")
        consumer = ResultConsumer(connector, resultConnector, errorConnector, resultService)
    }

    @Test(timeout = 10000)
    fun testSuccess(context: TestContext) {
        Mockito.`when`(resultService.doSomething()).then {}
        rule.vertx().deployVerticle(consumer, context.asyncAssertSuccess(){
            val message = JsonObject().put("meta", JsonObject().put("profileId", "123"))
            consumer.send(message, context.asyncAssertSuccess(){
                verify(resultService, times(1)).doSomething()
                connector.messages.size shouldBe 1
                errorConnector.messages.size shouldBe 0
                resultConnector.messages.size shouldBe 1
            })
            // test stuff
        })
    }

}