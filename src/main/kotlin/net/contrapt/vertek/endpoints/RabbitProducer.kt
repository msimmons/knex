package net.contrapt.vertx.endpoints

import com.rabbitmq.client.ConnectionFactory
import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import net.contrapt.vertx.plugs.MessagePlug
import net.contrapt.vertx.plugs.Plug

/**
 * Represents the configuration of a message bus endpoint that we would like to
 * send a message to
 */
abstract class RabbitProducer (
    val connectionFactory: ConnectionFactory,
    val address: String,
    val exchange: String,
    val routingKey: String
) : AbstractVerticle() {

    private val logger = LoggerFactory.getLogger(javaClass)

    protected val plugs = mutableListOf<MessagePlug>()

    lateinit var client : RabbitClient

    override fun start() {
        client = RabbitClient.create(vertx, connectionFactory)
        client.start(startupHandler())
    }

    fun startupHandler() = Handler<AsyncResult<Unit>> { async->
        when (async.succeeded()) {
            true -> {
                logger.info("Rabbit client connected")
                vertx.eventBus().consumer(address, publishHandler())
                addPlugs()
                logger.info("Publishing address $address -> $exchange:$routingKey")
            }
            false -> {
                logger.warn("Unable to connect to rabbit", async.cause())
                logger.warn("Trying again in 10s")
                vertx.setTimer(10000, {
                    logger.info("Trying again")
                    start()
                })
            }
        }
    }

    /**
     * Override this method to add [Plug]s to this consumer.  They will be applied to the incoming [Message] in order
     * before the message is sent to [handleMessage]
     */
    open fun addPlugs() {}

    /**
     * Publishes the given [Message] on the event bus for routing to [routingKey]
     */
    fun publish(message: Message<JsonObject>) {
        vertx.eventBus().publish(address, message.body(), DeliveryOptions().setHeaders(message.headers()))
    }

    /**
     * Publishes the given object as a [Message]
     */
    fun publish(body: JsonObject, properties: JsonObject = JsonObject()) {
        val message = JsonObject().put("body", body.encode()).put("properties", properties)
        vertx.eventBus().publish(address, message, DeliveryOptions())
    }

    /**
     * Handles consuming from the [EventBus] and publishing to this producer's [exchange] and [routingKey]
     */
    private fun publishHandler() = Handler<Message<JsonObject>> { message : Message<JsonObject> ->
        vertx.executeBlocking(Handler<Future<JsonObject>> { future ->
            try {
                processOutbound(message)
                val body = message.body().getString("body")
                val properties = message.body().getJsonObject("properties") ?: JsonObject()
                val newMessage = JsonObject().put("body", body).put("properties", properties)
                future.complete(newMessage)
            }
            catch (e: Exception) {
                future.fail(e)
            }
        }, false, Handler<AsyncResult<JsonObject>> {ar ->
            if ( ar.failed() ) throw RuntimeException("Unable to publish message", ar.cause())
            else basicPublish(ar.result())
        })
    }

    /**
     * Invoke [basicPublish] to publish the message to rabbit
     */
    private fun basicPublish(message: JsonObject) {
        client.basicPublish(exchange, routingKey, message, Handler<AsyncResult<Unit>> {ar ->
            if ( ar.cause() != null ) logger.error("Error publishing message: $message", ar.cause())
        })
    }

    /**
     * Apply plugs to the given [message] in the order they were added.  This happens before the outgoing message
     * is assembled and sent to the [EventBus]
     */
    private fun processOutbound(message: Message<JsonObject>) {
        plugs.forEach {
            it.process(message)
        }
    }

}
