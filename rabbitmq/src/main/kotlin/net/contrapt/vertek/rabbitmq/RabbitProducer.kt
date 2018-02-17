package net.contrapt.vertek.rabbitmq

import com.rabbitmq.client.ConnectionFactory
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject

import net.contrapt.vertek.endpoints.AbstractProducer

/**
 * Represents the configuration of a message bus endpoint that we would like to
 * send a message to
 */
abstract class RabbitProducer (
    val connectionFactory: ConnectionFactory,
    address: String,
    val exchange: String,
    val routingKey: String
) : AbstractProducer(address) {

    lateinit var client : RabbitClient

    final override fun start() {
        client = RabbitClient.create(vertx, connectionFactory)
        client.start({
            logger.info("Rabbit client connected")
            logger.info("Publishing address $address -> $exchange:$routingKey")
        })
    }

    override fun handleMessage(message: Message<JsonObject>): JsonObject {
        val body = message.body().getString("body")
        val properties = message.body().getJsonObject("properties") ?: JsonObject()
        return JsonObject().put("body", body).put("properties", properties)
    }

    override fun handleFailure(message: Message<JsonObject>, cause: Throwable) {
        super.handleFailure(message, cause)
    }

    override fun handleSuccess(message: JsonObject) {
        basicPublish(message)
    }

    /**
     * Invoke [basicPublish] to publish the message to rabbit
     */
    private fun basicPublish(message: JsonObject) {
        client.basicPublish(exchange, routingKey, message, Handler<AsyncResult<Unit>> {ar ->
            if ( ar.cause() != null ) logger.error("Error publishing message: $message", ar.cause())
        })
    }

}
