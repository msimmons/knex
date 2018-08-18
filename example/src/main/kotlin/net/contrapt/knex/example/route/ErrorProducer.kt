package net.contrapt.knex.example.route

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import net.contrapt.knex.endpoints.AbstractProducer
import net.contrapt.knex.endpoints.ProducerConnector
import net.contrapt.knex.example.plugs.OutboundProcessor

class ErrorProducer(connector: ProducerConnector) : AbstractProducer(connector) {

    init {
        addPlug(OutboundProcessor())
    }

    override fun handleMessage(message: Message<JsonObject>) {
        // What to do?
    }

}