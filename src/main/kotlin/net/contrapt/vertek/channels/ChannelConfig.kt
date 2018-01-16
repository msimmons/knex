package net.contrapt.vertx.channels

import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject

/**
 * Represents the configuration of a websocket api endpoint that we would like to provide
 */
data class ChannelConfig(
    val method: String,
    val path: String,
    val handler: Handler<Message<JsonObject>>
)
