package net.contrapt.vertek.endpoints

import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject

/**
 * Consumes messages routed by web socket connection
 */
abstract class WebSocketConsumer(
    val address: String
) : AbstractConsumer(), Handler<Message<JsonObject>> {

    final override fun start() {
        vertx.eventBus().consumer<JsonObject>(address, this)
        startInternal()
    }

    /**
     * Override this method to start any additional consumers, timers etc when this consumer starts
     */
    abstract fun startInternal()

}
