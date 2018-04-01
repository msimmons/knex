package net.contrapt.vertek.example.route

import io.vertx.core.Future
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import net.contrapt.vertek.endpoints.AbstractConsumer
import net.contrapt.vertek.endpoints.ConsumerConnector
import net.contrapt.vertek.endpoints.ProducerConnector
import net.contrapt.vertek.example.service.ResultService

/**
 * A consumer that processes a message and produces a result
 */
class ResultConsumer (
        connector: ConsumerConnector,
        resultConnector: ProducerConnector,
        errorConnector: ProducerConnector,
        private val resultService: ResultService
) : AbstractConsumer(connector) {

    private val resultProducer = ResultProducer(resultConnector)
    private val errorProducer = ErrorProducer(errorConnector)

    override fun beforeConnector(future: Future<Unit>) {
        addExceptionHandler(DefaultExceptionHandler(errorProducer))
        deployVerticles(arrayOf(errorProducer, resultProducer), future)
    }

    override fun handleMessage(message: Message<JsonObject>) {
        logger.info("Handling message $message")
        resultService.doSomething()
        resultProducer.send(message)
    }

}