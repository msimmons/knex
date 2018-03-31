package net.contrapt.vertek.rabbitmq

import com.rabbitmq.client.ConnectionFactory
import io.kotlintest.matchers.shouldBe
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.unit.Async
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import net.contrapt.vertek.endpoints.AbstractProducer
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class RabbitProducerTest {

    private val logger = LoggerFactory.getLogger(javaClass)

    @get:Rule
    var rule = RunTestOnContext()
    lateinit var factory: ConnectionFactory
    lateinit var producer: TestProducer
    lateinit var connector: RabbitProducerConnector

    @Before
    fun before(context: TestContext) {
        rule.vertx().exceptionHandler(context.exceptionHandler())
        factory = ConnectionFactory().apply {
            host = "localhost"
            isAutomaticRecoveryEnabled = true
            networkRecoveryInterval = 5000
        }
        connector = RabbitProducerConnector(factory, "producer.test", "amq.topic", "producer.test")
        producer = TestProducer(connector)
    }

    @Test(timeout = 10000)
    fun testSuccess(context: TestContext) {
        rule.vertx().deployVerticle(producer, context.asyncAssertSuccess(){
            val message = JsonObject().put("properties", JsonObject()
                    .put(RabbitProperty.PRIORITY, 1)
                    .put(RabbitProperty.CONTENT_TYPE, "aes/gcm")
                    .put(RabbitProperty.CORRELATION_ID, "456"))
            message.getJsonObject("properties").put("headers", JsonObject().put("profileId", "123"))
            message.put("body", """{"meta":{"profile_id": "123"}}""").put("key", "success")
            producer.finished = context.async()
            producer.send(message, context.asyncAssertSuccess() {
                producer.finished.awaitSuccess()
                producer.messageCount shouldBe 1
            })
        })
    }

    class TestProducer(connector: RabbitProducerConnector) : AbstractProducer(connector) {

        var messageCount = 0
        lateinit var finished : Async

        override fun handleMessage(message: Message<JsonObject>) {
            messageCount++
            finished.complete()
        }

    }
}