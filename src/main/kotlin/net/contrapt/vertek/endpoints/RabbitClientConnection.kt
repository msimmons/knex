package net.contrapt.vertx.endpoints

import com.rabbitmq.client.*
import io.vertx.core.logging.LoggerFactory
import java.lang.Thread.sleep

/**
 * A given instance of this connection will return the same connection to all clients
 */
class RabbitClientConnection(val factory: ConnectionFactory, val retryLimit: Int = 0) : ShutdownListener {

    private val log = LoggerFactory.getLogger(javaClass)
    private var connectionAssigned = false
    private lateinit var connection : Connection
    private var retryCount = 0

    override fun shutdownCompleted(cause: ShutdownSignalException?) {
        if ( cause?.isInitiatedByApplication == true ) {
            return
        }
        log.info("RabbitMQ connection shutdown! The client will attempt to reconnect automatically", cause)
    }

    private fun connect() {
        if ( connectionAssigned && connection.isOpen ) return
        try {
            log.info("Attempting to connect to RabbitMQ Broker at ${factory.host}")
            connection = factory.newConnection()
            connectionAssigned = true
            connection.addShutdownListener(this)
        }
        catch(e: Exception ) {
            log.warn("Unable to connect to RabbitMQ Broker at ${factory.host}", e)
            retryCount++
            if ( retryLimit > 0 && retryCount > retryLimit ) {
                retryCount = 0
                throw IllegalStateException("Retry limit of $retryLimit is exceeded, giving up")
            }
            log.warn("Trying again in ${factory.networkRecoveryInterval} ms")
            sleep(factory.networkRecoveryInterval)
            connect()
        }
    }

    fun isOpen() : Boolean {
        if ( !connectionAssigned ) return false
        return connection.isOpen
    }

    fun createChannel() : Channel {
        connect()
        return connection.createChannel()
    }

    fun close() {
        if ( !connectionAssigned ) return
        connection.close()
    }

}