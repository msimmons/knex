package net.contrapt.vertek.endpoints

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import net.contrapt.vertek.plugs.MessagePlug
import net.contrapt.vertek.plugs.Plug

/**
 * A consumer of messages.  Concrete subclasses define message handling in [handleMessage] and provide a
 * [ConsumerConnector] which implements the particular external transport.  Subclasses can also provide
 * [MessagePlug]'s for pipeline processing and [ExceptionHandler]'s for defining exception behaviour
 */
abstract class AbstractConsumer(connector: ConsumerConnector) : AbstractEndpoint(connector) {

    /**
     * Handle the incoming [Message] by first applying [Plug]s then executing this class's [handleMessage] method.  Any
     * unhandled exceptions will result in the [Connector]'s [handleFailure] method being called
     */
    final override fun handle(message: Message<JsonObject>) {
        // TODO What if you want a transaction around message handling?
        // TODO How would you implement aggregation, and maybe other EIPs?
        vertx.executeBlocking(Handler { future ->
            try{
                processPlugs(message)
                handleMessage(message)
                future.complete()
            }
            catch (e: Exception) {
                if (handleException(message, e)) {
                    logger.debug("Exception $e handled")
                    future.complete()
                }
                else {
                    logger.error("Unhandled exception", e)
                    future.fail(e)
                }
            }
        }, false, Handler<AsyncResult<Unit>> {ar ->
            if ( ar.failed() ) connector.handleFailure(message, ar.cause())
            else connector.handleSuccess(message)
        })
    }

    /**
     * Process the consumed [Message] after all inbound [MessagePlug]s are applied
     */
    abstract fun handleMessage(message: Message<JsonObject>)

    /**
     * Send an internal message to this [Consumer], bypassing the [Connector]
     */
    fun send(message: JsonObject, handler: Handler<AsyncResult<Message<JsonObject>>>) {
        vertx.eventBus().send(connector.address, message, handler)
    }


}
