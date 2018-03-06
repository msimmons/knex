package net.contrapt.vertek.endpoints

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import net.contrapt.vertek.endpoints.mock.MockConnector
import net.contrapt.vertek.plugs.MessagePlug
import org.junit.After
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

    @After
    fun after(context: TestContext) {
    }

    @Test
    fun testSuccess(context: TestContext) {
        rule.vertx().deployVerticle(consumer, context.asyncAssertSuccess(){
            connector.send(JsonObject().put("key", "success"), context.asyncAssertSuccess() {
                context.assertEquals(1, connector.successfulMessages.size)
            })
        })
    }

    @Test
    fun testFailure(context: TestContext) {
        rule.vertx().deployVerticle(consumer, context.asyncAssertSuccess(){
            connector.send(JsonObject().put("key", "failure"), context.asyncAssertSuccess() {
                context.assertEquals(1, connector.failedMessages.size)
            })
        })
    }

    @Test
    fun testPlug(context: TestContext) {
        consumer.addPlug(TestPlug())
        rule.vertx().deployVerticle(consumer, context.asyncAssertSuccess(){
            connector.send(JsonObject().put("key", "success"), context.asyncAssertSuccess() {
                context.assertEquals(1, connector.successfulMessages.size)
                context.assertTrue(connector.successfulMessages[0].body().containsKey("plug"))
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
}

