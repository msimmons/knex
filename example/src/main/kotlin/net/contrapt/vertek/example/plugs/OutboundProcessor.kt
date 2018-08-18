package net.contrapt.knex.example.plugs

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import net.contrapt.knex.plugs.MessagePlug

class OutboundProcessor : MessagePlug {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Make sure the message is a json string for outbound
     */
    override fun process(message: Message<JsonObject>) {
        val bodyObject = message.body().getValue("body")
        val body = when (bodyObject) {
            is String -> bodyObject
            is JsonObject -> bodyObject.encode()
            else -> ""
        }
        message.body().put("body", body)
        val properties = message.body().getJsonObject("properties")
        //logger.info("MESSAGE ${properties} direction=out")
    }
}