package net.contrapt.knex.rabbitmq

import com.rabbitmq.client.*
import com.rabbitmq.client.impl.recovery.AutorecoveringConnection
import io.vertx.core.logging.LoggerFactory
import java.lang.Thread.sleep

/**
 * A given instance of [RabbitClientConnection] manages a single [Connection] from which a [RabbitClient] can obtain
 * it's [Channel]s
 */
class RabbitClientConnection(val factory: ConnectionFactory, val retryLimit: Int = 0) : ShutdownListener, RecoveryListener {

    private val logger = LoggerFactory.getLogger(javaClass)
    private lateinit var connection : Connection
    private var retryCount = 0

    override fun shutdownCompleted(cause: ShutdownSignalException?) {
        if ( cause?.isInitiatedByApplication == true ) {
            return
        }
        logger.warn("RabbitMQ connection shutdown! The client will attempt to reconnect automatically", cause)
    }

    override fun handleRecovery(recoverable: Recoverable?) {
        logger.info("Connection recovered for $recoverable")
    }

    override fun handleRecoveryStarted(recoverable: Recoverable?) {
        logger.info("Automatic connection recovery started for $recoverable")
    }

    private fun connect() {
        if ( ::connection.isInitialized && connection.isOpen ) return
        try {
            logger.debug("Attempting to connect to RabbitMQ Broker at ${factory.host}")
            connection = factory.newConnection().apply {
                when (this) {
                    is AutorecoveringConnection -> this.addRecoveryListener(this@RabbitClientConnection)
                }
                addShutdownListener(this@RabbitClientConnection)
            }
        }
        catch(e: Exception ) {
            logger.warn("Unable to connect to RabbitMQ Broker at ${factory.host}", e)
            retryCount++
            if ( retryLimit > 0 && retryCount > retryLimit ) {
                retryCount = 0
                throw IllegalStateException("Retry limit of $retryLimit is exceeded, giving up")
            }
            logger.warn("Trying again in ${factory.networkRecoveryInterval} ms")
            sleep(factory.networkRecoveryInterval)
            connect()
        }
    }

    fun isOpen() : Boolean {
        if (!::connection.isInitialized) return false
        return connection.isOpen
    }

    fun createChannel() : Channel {
        connect()
        return connection.createChannel()
    }

    fun close() {
        if (!::connection.isInitialized) return
        connection.close()
    }

}