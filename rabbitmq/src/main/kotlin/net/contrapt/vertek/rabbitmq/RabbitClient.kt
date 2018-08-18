package net.contrapt.knex.rabbitmq

import com.rabbitmq.client.*
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import java.io.IOException
import java.nio.charset.Charset
import java.util.*
import java.util.function.BiConsumer

class RabbitClient private constructor(val vertx: Vertx, private val connection: RabbitClientConnection) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private lateinit var defaultChannel: Channel
    private lateinit var publishChannel: Channel
    private val consumerChannels = mutableMapOf<String, Channel>()

    fun start(startupHandler: Handler<AsyncResult<Unit>>) {
        logger.debug("Starting RabbitClient")
        vertx.executeBlocking(Handler { future ->
            try {
                connect()
                future.complete()
            } catch (e: Exception) {
                logger.error("Error starting RabbitClient", e)
                future.fail(e)
            }
        }, startupHandler)
    }

    fun stop(stopHandler: Handler<AsyncResult<Unit>>) {
        logger.debug("Stopping RabbitClient")
        vertx.executeBlocking(Handler { future ->
            try {
                disconnect()
                future.complete()
            } catch (e: IOException) {
                future.fail(e)
            }
        }, stopHandler)
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
            toJson(response)
        })
    }

    fun basicPublish(exchange: String, routingKey: String, message: JsonObject, resultHandler: Handler<AsyncResult<Unit>>) {
        withChannel(publishChannel, resultHandler, { c ->
            val properties = fromJson(message.getJsonObject("properties"))
            val body = encodeBody(message.getString("body"))
            c.basicPublish(exchange, routingKey, properties, body)
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
        logger.debug("Creating default channels...")
        defaultChannel = connection.createChannel()
        publishChannel = connection.createChannel()
        logger.debug("Channels created")
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

    /**
     * Convert a [GetResponse] to a [JsonObject] for placing on the [EventBus] as a [Message]
     */
    private fun toJson(response: GetResponse?) : JsonObject {
        return when ( response ) {
            null -> JsonObject()
            else -> JsonObject().apply {
                addToMessage(this, response.envelope)
                addToMessage(this, response.props)
                put("body", decodeBody(response.body))
                put("messageCount", response.messageCount)
            }
        }
    }

    private fun addToMessage(message: JsonObject, envelope: Envelope?) {
        if ( envelope == null ) return
        message.apply {
            put("deliveryTag", envelope.deliveryTag)
            put("isRedeliver", envelope.isRedeliver)
            put("exchange", envelope.exchange)
            put("routingKey", envelope.routingKey)
        }
    }

    /**
     * Add the rabbit properties to the [JsonObject] message
     */
    private fun addToMessage(message: JsonObject, properties: AMQP.BasicProperties?) {
        if ( properties == null ) return
        val json = RabbitProperty.toJson(properties)
        message.put("properties", json)
    }

    private fun decodeBody(body: ByteArray?) : String {
        return body?.toString(Charset.defaultCharset()) ?: ""
    }

    private fun encodeBody(body: String?) : ByteArray {
        return body?.toByteArray(Charset.defaultCharset()) ?: ByteArray(0)
    }

    private fun toJson(queueDeclare: AMQP.Queue.DeclareOk?): JsonObject? {
        if (queueDeclare == null) return null
        return JsonObject().apply {
            put("queue", queueDeclare.queue)
            put("messageCount", queueDeclare.messageCount)
            put("consumerCount", queueDeclare.consumerCount)
        }
    }

    private fun toJson(queueDelete: AMQP.Queue.DeleteOk?): JsonObject? {
        if (queueDelete == null) return null
        return JsonObject().apply {
            put("messageCount", queueDelete.messageCount)
        }
    }

    private fun fromJson(json: JsonObject?): AMQP.BasicProperties {
        return if (json == null) AMQP.BasicProperties() else RabbitProperty.fromJson(json)
    }

    private fun consumerHandler(channel: Channel, handler: Handler<AsyncResult<JsonObject>>) = object : DefaultConsumer(channel) {

        override fun handleDelivery(consumerTag: String?, envelope: Envelope?, properties: AMQP.BasicProperties?, body: ByteArray?) {
            val message = JsonObject().apply {
                addToMessage(this, envelope)
                addToMessage(this, properties)
                this.put("consumerTag", consumerTag)
                this.put("body", decodeBody(body))
            }
            vertx.runOnContext { _ -> handler.handle(Future.succeededFuture(message)) }
        }
    }

    companion object {
        @JvmStatic
        fun create(vertx: Vertx, connectionFactory: ConnectionFactory): RabbitClient = RabbitClient(vertx, RabbitClientConnection(connectionFactory))
    }
}