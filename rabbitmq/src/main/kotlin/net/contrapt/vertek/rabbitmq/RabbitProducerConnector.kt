package net.contrapt.vertek.rabbitmq

import com.rabbitmq.client.ConnectionFactory
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import net.contrapt.vertek.endpoints.ProducerConnector

/**
 * Bridge the [EventBus] at the address defined by [routingKey] to publish to a RabbitMQ broker.  The provided
 * [messageHandler] will consume from the bus and successful handling will result in a [basicPublish] of the message
 */
class RabbitProducerConnector(
        val connectionFactory: ConnectionFactory,
        val exchange: String,
        val routingKey: String
) : ProducerConnector {

    private val logger = LoggerFactory.getLogger(javaClass)
    override val address = routingKey
    private lateinit var client: RabbitClient

    override fun start(vertx: Vertx, messageHandler: Handler<Message<JsonObject>>, started: Future<Unit>) {
        synchronized(this, {
            if (::client.isInitialized) {
                started.complete()
                return
            }
            client = RabbitClient.create(vertx, connectionFactory)
        })
        doStartup(vertx, messageHandler, started)
    }

    private fun doStartup(vertx: Vertx, handler: Handler<Message<JsonObject>>, started: Future<Unit>) {
        val start = Future.future<Unit>()
        val finish = Future.future<Unit>().apply {
            setHandler {
                if (it.failed()) {
                    started.fail(it.cause())
                }
            }
        }
        client.start(start.completer())
        start.compose({
            logger.debug("Rabbit client connected at ${connectionFactory.host}")
            vertx.eventBus().consumer<JsonObject>(address, handler)
            logger.info("Publishing $address -> $exchange:$routingKey")
            started.complete()
        }, finish)
    }

    override fun handleFailure(message: Message<JsonObject>, cause: Throwable) {
        throw RuntimeException("Unable to publish message", cause)
    }

    override fun handleSuccess(message: Message<JsonObject>) {
        basicPublish(message)
    }

    /**
     * Invoke [basicPublish] to publish the message to rabbit
     */
    private fun basicPublish(message: Message<JsonObject>) {
        client.basicPublish(exchange, routingKey, message.body(), Handler<AsyncResult<Unit>> { ar ->
            if (ar.succeeded()) {
                message.reply(JsonObject())
            } else {
                logger.error("Error publishing message: $message", ar.cause())
                message.fail(500, ar.cause().message)
            }
        })
    }

}
