package net.contrapt.knex.example.route

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import net.contrapt.knex.endpoints.AbstractProducer
import net.contrapt.knex.endpoints.ProducerConnector

/**
 * Simple message publisher
 */
class SimpleProducer(connector: ProducerConnector) : AbstractProducer(connector) {

    override fun handleMessage(message: Message<JsonObject>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}