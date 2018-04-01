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
 * Consume messages routed by the rabbit broker and bridge them to the [EventBus] at the address defined by [queue]
 * The supplied [messageHandler] will subscribe to the bus and handle the delivered message
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

    override val address = queue

    private val logger = LoggerFactory.getLogger(javaClass)

    override fun start(vertx: Vertx, messageHandler: Handler<Message<JsonObject>>, started: Future<Unit>) {
        synchronized(this, {
            if (::client.isInitialized) {
                started.complete()
                return
            }
            client = RabbitClient.create(vertx, connectionFactory)
        })
        this.vertx = vertx
        this.messageHandler = messageHandler
        doStartup(started)
    }

    /**
     * Start the client, declare and bind queues and start consumers
     */
    private fun doStartup(started: Future<Unit>) {
        val start = Future.future<Unit>()
        val finish = Future.future<Unit>()
        finish.setHandler {
            if (it.failed()) {
                started.fail(it.cause())
            }
        }
        client.start(start.completer())
        start.compose {
            logger.info("Rabbit client connected at ${connectionFactory.host}")
            val declare = Future.future<JsonObject>()
            client.queueDeclare(queue, durable, exclusive, autoDelete, args, declare.completer())
            declare
        }.compose {
            logger.debug("Queue declared")
            val bind = Future.future<Unit>()
            client.queueBind(queue, exchange, routingKey, bind.completer())
            bind
        }.compose {
            logger.debug("$queue bound to $exchange:$routingKey")
            // Event bus consumer will pick up the bridged message using the handler provided
            vertx.eventBus().consumer(queue, messageHandler)
            // Basic consumes bridges rabbit message to the event bus
            val consume = Future.future<String>()
            (1..consumers).forEach {
                client.basicConsume(queue, queue, autoAck, prefetchLimit, consume.completer())
            }
            consume
        }.compose ({
            logger.info("Consuming $exchange:$routingKey -> $queue [$it]")
            started.complete()
        }, finish)
    }

    /**
     * Publishes a message to this [Consumer]'s exchange and routing key.  It will be then be consumed
     * by this consumer
     */
    fun publish(message: JsonObject, handler: Handler<AsyncResult<Unit>>) {
        client.basicPublish(exchange, routingKey, message, handler)
    }

    /**
     * Failure handling does [basicNack] of the message if [autoAck] is not enabled
     */
    override fun handleFailure(message: Message<JsonObject>, cause: Throwable) {
        basicNack(message, cause)
    }

    /**
     * Success handling does [basicAck] if necessary
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
