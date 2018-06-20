package net.contrapt.vertek.example.plugs

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import net.contrapt.vertek.plugs.MessagePlug

class InboundProcessor : MessagePlug {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Turn the message into a JSON object
     */
    override fun process(message: Message<JsonObject>) {
        val json = JsonObject(message.body().getString("body"))
        message.body().put("body", json)
        val properties = message.body().getJsonObject("properties")
        logger.info("MESSAGE correlationId=${properties.getString("correlationId")} direction=in")
    }
}