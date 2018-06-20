package net.contrapt.vertek.example.route

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import net.contrapt.vertek.endpoints.AbstractProducer
import net.contrapt.vertek.endpoints.ProducerConnector
import net.contrapt.vertek.example.plugs.OutboundProcessor
import net.contrapt.vertek.example.plugs.Tracer

/**
 * A result producer
 */
class ResultProducer(connector: ProducerConnector) : AbstractProducer(connector) {

    init {
        addPlug(Tracer().outbound)
        addPlug(OutboundProcessor())
    }

    override fun handleMessage(message: Message<JsonObject>) {
    }

}