package net.contrapt.vertek.endpoints

import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject

/**
 * Defines the contract for connecting to different types of consumers and producers.  [Connector]s act as bridges
 * between the [EventBus] and external resources such as message buses, web sockets etc.
 */
interface Connector {

    /**
     * The event bus bridge address
     */
    val address: String

    /**
     * Startup the [Connector]
     * @param vertx The current instance of [Vertx]
     * @param messageHandler The connector's handling of the message
     * @param started A [Future] to indicate when the connector has started
     */
    fun start(vertx: Vertx, messageHandler: Handler<Message<JsonObject>>, started: Future<Unit>)

    /**
     * Handle failure of message processing.  This method will be called for any unhandled exceptions during message
     * processing
     */
    fun handleFailure(message: Message<JsonObject>, cause: Throwable)

    /**
     * Handle successful message processing.  This method will be called for all successful handling
     */
    fun handleSuccess(message: Message<JsonObject>)
}