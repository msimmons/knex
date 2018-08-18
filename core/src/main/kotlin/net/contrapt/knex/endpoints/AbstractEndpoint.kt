package net.contrapt.knex.endpoints

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Verticle
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import net.contrapt.knex.plugs.MessagePlug
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.jvmName

/**
 * An [AbstractEndpoint] uses a [Connector] to communicate with the outside world.  It defines message handling in its
 * [handle] method, pipelined transformations via [Plugs]s and exception handling via [ExceptionHandler]s
 */
abstract class AbstractEndpoint(val connector: Connector) : AbstractVerticle(), Handler<Message<JsonObject>> {

    protected val logger = LoggerFactory.getLogger(javaClass)

    private val plugs = mutableListOf<MessagePlug>()
    private val exceptionHandlers = mutableListOf<Pair<KClass<out Throwable>?, ExceptionHandler>>()

    /**
     * Start this endpoint [Verticle] by starting the [Connector]. Code defined in [beforeConnector] will run first,
     * followed by the [Connector]'s [start] method then code defined in [afterConnector].  The given [Future] will
     * be failed if any step fails
     */
    final override fun start(future: Future<Void>) {
        val failure = Future.future<Unit>()
        failure.setHandler {
            if (it.failed()) {
                future.fail(it.cause())
            }
        }
        val before = Future.future<Unit>()
        beforeConnector(before)
        before.compose {
            val connect = Future.future<Unit>()
            connector.start(vertx, this, connect)
            connect
        }.compose {
            val after = Future.future<Unit>()
            afterConnector(after)
            after
        }.compose({
            future.complete()
        }, failure)
    }

    /**
     * Deploy the given verticles sequentially, if any fail, fail the given [Future]
     */
    fun deployVerticles(verticles: Array<Verticle>, future: Future<Unit>) {
        if (verticles.size == 0) {
            future.complete()
            return
        }
        vertx.deployVerticle(verticles.first(), {
            if (it.failed()) future.fail(it.cause())
            else deployVerticles(verticles.sliceArray(1..(verticles.size - 1)), future)
        })
    }

    /**
     * Override to define additional startup code to be executed before the [Connector] is started, such as start other
     * dependent components
     */
    open fun beforeConnector(future: Future<Unit>) { future.complete() }

    /**
     * Override to define additional startup code executed after the connector is started
     */
    open fun afterConnector(future: Future<Unit>) { future.complete() }

    /**
     * Add a [MessagePlug] to the processing stream.  [Plug]s are executed in the order they are added
     */
    fun addPlug(plug: MessagePlug) {
        plugs.add(plug)
    }

    /**
     * Add an [ExceptionHandler]. They are called in the order added for the given [Throwable] or any superclasses
     * of it until one of the handlers returns [true] or the end of the list is reached.  If no handlers return [true],
     * then the [Connector]'s failure handler is called
     */
    fun addExceptionHandler(handler: ExceptionHandler, klass: KClass<out Throwable>? = null) {
        exceptionHandlers.add(Pair(klass, handler))
    }

    /**
     * Process each [Plug] in the order they were added.  Exceptions will propagate to the [ExceptionHandler]s if any
     */
    protected fun processPlugs(message: Message<JsonObject>) {
        plugs.forEach {
            it.process(message)
        }
    }

    /**
     * Call each [ExceptionHandler] that matches its declared exception class or subclass.  Return on the first
     * handler that says that it handled the exception, otherwise try all that match
     */
    protected fun handleException(message: Message<JsonObject>, exception: Throwable) : Boolean {
        var handled = false
        exceptionHandlers.forEach { handler ->
            val handledClass = handler.first
            handled = when {
                handledClass == null -> doHandle(handler.second, message, exception)
                handledClass.isSuperclassOf(exception::class) -> doHandle(handler.second, message, exception)
                else -> false
            }
            if (handled) return@forEach
        }
        return handled
    }

    /**
     * Call the [ExceptionHandler] in try/catch so that we can at least report exceptions in the exception handlers.
     * An exception causes the handler to return false
     */
    private fun doHandle(handler: ExceptionHandler, message: Message<JsonObject>, exception: Throwable) : Boolean {
        try {
            return handler.handle(message, exception)
        }
        catch (e: Exception) {
            logger.error("Exception occurred in exception handler ${handler::class.jvmName}", e)
            return false
        }
    }
}