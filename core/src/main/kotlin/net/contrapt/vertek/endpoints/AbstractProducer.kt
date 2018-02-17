package net.contrapt.vertek.endpoints

import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import net.contrapt.vertek.plugs.MessagePlug
import net.contrapt.vertek.plugs.Plug

/**
 * A producer of messages.  Concrete subclasses define where the messages are going, such as message queue system, web
 * socket
 */
abstract class AbstractProducer(
    val address: String
) : AbstractVerticle(), Handler<Message<JsonObject>> {

    val logger = LoggerFactory.getLogger(javaClass)
    private val plugs = mutableListOf<MessagePlug>()

    init {
        vertx.eventBus().consumer(address, this)
    }

    /**
     * Add a [Plug] to outbound processing.  Plugs will be applied in order they are added
     */
    fun addPlug(plug: MessagePlug) {
        plugs.add(plug)
    }

    /**
     * Publishes the given [Message] on the event bus
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
     * Handles consuming from the [EventBus], processing [Plug]s and [handleMessage] possibly producing new payload to
     * be published
     */
    final override fun handle(message : Message<JsonObject>) {
        vertx.executeBlocking(Handler<Future<JsonObject>> { future ->
            try {
                processOutbound(message)
                val jsonObject = handleMessage(message)
                future.complete(jsonObject)
            }
            catch (e: Exception) {
                future.fail(e)
            }
        }, false, Handler<AsyncResult<JsonObject>> {ar ->
            if ( ar.failed() ) handleFailure(message, ar.cause())
            else handleSuccess(ar.result())
        })
    }

    abstract fun handleMessage(message: Message<JsonObject>) : JsonObject

    /**
     * Override this method to handle failure in publishing.  By default it throws a [RuntimeException]
     */
    open fun handleFailure(message: Message<JsonObject>, cause: Throwable) {
        throw RuntimeException("Unable to publish message", cause)
    }

    /**
     * Override this method to handle success -- by default it does nothing
     */
    open fun handleSuccess(message: JsonObject) {
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
