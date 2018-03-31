package net.contrapt.vertek.endpoints

import io.kotlintest.matchers.shouldBe
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
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
class AbstractConsumerTest {

    @get:Rule
    var rule = RunTestOnContext()
    lateinit var consumer: TestConsumer
    lateinit var connector: MockConnector

    @Before
    fun before(context: TestContext) {
        rule.vertx().exceptionHandler(context.exceptionHandler())
        connector = MockConnector("test")
        consumer = TestConsumer(connector)
    }

    @Test
    fun testSuccess(context: TestContext) {
        rule.vertx().deployVerticle(consumer, context.asyncAssertSuccess(){
            connector.send(JsonObject().put("key", "success"), context.asyncAssertSuccess() {
                connector.messages.size shouldBe 1
            })
        })
    }

    @Test
    fun testFailure(context: TestContext) {
        rule.vertx().deployVerticle(consumer, context.asyncAssertSuccess(){
            connector.send(JsonObject().put("key", "failure"), context.asyncAssertSuccess() {
                connector.failedMessages.size shouldBe 1
            })
        })
    }

    @Test
    fun testPlug(context: TestContext) {
        consumer.addPlug(TestPlug())
        rule.vertx().deployVerticle(consumer, context.asyncAssertSuccess(){
            connector.send(JsonObject().put("key", "success"), context.asyncAssertSuccess() {
                connector.messages.size shouldBe 1
                connector.messages[0].body().containsKey("plug") shouldBe true
            })
        })
    }

    @Test
    fun testExceptionHandler(context: TestContext) {
        consumer.addExceptionHandler(TestExceptionHandler(true))
        rule.vertx().deployVerticle(consumer, context.asyncAssertSuccess(){
            connector.send(JsonObject().put("key", "failure"), context.asyncAssertSuccess() {
                connector.messages.size shouldBe 1
                connector.messages[0].body().containsKey("handled") shouldBe true
            })
        })
    }

    class TestConsumer(connector: ConsumerConnector) : AbstractConsumer(connector) {

        override fun handleMessage(message: Message<JsonObject>) {
            when ( message.body().getString("key") ) {
                "failure" -> throw RuntimeException("I have been asked to fail")
            }
        }
    }

    class TestPlug : MessagePlug {

        override fun process(message: Message<JsonObject>) {
            message.body().put("plug", Instant.now())
        }

    }

    class TestExceptionHandler(val handled: Boolean) : ExceptionHandler {

        val logger = LoggerFactory.getLogger(javaClass)

        override fun handle(message: Message<JsonObject>, exception: Throwable): Boolean {
            logger.error("Exception handler", exception)
            message.body().put("handled", handled)
            return handled
        }

    }
}

