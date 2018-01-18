package net.contrapt.vertek

import com.rabbitmq.client.ConnectionFactory
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.ext.unit.TestContext
import io.vertx.ext.unit.junit.RunTestOnContext
import io.vertx.ext.unit.junit.VertxUnitRunner
import net.contrapt.vertx.endpoints.RabbitConsumer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(VertxUnitRunner::class)
class TestSpec {

    @get:Rule
    var rule = RunTestOnContext()
    lateinit var verticle : RabbitConsumer

    @Before
    fun before(context: TestContext) {
        rule.vertx().exceptionHandler(context.exceptionHandler())
        val connectionFactory = ConnectionFactory().apply {
            host = "localhost"
            isAutomaticRecoveryEnabled = true
            networkRecoveryInterval = 5000
        }
        verticle = TestConsumer(connectionFactory)
    }

    @After
    fun after(context: TestContext) {
    }

    @Test
    fun testConnect(context: TestContext) {
        val finish = context.async()
        rule.vertx().deployVerticle(verticle, context.asyncAssertSuccess(){ id ->
        })
/*
        rule.vertx().eventBus().consumer<JsonObject>("anaddress", {message ->
            finish.complete()
        })
*/
    }

}

class TestConsumer(connectionFactory: ConnectionFactory) : RabbitConsumer(connectionFactory, "amq.topic", "test.key", "test.queue", autoAck = false, consumers = 3) {

    override fun startInternal() {
    }

    override fun handleMessage(message: Message<JsonObject>) {
        logger.info("Got the message: ${message}")
    }

}