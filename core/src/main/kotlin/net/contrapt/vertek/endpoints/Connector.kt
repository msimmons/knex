package net.contrapt.vertek.endpoints

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject

/**
 * Defines the contract for connecting to different types of consumers and producers
 */
interface Connector {

    val address: String

    /**
     * Startup the [Connector]
     * @param vertx The current instance of [Vertx]
     * @param messageHandler The connector's handling of the message
     * @param startHandler Handler to call after startup
     */
    fun start(vertx: Vertx, messageHandler: Handler<Message<JsonObject>>, startHandler: Handler<AsyncResult<Unit>>)

    /**
     * Handle failure of message processing
     */
    fun handleFailure(message: Message<JsonObject>, cause: Throwable)

    /**
     * Handle successful message processing
     */
    fun handleSuccess(message: Message<JsonObject>)
}