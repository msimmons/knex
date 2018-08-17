package net.contrapt.vertek.example.route

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import net.contrapt.vertek.endpoints.AbstractProducer
import net.contrapt.vertek.endpoints.ProducerConnector
import net.contrapt.vertek.example.plugs.OutboundProcessor

class ErrorProducer(connector: ProducerConnector) : AbstractProducer(connector) {

    init {
        addPlug(OutboundProcessor())
    }

    override fun handleMessage(message: Message<JsonObject>) {
        // What to do?
    }

}