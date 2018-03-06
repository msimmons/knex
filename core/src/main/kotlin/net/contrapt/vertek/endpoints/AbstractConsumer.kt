package net.contrapt.vertek.endpoints

import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import net.contrapt.vertek.plugs.MessagePlug
import net.contrapt.vertek.plugs.Plug

/**
 * A consumer of messages.  Concrete subclasses define message handling in [handleMessage] and provide a
 * [ConsumerConnector] which implements the particular external transport.  Subclasses can also provide
 * [MessagePlug]'s for pipeline processing and [ExceptionHandler]'s for defining exception behaviour
 */
abstract class AbstractConsumer(val connector: ConsumerConnector) : AbstractVerticle(), Handler<Message<JsonObject>> {

    private val plugs = mutableListOf<MessagePlug>()
    protected val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Start this consumer [Verticle] by starting the [ConsumerConnector] and if successful calling [startInternal]
     */
    final override fun start(future: Future<Void>) {
        connector.start(vertx, this, Handler { ar ->
            if ( ar.succeeded() ) {
                startInternal()
                future.complete()
            }
            else {
                future.fail(ar.cause())
            }
        })
    }

    /**
     * Override to define additional startup code
     */
    open fun startInternal() {}

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
        vertx.executeBlocking(Handler { future ->
            try{
                processInbound(message)
                handleMessage(message)
                future.complete()
            }
            catch (e: Exception) {
                // TODO Exception handlers
                future.fail(e)
            }
        }, false, Handler<AsyncResult<Unit>> {ar ->
            if ( ar.failed() ) connector.handleFailure(message, ar.cause())
            else connector.handleSuccess(message)
        })
    }

    private fun processInbound(message: Message<JsonObject>) {
        plugs.forEach {
            it.process(message)
        }
    }

    /**
     * Process the consumed [Message] after all inbound [MessagePlug]s are applied
     */
    abstract fun handleMessage(message: Message<JsonObject>)

}
