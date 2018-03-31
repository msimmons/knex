package net.contrapt.vertek.rabbitmq

import com.rabbitmq.client.ConnectionFactory
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import net.contrapt.vertek.endpoints.ConsumerConnector

/**
 * Consume messages routed by the rabbit broker
 */
class RabbitConsumerConnector(
    val connectionFactory: ConnectionFactory,
    val exchange: String,
    val routingKey: String,
    val queue: String,
    val durable: Boolean = true,
    val exclusive: Boolean = false,
    val autoDelete: Boolean = false,
    val autoAck: Boolean = true,
    val prefetchLimit: Int = 0,
    val consumers: Int = 1,
    val args: Map<String,Any> = mapOf()
) : ConsumerConnector {

    private lateinit var client : RabbitClient
    private lateinit var vertx: Vertx
    private lateinit var messageHandler: Handler<Message<JsonObject>>
    private lateinit var startHandler: Handler<AsyncResult<Unit>>

    override val address = queue

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun start(vertx: Vertx, messageHandler: Handler<Message<JsonObject>>, startHandler: Handler<AsyncResult<Unit>>) {
        this.vertx = vertx
        this.messageHandler = messageHandler
        this.startHandler = startHandler
        client = RabbitClient.create(vertx, connectionFactory)
        client.start(clientStartupHandler())
    }

    /**
     * Handler for [RabbitClient] startup --
     */
    private fun clientStartupHandler() = Handler<AsyncResult<Unit>> { ar ->
        if ( ar.succeeded() ) {
            logger.info("Rabbit client connected at ${connectionFactory.host}")
            client.queueDeclare(queue, durable, exclusive, autoDelete, args, bindQueue())
        }
        else {
            startHandler.handle(Future.failedFuture(ar.cause()))
        }
    }

    /**
     * Bind this consumer's [queue] to it's [exchange] and [routingKey]
     */
    private fun bindQueue() = Handler<AsyncResult<JsonObject>> { ar ->
        if( ar.failed() ) {
            logger.error(ar.cause())
            startHandler.handle(Future.failedFuture(ar.cause()))
        }
        else {
            client.queueBind(queue, exchange, routingKey, startConsumers())
        }
    }

    /**
     * Setup this consumer's [basicConsumer]s on rabbit as well as the [EventBus] consumer that will handle incoming
     * messages
     */
    private fun startConsumers() = Handler<AsyncResult<Unit>> { ar ->
        if ( ar.failed() ) {
            startHandler.handle(Future.failedFuture(ar.cause()))
        }
        else {
            // Event bus consumer will pick up the bridged message using the handler provided
            vertx.eventBus().consumer(queue, messageHandler)
            // Basic consumes bridges rabbit message to the event bus
            (1..consumers).forEach {
                client.basicConsume(queue, queue, autoAck, prefetchLimit, Handler { ar -> handleConsumerStartup(ar) })
            }
        }
    }

    /**
     * Handle consumer startup by signaling success or failure on the passed in [startHandler]
     */
    private fun handleConsumerStartup(ar: AsyncResult<String>) {
        when ( ar.succeeded() ) {
            true -> {
                logger.info("Listening to $exchange:$routingKey -> $queue [${ar.result()}]")
                startHandler.handle(Future.succeededFuture())
            }
            else -> {
                logger.error("Failed to setup consumer $exchange:$routingKey -> $queue", ar.cause())
                startHandler.handle(Future.failedFuture(ar.cause()))
            }
        }
    }

    /**
     * Publishes a message to this [Consumer]'s exchange and routing key.  It will be then be consumed
     * by this consumer
     */
    fun send(message: JsonObject, handler: Handler<AsyncResult<Unit>>) {
        client.basicPublish(exchange, routingKey, message, handler)
    }

    /**
     * Default failure handling does [basicNack] of the message
     */
    override fun handleFailure(message: Message<JsonObject>, cause: Throwable) {
        basicNack(message, cause)
    }

    /**
     * Default success handling does [basicAck] if necessary
     */
    override fun handleSuccess(message: Message<JsonObject>) {
        basicAck(message)
    }

    /**
     * Send [basicAck] if this consumer is not set to [autoAck]
     */
    private fun basicAck(message: Message<JsonObject>) {
        when (autoAck) {
            false -> {
                val consumerTag = message.body().getString("consumerTag")
                client.basicAck(consumerTag, message.body().getLong("deliveryTag"), false, Handler<AsyncResult<Unit>> {ar ->
                    if ( ar.failed() ) logger.error(ar.cause())
                })
            }
        }
    }

    /**
     * Send [basicNack] if this consumer is not set to [autoAck]
     */
    private fun basicNack(message: Message<JsonObject>, exception: Throwable) {
        when (autoAck) {
            false -> {
                val consumerTag = message.body().getString("consumerTag")
                client.basicNack(consumerTag, message.body().getLong("deliveryTag"), false, true, Handler<AsyncResult<Unit>> {ar ->
                    if ( ar.failed() ) logger.error(ar.cause())
                })
                logger.error(exception)
            }
        }
    }

}
