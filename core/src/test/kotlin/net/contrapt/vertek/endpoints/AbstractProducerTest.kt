package net.contrapt.vertek.endpoints

import io.kotlintest.matchers.shouldBe
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import net.contrapt.vertek.endpoints.mock.MockConnector
import net.contrapt.vertek.plugs.MessagePlug
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant

@RunWith(VertxUnitRunner::class)
class AbstractProducerTest {

    @get:Rule
    var rule = RunTestOnContext()
    lateinit var connector: MockConnector
    lateinit var producer: TestProducer

    @Before
    fun before(context: TestContext) {
        rule.vertx().exceptionHandler(context.exceptionHandler())
        connector = MockConnector("test")
        producer = TestProducer(connector)
    }

    @Test
    fun testSuccess(context: TestContext) {
        rule.vertx().deployVerticle(producer, context.asyncAssertSuccess(){
            producer.send(JsonObject().put("key", "success"), handler = context.asyncAssertSuccess {
                connector.successfulMessages.size shouldBe 1
            })
        })
    }

    @Test
    fun testFailure(context: TestContext) {
        rule.vertx().deployVerticle(producer, context.asyncAssertSuccess(){
            producer.send(JsonObject().put("key", "failure"), handler = context.asyncAssertSuccess {
                connector.failedMessages.size shouldBe 1
            })
        })
    }

    @Test
    fun testPlug(context: TestContext) {
        producer.addPlug(TestPlug())
        rule.vertx().deployVerticle(producer, context.asyncAssertSuccess(){
            producer.send(JsonObject().put("key", "success"), context.asyncAssertSuccess() {
                connector.successfulMessages.size shouldBe 1
                connector.successfulMessages[0].body().containsKey("plug") shouldBe true
            })
        })
    }

    class TestProducer(connector: ProducerConnector) : AbstractProducer(connector) {

        override fun handleMessage(message: Message<JsonObject>) {
            message.headers()
            when ( message.body().getString("key") ) {
                "failure" -> throw RuntimeException("Asked to throw")
            }
        }

    }

    class TestPlug : MessagePlug {

        override fun process(message: Message<JsonObject>) {
            message.body().put("plug", Instant.now())
        }

    }
}

