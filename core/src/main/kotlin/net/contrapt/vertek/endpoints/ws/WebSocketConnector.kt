package net.contrapt.knex.endpoints.ws

import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import net.contrapt.knex.endpoints.ConsumerConnector

/**
 * Consumes messages routed by the [SockJSHandler] via its [bridge] feature.  The [Message]s are bridged to the given
 * address and handled by the given [messageHandler]
 */
class WebSocketConnector(
    override val address: String
) : ConsumerConnector {

    override fun start(vertx: Vertx, messageHandler: Handler<Message<JsonObject>>, started: Future<Unit>) {
        vertx.eventBus().consumer<JsonObject>(address, messageHandler)
        started.complete()
    }

    override fun handleFailure(message: Message<JsonObject>, cause: Throwable) {
        message.fail(500, cause.message)
    }

    override fun handleSuccess(message: Message<JsonObject>) {
    }
}