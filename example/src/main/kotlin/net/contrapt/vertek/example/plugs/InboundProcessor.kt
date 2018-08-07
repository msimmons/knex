package net.contrapt.vertek.example.plugs

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import net.contrapt.vertek.plugs.MessagePlug

class InboundProcessor : MessagePlug {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Convert incoming body into appropriate format -- (for us a JSON object)
     */
    override fun process(message: Message<JsonObject>) {
        val bodyObject = message.body().getValue("body")
        val body = when (bodyObject) {
            is String -> JsonObject(bodyObject)
            is JsonObject -> bodyObject
            else -> JsonObject()
        }
        message.body().put("body", body)
        val properties = message.body().getJsonObject("properties", JsonObject())
        //logger.info("MESSAGE ${properties} direction=in")
    }
}