package net.contrapt.knex.example.route

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import net.contrapt.knex.endpoints.AbstractConsumer
import net.contrapt.knex.endpoints.ConsumerConnector

class WsConsumer(connector: ConsumerConnector) : AbstractConsumer(connector) {

    override fun handleMessage(message: Message<JsonObject>) {
        logger.info("Got Message: ${message.body().encode()} ${message.replyAddress()}")
        message.reply(JsonObject().put("firstName", "Mark").put("age", 30).encode())
    }
}