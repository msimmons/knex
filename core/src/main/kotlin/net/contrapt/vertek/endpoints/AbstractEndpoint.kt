package net.contrapt.vertek.endpoints

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import net.contrapt.vertek.plugs.MessagePlug
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

/**
 * Interface and default implementations for consumers and producers
 */
abstract class AbstractEndpoint(val connector: Connector) : AbstractVerticle(), Handler<Message<JsonObject>> {

    protected val logger = LoggerFactory.getLogger(javaClass)

    private val plugs = mutableListOf<MessagePlug>()
    private val exceptionHandlers = mutableListOf<Pair<KClass<out Throwable>?, ExceptionHandler>>()

    /**
     * Start this producer [Verticle] by starting the [ProducerConnector] and if successful calling [startupInternal]
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
     * Add an [ExceptionHandler]. They are called in the order added for the given [Throwable] or any superclasses
     * of it until on of the handlers returns [true] or the end of the list is reached.  If no handlers return [true],
     * then the [Connector]'s failure handler is called
     */
    fun addExceptionHandler(handler: ExceptionHandler, klass: KClass<out Throwable>? = null) {
        exceptionHandlers.add(Pair(klass, handler))
    }

    protected fun processPlugs(message: Message<JsonObject>) {
        plugs.forEach {
            it.process(message)
        }
    }

    protected fun handleException(message: Message<JsonObject>, exception: Throwable) : Boolean {
        var handled = false
        exceptionHandlers.forEach { handler ->
            val handledClass = handler.first
            handled = when {
                handledClass == null -> handler.second.handle(message, exception)
                handledClass.isSuperclassOf(exception::class) -> handler.second.handle(message, exception)
                else -> false
            }
        }
        return handled
    }
}