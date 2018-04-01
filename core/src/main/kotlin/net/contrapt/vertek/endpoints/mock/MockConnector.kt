package net.contrapt.vertek.endpoints.mock

import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import net.contrapt.vertek.endpoints.ConsumerConnector
import net.contrapt.vertek.endpoints.ProducerConnector

/**
 * A [Connector] to facilitate testing.  The [MockConnector] keeps track of successful and failed messages that pass
 * through it so that your tests can verify expectations.  This can be used as a [ConsumerConnector] or a
 * [ProducerConnector]
 */
class MockConnector(
    override val address: String
) : ConsumerConnector, ProducerConnector {

    private val logger = LoggerFactory.getLogger(javaClass)

    val messages = mutableListOf<Message<JsonObject>>()
    val failedMessages = mutableListOf<Message<JsonObject>>()

    override fun start(vertx: Vertx, messageHandler: Handler<Message<JsonObject>>, started: Future<Unit>) {
        vertx.eventBus().consumer<JsonObject>(address, messageHandler)
        started.complete()
    }

    override fun handleFailure(message: Message<JsonObject>, cause: Throwable) {
        message.body().put("vertek.exception", cause.message)
        failedMessages.add(message)
        logger.error(message.body(), cause)
        message.reply(message.body())
    }

    override fun handleSuccess(message: Message<JsonObject>) {
        messages.add(message)
        message.reply(message.body())
    }
}