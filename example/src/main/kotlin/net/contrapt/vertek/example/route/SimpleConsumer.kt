package net.contrapt.vertek.example.route

import io.vertx.core.Future
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import net.contrapt.vertek.endpoints.AbstractConsumer
import net.contrapt.vertek.endpoints.ConsumerConnector
import net.contrapt.vertek.endpoints.ProducerConnector
import net.contrapt.vertek.example.plugs.InboundProcessor
import net.contrapt.vertek.example.plugs.Tracer
import net.contrapt.vertek.example.service.SimpleService

/**
 * A simple consumer that consumes a message and does something with it
 */
class SimpleConsumer (
        connector: ConsumerConnector,
        errorConnector: ProducerConnector,
        private val simpleService: SimpleService
) : AbstractConsumer(connector) {

    private val errorProducer = ErrorProducer(errorConnector)

    init {
        addPlug(InboundProcessor())
        addPlug(Tracer().inbound)
    }

    override fun beforeConnector(future: Future<Unit>) {
        addExceptionHandler(DefaultExceptionHandler(errorProducer))
        deployVerticles(arrayOf(errorProducer), future)
    }

    override fun handleMessage(message: Message<JsonObject>) {
        logger.info("Handling message ${message.body()}")
    }

}