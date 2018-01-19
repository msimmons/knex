package net.contrapt.vertx.endpoints

import com.rabbitmq.client.*
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import java.io.IOException
import java.io.UnsupportedEncodingException
import java.nio.charset.Charset
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.function.BiConsumer

class RabbitClient private constructor(val vertx: Vertx, private val connection: RabbitClientConnection) {

    private val logger = LoggerFactory.getLogger(javaClass)

    var includeProperties = true

    private lateinit var defaultChannel: Channel
    private lateinit var publishChannel: Channel
    private val consumerChannels = mutableMapOf<String, Channel>()

    fun start(successHandler: () -> Unit) {
        logger.info("Starting rabbitmq client")
        vertx.executeBlocking(Handler<Future<Unit>> { future ->
            try {
                connect()
                future.complete()
            } catch (e: Exception) {
                logger.error("Could not connect to rabbitmq", e)
                future.fail(e)
            }
        }, startHandler(successHandler))
    }

    private fun startHandler(successHandler: () -> Unit) = Handler<AsyncResult<Unit>> { async ->
        when (async.succeeded()) {
            true -> {
                successHandler()
            }
            false -> {
                logger.warn("Unable to connect to rabbit", async.cause())
                logger.warn("Trying again in 10s")
                vertx.setTimer(10000, {
                    logger.info("Trying again")
                    start(successHandler)
                })
            }
        }
    }

    fun stop(resultHandler: Handler<AsyncResult<Unit>>) {
        logger.info("Stopping rabbitmq client")
        vertx.executeBlocking(Handler<Future<Unit>> { future ->
            try {
                disconnect()
                future.complete()
            } catch (e: IOException) {
                future.fail(e)
            }
        }, resultHandler)
    }

    fun basicAck(consumerTag: String, deliveryTag: Long, multiple: Boolean, resultHandler: Handler<AsyncResult<Unit>>) {
        val channel = consumerChannels.getOrElse(consumerTag, { defaultChannel })
        withChannel(channel, resultHandler, { c ->
            c.basicAck(deliveryTag, multiple)
        })
    }

    fun basicNack(consumerTag: String, deliveryTag: Long, multiple: Boolean, requeue: Boolean, resultHandler: Handler<AsyncResult<Unit>>) {
        val channel = consumerChannels.getOrElse(consumerTag, { defaultChannel })
        withChannel(channel, resultHandler, { c ->
            c.basicNack(deliveryTag, multiple, requeue)
        })
    }

    fun basicConsume(queue: String, address: String, resultHandler: Handler<AsyncResult<String>>) {
        basicConsume(queue, address, true, 0, resultHandler)
    }

    fun basicConsume(queue: String, address: String, autoAck: Boolean, prefetchCount: Int, resultHandler: Handler<AsyncResult<String>>) {
        val channel = connection.createChannel()
        withChannel(channel, resultHandler, { c ->
            c.basicQos(prefetchCount, true)
            val consumerTag = c.basicConsume(queue, autoAck, consumerHandler(c, consumerResultHandler(address)))
            consumerChannels.put(consumerTag, c)
            consumerTag
        })
    }

    private fun consumerResultHandler(address: String) = Handler<AsyncResult<JsonObject>> { ar ->
        if ( ar.succeeded() ) {
            vertx.eventBus().send(address, ar.result())
        } else {
            logger.error("Exception occurred inside rabbitmq service consumer.", ar.cause())
        }
    }

    fun basicGet(queue: String, autoAck: Boolean, resultHandler: Handler<AsyncResult<JsonObject>>) {
        withChannel(defaultChannel, resultHandler, { c ->
            val response = c.basicGet(queue, autoAck)
            if (response == null) {
                JsonObject()
            } else {
                val json = JsonObject()
                populate(json, response.envelope)
                if (includeProperties) {
                    put("properties", toJson(response.props), json)
                }
                put("body", parse(response.props, response.body), json)
                put("messageCount", response.messageCount, json)
                json
            }
        })
    }

    fun basicPublish(exchange: String, routingKey: String, message: JsonObject, resultHandler: Handler<AsyncResult<Unit>>) {
        withChannel(publishChannel, resultHandler, { c ->
            //TODO: Really need an SPI / Interface to decouple this and allow pluggable implementations
            val properties = message.getJsonObject("properties")
            val contentType = if (properties == null) null else properties.getString("contentType")
            val encoding = properties?.getString("contentEncoding")
            val body: ByteArray
            body = when (contentType) {
                null -> encode(encoding, message.getString("body"))
                "application/json" -> encode(encoding, message.getJsonObject("body").toString())
                "application/octet-stream" -> message.getBinary("body")
                "text/plain" -> encode(encoding, message.getString("body"))
                else -> encode(encoding, message.getString("body"))
            }
            c.basicPublish(exchange, routingKey, fromJson(properties), body)
        })
    }

    fun basicQos(prefetchCount: Int, resultHandler: Handler<AsyncResult<Unit>>) {
        withChannel(defaultChannel, resultHandler, { c ->
            c.basicQos(prefetchCount)
        })
    }

