package net.contrapt.knex.example.plugs

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import net.contrapt.knex.plugs.MessagePlug
import net.contrapt.knex.rabbitmq.RabbitProperty
import java.time.Instant

class Tracer {

    private val logger = LoggerFactory.getLogger(javaClass)

    val inbound = object : MessagePlug {
        override fun process(message: Message<JsonObject>) {
            val routingKey = message.body().getString("routingKey")
            val properties = message.body().getJsonObject("properties", JsonObject())
            val headers = properties.getJsonObject("headers", JsonObject())
            when (headers.getString("trace_id")) {
                null -> initTrace(routingKey, properties, headers)
                else -> startTrace(properties, headers)
            }
            properties.put("headers", headers)
            message.body().put("properties", properties)
        }

    }

    val outbound = object : MessagePlug {
        override fun process(message: Message<JsonObject>) {
            val properties = message.body().getJsonObject("properties", JsonObject())
            val headers = properties.getJsonObject("headers", JsonObject())
            endTrace(properties, headers)
            properties.put("headers", headers)
            message.body().put("properties", properties)
        }
    }

    private fun initTrace(routingKey: String, properties: JsonObject, headers: JsonObject) {
        headers.put("trace_id", properties.getString(RabbitProperty.CORRELATION_ID))
        headers.put("trace_name", routingKey)
        startTrace(properties, headers)
    }

    private fun startTrace(properties: JsonObject, headers: JsonObject) {
        headers.put("trace_start", Instant.now().toEpochMilli())
        headers.put("trace_service", "some service")
        headers.put("trace_resource", "some resource")
    }

    private fun endTrace(properties: JsonObject, headers: JsonObject) {
        val start = headers.getLong("trace_start")
        headers.put("trace_duration", Instant.now().toEpochMilli() - start)
        logTrace(headers)
        headers.remove("trace_start")
        headers.remove("trace_duration")
    }

    private fun logTrace(headers: JsonObject) {
        val traceId = headers.getString("trace_id")
        val traceName = headers.getString("trace_name")
        val start = headers.getLong("trace_start")
        val duration = headers.getLong("trace_duration")
        logger.info("TRACE traceId=$traceId traceName=$traceName start=$start duration=$duration")
    }
}