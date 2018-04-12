package net.contrapt.vertek.example.route

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import net.contrapt.vertek.endpoints.AbstractConsumer
import net.contrapt.vertek.endpoints.ConsumerConnector

class WsConsumer(connector: ConsumerConnector) : AbstractConsumer(connector) {

    override fun handleMessage(message: Message<JsonObject>) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}