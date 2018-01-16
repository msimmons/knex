package net.contrapt.vertx.channels

import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory

abstract class Channel(val channel: String) : AbstractVerticle() {

    val logger = LoggerFactory.getLogger(javaClass)

    abstract val endpoints : Set<ChannelConfig>

    private val endpointMap = mutableMapOf<String, ChannelConfig>()

    override fun start() {
        logger.info("Starting channel '$channel'")
        endpointMap.putAll(endpoints.associate { endpoint->
            Pair(endpointKey(endpoint.method, endpoint.path), endpoint)
        })
        vertx.eventBus().consumer<JsonObject>(channel, { message: Message<JsonObject> ->
            logger.info("Got message ${message.body()?.encode()}")
            val uid = message.headers().get("uid")
            val method = message.headers()["method"]
            val path = message.headers()["path"]
            logger.info("UID: ${uid} $method $path")
            val endpoint = findEndpoint(method, path)
            when (endpoint) {
                null -> message.fail(404, "No endpoint defined for $channel $method $path")
                else -> try {
                    logger.info("Calling handler ${endpoint.handler} ${Thread.currentThread()}")
                    vertx.executeBlocking(Handler<Future<Nothing>> { future ->
                        endpoint.handler.handle(message)
                        future.complete()
                    }, false, Handler<AsyncResult<Nothing>> {ar ->
                        if ( ar.failed() ) message.fail(500, "Unhandled exception in handler: ${ar.cause()}")
                    })
                }
                catch( e: Exception) {
                    message.fail(500, "Unhandled exception in handler: $e")
                }
            }
        })
    }

    private fun findEndpoint(method: String, path: String) : ChannelConfig? {
        return endpointMap[endpointKey(method,path)]
    }

    private fun endpointKey(method: String, path: String) = "$method $path"

}