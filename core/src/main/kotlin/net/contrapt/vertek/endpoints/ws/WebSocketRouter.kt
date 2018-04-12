package net.contrapt.vertek.endpoints.ws

import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.bridge.BridgeEventType
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.kotlin.ext.web.handler.sockjs.BridgeOptions
import io.vertx.kotlin.ext.web.handler.sockjs.PermittedOptions
import io.vertx.kotlin.ext.web.handler.sockjs.SockJSHandlerOptions
import net.contrapt.vertek.endpoints.AbstractConsumer
import net.contrapt.vertek.plugs.BridgeEventPlug

class WebSocketRouter(
        val vertx: Vertx,
        val path: String,
        val consumers: Collection<AbstractConsumer>
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val inboundPlugs = mutableListOf<BridgeEventPlug>()
    private val outboundPlugs = mutableListOf<BridgeEventPlug>()

    private val sockJSOptions = SockJSHandlerOptions().apply {
        sessionTimeout = 600000
    }
    private val requestChannel = PermittedOptions(
        addressRegex = ".*"
    )
    private val responseChannel = PermittedOptions(
        addressRegex = ".*"
    )
    private val bridgeOptions = BridgeOptions(
        inboundPermitteds = listOf(requestChannel),
        outboundPermitted = listOf(responseChannel)
    )


    fun router() = Router.router(vertx).apply {
        route("$path/*").handler(preHandler())
        route("$path/*").handler(sockJSHandler())
    }


    private fun preHandler() = Handler<RoutingContext> { context ->
        // Is there anything to do here?
        logger.debug("preHandler for ${context.normalisedPath()}")
        context.next()
    }

    private fun bridgeEventHandler() = Handler<BridgeEvent> { event ->
        when (event.type()) {
            BridgeEventType.SOCKET_PING -> {}
            BridgeEventType.RECEIVE -> processOutbound(event)
            BridgeEventType.SEND -> processInbound(event)
            BridgeEventType.PUBLISH -> processInbound(event)
            else -> logger.debug("${event.type()} ${event.socket().headers().entries()} ${event.rawMessage?.encode()}")
        }
        /*
        val wsKey = event.socket().headers()["Sec-WebSocket-Key"]
        event.rawMessage?.getJsonObject("headers")?.put("wsKey", wsKey)
        */
        event.complete(true)
    }

    private fun sockJSHandler() = SockJSHandler.create(vertx, sockJSOptions).bridge(bridgeOptions, bridgeEventHandler())

    private fun processOutbound(event: BridgeEvent) {
        outboundPlugs.forEach {
            it.process(event)
        }
    }

    private fun processInbound(event: BridgeEvent) {
        inboundPlugs.forEach {
            it.process(event)
        }
    }
}