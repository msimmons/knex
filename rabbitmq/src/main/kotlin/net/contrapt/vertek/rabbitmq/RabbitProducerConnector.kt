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
 * Represents the configuration of a message bus endpoint that we would like to
 * send a message to
 */
abstract class RabbitProducerConnector(
    val connectionFactory: ConnectionFactory,
    override val address: String,
    val exchange: String,
    val routingKey: String
) : ProducerConnector {

    private val logger = LoggerFactory.getLogger(javaClass)

    lateinit var client : RabbitClient

    final override fun start(vertx: Vertx, messageHandler: Handler<Message<JsonObject>>, startHandler: Handler<AsyncResult<Unit>>) {
        client = RabbitClient.create(vertx, connectionFactory)
        client.start(clientStartupHandler(vertx, messageHandler, startHandler))
    }

    private fun clientStartupHandler(vertx: Vertx, handler: Handler<Message<JsonObject>>, startHandler: Handler<AsyncResult<Unit>>) = Handler<AsyncResult<Unit>> { ar ->
        if ( ar.succeeded() ) {
            logger.info("Rabbit client connected")
            logger.info("Publishing address $address -> $exchange:$routingKey")
            vertx.eventBus().consumer<JsonObject>(address, handler)
            startHandler.handle(Future.succeededFuture())
        }
        else {
            startHandler.handle(Future.failedFuture(ar.cause()))
        }
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
        client.basicPublish(exchange, routingKey, message.body(), Handler<AsyncResult<Unit>> {ar ->
            if ( ar.cause() != null ) logger.error("Error publishing message: $message", ar.cause())
        })
    }

}
