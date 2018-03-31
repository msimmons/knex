package net.contrapt.vertek.endpoints

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import net.contrapt.vertek.plugs.MessagePlug
import net.contrapt.vertek.plugs.Plug

/**
 * A producer of messages via the [send] methods.  The [ProducerConnector] defines the external publishing
 * system.  Subclasses of [AbstractProducer] define the message handling in [handleMessage] and can add
 * [MessagePlug]s for pipeline processing and [ExceptionHandler]s
 */
abstract class AbstractProducer(connector: ProducerConnector) : AbstractEndpoint(connector) {

    /** Default async handler for event bus send */
    private val defaultHandler = Handler<AsyncResult<Message<JsonObject>>> {}

    /**
     * Publishes the given [Message] to this [Producer]'s [Connector].  [Message] contents are typically [Connector]
     * specific
     */
    fun send(message: Message<JsonObject>, handler: Handler<AsyncResult<Message<JsonObject>>> = defaultHandler) {
        vertx.eventBus().send(connector.address, message.body(), DeliveryOptions().setHeaders(message.headers()), handler)
    }

    /**
     * Publishes the given [JsonObject] body as a [Message] to this [Connector]
     */
    fun send(body: JsonObject, handler: Handler<AsyncResult<Message<JsonObject>>> = defaultHandler) {
        vertx.eventBus().send(connector.address, body, DeliveryOptions(), handler)
    }

    /**
     * Handles consuming from the [EventBus], processing [Plug]s and [handleMessage] possibly producing new payload to
     * be published via the [Connector].  Unhandled exceptions will cause the [Connector]'s [handleFailure] method
     * to be called
     */
    final override fun handle(message : Message<JsonObject>) {
        vertx.executeBlocking(Handler { future ->
            try {
                handleMessage(message)
                processPlugs(message)
                future.complete()
            }
            catch (e: Exception) {
                if (handleException(message, e)) {
                    logger.debug("Handled exception $e")
                    future.complete()
                } else {
                    future.fail(e)
                }
            }
        }, false, Handler<AsyncResult<Unit>> {ar ->
            if ( ar.failed() ) connector.handleFailure(message, ar.cause())
            else connector.handleSuccess(message)
        })
    }

    /**
     * Do any processing of the message before outbound [MessagePlug]s are applied and [ProducerConnector]
     * publishes the message
     */
    abstract fun handleMessage(message: Message<JsonObject>)

}
