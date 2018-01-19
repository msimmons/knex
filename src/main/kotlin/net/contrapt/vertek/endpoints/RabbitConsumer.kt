package net.contrapt.vertx.endpoints

import com.rabbitmq.client.ConnectionFactory
import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.groovy.rabbitmq.RabbitMQClient_GroovyExtension.basicAck
import io.vertx.groovy.rabbitmq.RabbitMQClient_GroovyExtension.basicNack

/**
 * Consume messages routed by the rabbit broker
 */
abstract class RabbitConsumer(
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
) : AbstractConsumer(), Handler<Message<JsonObject>> {

    lateinit var client : RabbitClient

    final override fun start() {
        client = RabbitClient.create(vertx, connectionFactory)
        client.start({
            logger.info("Rabbit client connected")
            startInternal()
            client.queueDeclare(queue, durable, exclusive, autoDelete, args, bindQueue())
        })
    }

    /**
     * Override this method to start any additional consumers, timers etc when this consumer starts
     */
    abstract fun startInternal()

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

    /**
     * Bind this consumer's [queue] to it's [exchange] and [routingKey]
     */
    private fun bindQueue() = Handler<AsyncResult<JsonObject>> { async ->
        if( async.failed() ) throw IllegalStateException("Failed to declare queue $queue", async.cause())
        client.queueBind(queue, exchange, routingKey, startConsumers())
    }

    /**
     * Setup this consumer's [basicConsume]rs on rabbit as well as the [EventBus] consumer that will handle incoming
     * messages
     */
    private fun startConsumers() = Handler<AsyncResult<Unit>> { async ->
        if ( async.failed() ) throw IllegalStateException("Failed to bind queue $exchange:$routingKey -> $queue", async.cause())
        // Basic consumes bridges rabbit message to the event bus
        (1..consumers).forEach {
            client.basicConsume(queue, queue, autoAck, prefetchLimit, Handler<AsyncResult<String>> { ar -> handleConsumer(ar)})
        }
        // Event bus consumer will pick up the bridged message -- the handler is this concrete subclass
        vertx.eventBus().consumer(queue, this)
    }

    private fun handleConsumer(ar: AsyncResult<String>) {
        when ( ar.succeeded() ) {
            true -> logger.info("Listening to $exchange:$routingKey -> $queue [${ar.result()}]")
            else -> logger.error("Failed to setup consumer $exchange:$routingKey -> $queue", ar.cause())
        }
    }

}