    fun exchangeDeclare(exchange: String, type: String, durable: Boolean, autoDelete: Boolean, resultHandler: Handler<AsyncResult<Unit>>) {
        withChannel(defaultChannel, resultHandler, { c ->
            c.exchangeDeclare(exchange, type, durable, autoDelete, null)
        })
    }

    fun exchangeDeclare(exchange: String, type: String, durable: Boolean, autoDelete: Boolean, config: Map<String, String>,
                        resultHandler: Handler<AsyncResult<Unit>>) {
        //convert map
        val transformedMap = HashMap<String, Any>()
        config.forEach(BiConsumer<String, String>({ key, value -> transformedMap.put(key, value) }))

        withChannel(defaultChannel, resultHandler, { c ->
            c.exchangeDeclare(exchange, type, durable, autoDelete, transformedMap)
        })
    }

    fun exchangeDelete(exchange: String, resultHandler: Handler<AsyncResult<Unit>>) {
        withChannel(defaultChannel, resultHandler, { c ->
            c.exchangeDelete(exchange)
        })
    }

    fun exchangeBind(destination: String, source: String, routingKey: String, resultHandler: Handler<AsyncResult<Unit>>) {
        withChannel(defaultChannel, resultHandler, { c ->
            c.exchangeBind(destination, source, routingKey)
        })
    }

    fun exchangeUnbind(destination: String, source: String, routingKey: String, resultHandler: Handler<AsyncResult<Unit>>) {
        withChannel(defaultChannel, resultHandler, { c ->
            c.exchangeUnbind(destination, source, routingKey)
        })
    }

    fun queueDeclareAuto(resultHandler: Handler<AsyncResult<JsonObject>>) {
        withChannel(defaultChannel, resultHandler, { c ->
            val result = c.queueDeclare()
            toJson(result) as JsonObject
        })
    }

    fun queueDeclare(queue: String, durable: Boolean, exclusive: Boolean, autoDelete: Boolean, args: Map<String,Any>, resultHandler: Handler<AsyncResult<JsonObject>>) {
        withChannel(defaultChannel, resultHandler, { c ->
            val result = c.queueDeclare(queue, durable, exclusive, autoDelete, args)
            toJson(result) as JsonObject
        })
    }

    fun queueDelete(queue: String, resultHandler: Handler<AsyncResult<JsonObject>>) {
        withChannel(defaultChannel, resultHandler, { c ->
            val result = c.queueDelete(queue)
            toJson(result) as JsonObject
        })
    }

    fun queueDeleteIf(queue: String, ifUnused: Boolean, ifEmpty: Boolean, resultHandler: Handler<AsyncResult<JsonObject>>) {
        withChannel(defaultChannel, resultHandler, { c ->
            val result = c.queueDelete(queue, ifUnused, ifEmpty)
            toJson(result) as JsonObject
        })
    }

    fun queueBind(queue: String, exchange: String, routingKey: String, resultHandler: Handler<AsyncResult<Unit>>) {
        withChannel(defaultChannel, resultHandler, { c ->
            c.queueBind(queue, exchange, routingKey)
        })
    }

    fun messageCount(queue: String, resultHandler: Handler<AsyncResult<JsonObject>>) {
        withChannel(defaultChannel, resultHandler, { c ->
            val result = c.messageCount(queue)
            JsonObject().put("messageCount", result)
        })
    }


    private fun <T> withChannel(channel: Channel, resultHandler: Handler<AsyncResult<T>>, channelAction: (channel: Channel) -> T) {
        if (!connection.isOpen() || !channel.isOpen) {
            resultHandler.handle(Future.failedFuture("Not connected"))
            return
        }
        vertx.executeBlocking(Handler<Future<T>> { future ->
            try {
                val result = channelAction(channel)
                future.complete(result)
            } catch (t: Throwable) {
                future.fail(t)
            }
        }, resultHandler)
    }

    private fun connect() {
        logger.debug("Connecting to rabbitmq...")
        defaultChannel = connection.createChannel()
        publishChannel = connection.createChannel()
        logger.debug("Connected to rabbitmq !")
    }

    private fun disconnect() {
        try {
            logger.debug("Disconnecting from rabbitmq...")
            // This will close all consumerChannels related to this connection
            defaultChannel.close()
            publishChannel.close()
            consumerChannels.forEach { it.value.close() }
            connection.close()
            logger.debug("Disconnected from rabbitmq !")
        } finally {
            consumerChannels.clear()
        }
    }

    private fun populate(json: JsonObject, envelope: Envelope?) {
        if (envelope == null) return

        put("deliveryTag", envelope.deliveryTag, json)
        put("isRedeliver", envelope.isRedeliver, json)
        put("exchange", envelope.exchange, json)
        put("routingKey", envelope.routingKey, json)
    }

    private fun toJson(queueDeclare: AMQP.Queue.DeclareOk?): JsonObject? {
        if (queueDeclare == null) return null
        val json = JsonObject()
        put("queue", queueDeclare.queue, json)
        put("messageCount", queueDeclare.messageCount, json)
        put("consumerCount", queueDeclare.consumerCount, json)

        return json
    }

