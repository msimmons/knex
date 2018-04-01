package net.contrapt.vertek.example.route

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import net.contrapt.vertek.endpoints.AbstractProducer
import net.contrapt.vertek.endpoints.ProducerConnector

/**
 * A result producer
 */
class ResultProducer(connector: ProducerConnector) : AbstractProducer(connector) {

    override fun handleMessage(message: Message<JsonObject>) {
    }

}