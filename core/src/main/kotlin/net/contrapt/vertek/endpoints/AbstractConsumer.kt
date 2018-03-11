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
     * Handle the incoming [message] by first applying [Plug]s then executing this classes [handleMessage].  Any
     * unhandled exceptions will result in message being nacked if [autoAck] is not set
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
                else future.fail(e)
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

}