    private fun toJson(queueDelete: AMQP.Queue.DeleteOk?): JsonObject? {
        if (queueDelete == null) return null
        val json = JsonObject()
        put("messageCount", queueDelete.messageCount, json)

        return json
    }

    private fun toJson(properties: AMQP.BasicProperties?): JsonObject? {
        if (properties == null) return null

        val json = JsonObject()
        put("contentType", properties.contentType, json)
        put("contentEncoding", properties.contentEncoding, json)
        put("headers", toJsonObject(properties.headers), json)
        put("deliveryMode", properties.deliveryMode, json)
        put("priority", properties.priority, json)
        put("correlationId", properties.correlationId, json)
        put("replyTo", properties.replyTo, json)
        put("expiration", properties.expiration, json)
        put("messageId", properties.messageId, json)
        put("timestamp", properties.timestamp, json)
        put("type", properties.type, json)
        put("userId", properties.userId, json)
        put("appId", properties.appId, json)
        put("clusterId", properties.clusterId, json)

        return json
    }

    private fun toJsonObject(map: Map<String, Any?>?) : JsonObject {
        if ( map == null ) return JsonObject()
        val json = JsonObject()
        map.entries.forEach { put(it.key, it.value, json) }
        return json
    }

    private fun fromJson(json: JsonObject?): AMQP.BasicProperties {
        return if (json == null) AMQP.BasicProperties() else AMQP.BasicProperties.Builder()
            .contentType(json.getString("contentType"))
            .contentEncoding(json.getString("contentEncoding"))
            .headers((json.getJsonObject("headers")?.map))
            .deliveryMode(json.getInteger("deliveryMode"))
            .priority(json.getInteger("priority"))
            .correlationId(json.getString("correlationId"))
            .replyTo(json.getString("replyTo"))
            .expiration(json.getString("expiration"))
            .messageId(json.getString("messageId"))
            .timestamp(parseDate(json.getString("timestamp")))
            .type(json.getString("type"))
            .userId(json.getString("userId"))
            .appId(json.getString("appId"))
            .clusterId(json.getString("clusterId")).build()

    }

    private fun parse(properties: AMQP.BasicProperties?, bytes: ByteArray?): Any? {
        if (bytes == null) return null

        val encoding = properties?.contentEncoding
        val contentType = properties?.contentType
        return when (contentType) {
            //"application/json" -> JsonObject(decode(encoding, bytes))
            "application/octet-stream" -> bytes
            "text/plain" -> decode(encoding, bytes)
            else -> decode(encoding, bytes)
        }
    }

    private fun decode(encoding: String?, bytes: ByteArray): String {
        return if (encoding == null) {
            String(bytes, Charset.forName("UTF-8"))
        } else if ( !Charset.availableCharsets().containsKey(encoding) ) {
            String(bytes, Charset.forName("UTF-8"))
        } else {
            String(bytes, Charset.forName(encoding))
        }
    }

    private fun encode(encoding: String?, string: String): ByteArray {
        return if (encoding == null) {
            string.toByteArray()
        } else if ( !Charset.availableCharsets().containsKey(encoding) ) {
            string.toByteArray()
        } else {
            string.toByteArray(charset(encoding))
        }
    }

    private fun put(field: String, value: Any?, json: JsonObject) {
        when ( value ) {
            null -> return
            is LongString -> json.put(field, String(value.bytes))
            is Date -> put(field, value, json)
            else -> json.put(field, value)
        }
    }

    private fun put(field: String, value: Date?, json: JsonObject) {
        if ( value == null ) return
        val date = OffsetDateTime.ofInstant(value.toInstant(), ZoneId.of("UTC"))
        val format = date.format(dateTimeFormatter)
        json.put(field, format)
    }

    private fun parseDate(date: String?): Date? {
        if (date == null) return null

        val odt = OffsetDateTime.parse(date, dateTimeFormatter)
        return Date.from(odt.toInstant())
    }

    private val dateTimeFormatter = DateTimeFormatter.ISO_DATE_TIME

    private fun consumerHandler(channel: Channel, handler: Handler<AsyncResult<JsonObject>>) = object : DefaultConsumer(channel) {

        override fun handleDelivery(consumerTag: String?, envelope: Envelope?, properties: AMQP.BasicProperties?, body: ByteArray?) {
            val msg = JsonObject()
            msg.put("consumerTag", consumerTag)

            // Add the envelope data
            populate(msg, envelope)

            // Add properties (if configured)
            if (includeProperties) {
                put("properties", toJson(properties), msg)
            }

            //TODO: Allow an SPI which can be pluggable to handle parsing the body
            // Parse the body
            try {
                msg.put("body", parse(properties, body))
                vertx.runOnContext { _ -> handler.handle(Future.succeededFuture(msg)) }

                msg.put("deliveryTag", envelope?.deliveryTag)

            } catch (e: UnsupportedEncodingException) {
                vertx.runOnContext { _ -> handler.handle(Future.failedFuture(e)) }
            }
        }
    }

    companion object {
        @JvmStatic
        fun create(vertx: Vertx, connectionFactory: ConnectionFactory): RabbitClient = RabbitClient(vertx, RabbitClientConnection(connectionFactory))
    }
}