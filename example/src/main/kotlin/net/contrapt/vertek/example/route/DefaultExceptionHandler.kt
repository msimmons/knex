package net.contrapt.vertek.example.route

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import net.contrapt.vertek.endpoints.AbstractProducer
import net.contrapt.vertek.endpoints.ExceptionHandler

class DefaultExceptionHandler(val endpoint: AbstractProducer) : ExceptionHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Log the error, add stack trace info the message and publish to the error endpoint
     */
    override fun handle(message: Message<JsonObject>, exception: Throwable): Boolean {
        logger.error("Failed on $message", exception)
        message.body().put("exception", exception.message)
        endpoint.send(message)
        return true
    }
}