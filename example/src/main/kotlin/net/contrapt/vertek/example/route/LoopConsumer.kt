package net.contrapt.vertek.example.route

import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import net.contrapt.vertek.endpoints.AbstractConsumer
import net.contrapt.vertek.endpoints.ConsumerConnector
import net.contrapt.vertek.example.plugs.InboundProcessor

/**
 * A consumer that processes a message and produces a result
 */
class LoopConsumer (
        connector: ConsumerConnector,
        val res: ResultConsumer
) : AbstractConsumer(connector) {

    init {
        addPlug(InboundProcessor())
    }

    override fun handleMessage(message: Message<JsonObject>) {
        val loopCount = message.body().getJsonObject("body").getInteger("loopCount", 10)
        (0..loopCount).forEach {
            res.send(message.body(), Handler {  })
        }
    }

}