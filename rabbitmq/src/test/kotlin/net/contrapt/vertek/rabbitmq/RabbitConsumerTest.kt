package net.contrapt.vertek.rabbitmq

import com.rabbitmq.client.ConnectionFactory
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.unit.Async
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import net.contrapt.vertek.endpoints.AbstractConsumer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class RabbitConsumerTest {

    private val logger = LoggerFactory.getLogger(javaClass)

    @get:Rule
    var rule = RunTestOnContext()
    lateinit var factory: ConnectionFactory
    lateinit var consumer: TestConsumer
    lateinit var connector: RabbitConsumerConnector

    @Before
    fun before(context: TestContext) {
        rule.vertx().exceptionHandler(context.exceptionHandler())
        factory = ConnectionFactory().apply {
            host = "localhost"
            isAutomaticRecoveryEnabled = true
            networkRecoveryInterval = 5000
        }
        connector = RabbitConsumerConnector(factory, "amq.topic", "consumer.test", "consumer.test", durable = false)
        consumer = TestConsumer(connector)
    }

    @After
    fun after(context: TestContext) {
    }

    @Test(timeout = 10000)
    fun testSuccess(context: TestContext) {
        rule.vertx().deployVerticle(consumer, context.asyncAssertSuccess(){
            consumer.finished = context.async()
            connector.send(JsonObject().put("key", "success").put("body", "something"), context.asyncAssertSuccess() {
                consumer.finished.awaitSuccess()
                context.assertEquals(1, consumer.messageCount, "Message count")
            })
        })
    }

    class TestConsumer(connector: RabbitConsumerConnector) : AbstractConsumer(connector) {

        var messageCount = 0
        lateinit var finished : Async

        override fun handleMessage(message: Message<JsonObject>) {
            messageCount++
            logger.info("Handling the message ${message.body()}")
            finished.complete()
        }

    }
}