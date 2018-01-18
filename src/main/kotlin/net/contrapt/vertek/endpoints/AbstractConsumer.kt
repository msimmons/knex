package net.contrapt.vertx.endpoints

import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import net.contrapt.vertx.plugs.MessagePlug
import net.contrapt.vertx.plugs.Plug

/**
 * A consumer of messages.  Concrete subclasses define where these messages are coming from, such as a message broker,
 * web socket connection etc.
 */
abstract class AbstractConsumer(
) : AbstractVerticle(), Handler<Message<JsonObject>> {

    private val plugs = mutableListOf<MessagePlug>()
    val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Add a [MessagePlug] to the inbound processing stream.  Plugs are executed in the order they are added
     */
    fun addPlug(plug: MessagePlug) {
        plugs.add(plug)
    }

    /**
     * Handle the incoming [message] by first applying [Plug]s then executing this classes [handleMessage].  Any
     * unhandled exceptions will result in message being nacked if [autoAck] is not set
     */
    final override fun handle(message: Message<JsonObject>) {
        // TODO What if you want a transaction around message handling?
        // TODO How would you implement aggregation, and maybe other EIPs?
        vertx.executeBlocking(Handler<Future<Nothing>> { future ->
            try{
                processInbound(message)
                handleMessage(message)
                future.complete()
            }
            catch (e: Exception) {
                // TODO Exception handlers?
                future.fail(e)
            }
        }, false, Handler<AsyncResult<Nothing>> {ar ->
            if ( ar.failed() ) handleFailure(message, ar.cause())
            else handleSuccess(message)
        })
    }

    private fun processInbound(message: Message<JsonObject>) {
        plugs.forEach {
            it.process(message)
        }
    }

    /**
     * Override this method to implement the main [Message] handling code for this consumer
     */
    abstract fun handleMessage(message: Message<JsonObject>)

    /**
     * Override this method to implement message handler failure code
     * Default implementation does nothing
     */
    open fun handleFailure(message: Message<JsonObject>, cause: Throwable) {
    }

    /**
     * Override this method to implement message handler success
     * Default implementation does nothing
     */
    open fun handleSuccess(message: Message<JsonObject>) {
    }

}
