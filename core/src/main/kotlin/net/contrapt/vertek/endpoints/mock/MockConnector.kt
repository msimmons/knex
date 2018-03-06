package net.contrapt.vertek.endpoints.mock

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import net.contrapt.vertek.endpoints.ConsumerConnector
import net.contrapt.vertek.endpoints.ProducerConnector

/**
 * A [Connector] to facilitate testing
 * TODO What does it do?
 */
class MockConnector(
    override val address: String
) : ConsumerConnector, ProducerConnector {

    private val logger = LoggerFactory.getLogger(javaClass)

    lateinit var vertx: Vertx
    val successfulMessages = mutableListOf<Message<JsonObject>>()
    val failedMessages = mutableListOf<Message<JsonObject>>()

    /**
     * Send a message for a consumer as if it came from the external [Connector]
     */
    fun send(message: JsonObject, handler: Handler<AsyncResult<Message<JsonObject>>>) {
        vertx.eventBus().send(address, message, handler)
    }

    override fun start(vertx: Vertx, messageHandler: Handler<Message<JsonObject>>, startHandler: Handler<AsyncResult<Unit>>) {
        this.vertx = vertx
        vertx.eventBus().consumer<JsonObject>(address, messageHandler)
        startHandler.handle(Future.succeededFuture())
    }

    override fun handleFailure(message: Message<JsonObject>, cause: Throwable) {
        message.body().put("vertek.exception", cause.message)
        failedMessages.add(message)
        logger.error(message.body(), cause)
        message.reply(message.body())
    }

    override fun handleSuccess(message: Message<JsonObject>) {
        successfulMessages.add(message)
        message.reply(message.body())
    }
}