package net.contrapt.knex.rabbitmq

import com.rabbitmq.client.ConnectionFactory
import io.kotlintest.matchers.shouldBe
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.unit.Async
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import net.contrapt.knex.endpoints.AbstractConsumer
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

    @Test(timeout = 10000)
    fun testSuccess(context: TestContext) {
        rule.vertx().deployVerticle(consumer, context.asyncAssertSuccess(){
            val message = JsonObject().put("properties", JsonObject()
                    .put(RabbitProperty.PRIORITY, 1)
                    .put(RabbitProperty.CONTENT_TYPE, "aes/gcm")
                    .put(RabbitProperty.CORRELATION_ID, "456"))
            message.getJsonObject("properties").put("headers", JsonObject().put("profileId", "123"))
            message.put("body", """{"meta":{"profile_id": "123"}}""").put("key", "success")
            consumer.finished = context.async()
            connector.publish(message, context.asyncAssertSuccess() {
                consumer.finished.awaitSuccess()
                consumer.messageCount shouldBe 1
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