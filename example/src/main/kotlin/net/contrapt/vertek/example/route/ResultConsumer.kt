package net.contrapt.knex.example.route

import io.vertx.core.Future
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import net.contrapt.knex.endpoints.AbstractConsumer
import net.contrapt.knex.endpoints.ConsumerConnector
import net.contrapt.knex.endpoints.ProducerConnector
import net.contrapt.knex.example.plugs.InboundProcessor
import net.contrapt.knex.example.plugs.Tracer
import net.contrapt.knex.example.service.ResultService

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

    init {
        addPlug(InboundProcessor())
        addPlug(Tracer().inbound)
    }

    override fun beforeConnector(future: Future<Unit>) {
        addExceptionHandler(DefaultExceptionHandler(errorProducer))
        deployVerticles(arrayOf(errorProducer, resultProducer), future)
    }

    override fun handleMessage(message: Message<JsonObject>) {
        val user = resultService.doSomething()
        val userObj = JsonObject.mapFrom(user)
        vertx.eventBus().send("example.simple", userObj)
        message.body().getJsonObject("body").put("user", userObj)
        resultProducer.send(message)
    }

}