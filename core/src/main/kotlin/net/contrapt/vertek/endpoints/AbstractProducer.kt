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
 * A producer of messages via the [send] methods.  The [ProducerConnector] defines the external publishing
 * system.  Subclasses of [AbstractProducer] define the message handling in [handleMessage] and can add
 * [MessagePlug]s for pipeline processing and [ExceptionHandler]s
 */
abstract class AbstractProducer(val connector: ProducerConnector) : AbstractVerticle(), Handler<Message<JsonObject>> {

    protected val logger = LoggerFactory.getLogger(javaClass)
    private val plugs = mutableListOf<MessagePlug>()

    /**
     * Start this producer [Verticle] by starting the [ProducerConnector] and if successful calling [startupInternal]
     */
    final override fun start(future: Future<Void>) {
        connector.start(vertx, this, Handler { ar ->
            if ( ar.succeeded() ) {
                startupInternal()
                future.complete()
            }
            else {
                future.fail(ar.cause())
            }
        })
    }

    /**
     * Subclasses can override this to provide initialization.  It is called after the
     * [Connector] has started
     */
    open fun startupInternal() {
    }

    /**
     * Add a [Plug] to outbound processing.  Plugs will be applied in order they are added
     */
    fun addPlug(plug: MessagePlug) {
        plugs.add(plug)
    }

    /** Default async handler for event bus send */
    private val defaultHandler = Handler<AsyncResult<Message<JsonObject>>> {}

    /**
     * Publishes the given [Message] on the [EventBus].  Contents of message body are [Connector] specific
     */
    fun send(message: Message<JsonObject>, handler: Handler<AsyncResult<Message<JsonObject>>> = defaultHandler) {
        vertx.eventBus().send(connector.address, message.body(), DeliveryOptions().setHeaders(message.headers()), handler)
    }

    /**
     * Publishes the given [JsonObject] body as a [Message] on the [EventBus]
     */
    fun send(body: JsonObject, handler: Handler<AsyncResult<Message<JsonObject>>> = defaultHandler) {
        vertx.eventBus().send(connector.address, body, DeliveryOptions(), handler)
    }

    /**
     * Handles consuming from the [EventBus], processing [Plug]s and [handleMessage] possibly producing new payload to
     * be published
     */
    final override fun handle(message : Message<JsonObject>) {
        vertx.executeBlocking(Handler { future ->
            try {
                handleMessage(message)
                processOutbound(message)
                future.complete()
            }
            catch (e: Exception) {
                //TODO add exception handlers
                future.fail(e)
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

    /**
     * Apply plugs to the given [Message] in the order they were added.  This happens after the outgoing message
     * is processed by [handleMessage] and right before the [ProducerConnector] publishes
     */
    private fun processOutbound(message: Message<JsonObject>) {
        plugs.forEach {
            it.process(message)
        }
    }

}
