package net.contrapt.vertek.endpoints.ws

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import net.contrapt.vertek.endpoints.ConsumerConnector

/**
 * Consumes messages routed by web socket connection
 */
class WebSocketConnector(
    override val address: String
) : ConsumerConnector {

    override fun start(vertx: Vertx, handler: Handler<Message<JsonObject>>, startHandler: Handler<AsyncResult<Unit>>) {
        vertx.eventBus().consumer<JsonObject>(address, handler)
        startHandler.handle(Future.succeededFuture())
    }

    override fun handleFailure(message: Message<JsonObject>, cause: Throwable) {
        message.fail(500, cause.message)
    }

    override fun handleSuccess(message: Message<JsonObject>) {
    }
}