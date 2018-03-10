package net.contrapt.vertek.endpoints.ws

import io.vertx.core.AbstractVerticle
import io.vertx.core.Handler
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.bridge.BridgeEventType
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.sockjs.BridgeEvent
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.kotlin.ext.web.handler.sockjs.BridgeOptions
import io.vertx.kotlin.ext.web.handler.sockjs.PermittedOptions
import io.vertx.kotlin.ext.web.handler.sockjs.SockJSHandlerOptions
import net.contrapt.vertek.plugs.BridgeEventPlug

class WebSocketRouter(
    val port: Int,
    val path: String,
    val consumers: Collection<WebSocketConsumer>
) : AbstractVerticle() {

    val logger = LoggerFactory.getLogger(javaClass)

    private val inboundPlugs = mutableListOf<BridgeEventPlug>()
    private val outboundPlugs = mutableListOf<BridgeEventPlug>()

    val sockJsOptions = SockJSHandlerOptions().apply {
        //heartbeatInterval = 5000
        //isInsertJSESSIONID = false
        sessionTimeout = 600000
    }
    val requestChannel = PermittedOptions(
        addressRegex = ".*"
    )
    val responseChannel = PermittedOptions(
        addressRegex = ".*"
    )
    val bridgeOptions = BridgeOptions(
        inboundPermitteds = listOf(requestChannel),
        outboundPermitted = listOf(responseChannel)
    )

    override fun start() {

        // Start all the consumers
        consumers.forEach {
            vertx.deployVerticle(it)
        }

        val sockJsHandler = SockJSHandler.create(vertx, sockJsOptions).bridge(bridgeOptions, bridgeEventHandler())
        val http = vertx.createHttpServer()
        val router = Router.router(vertx).apply {
            route("${path}/*").handler(preHandler())
            route("${path}/*").handler(sockJsHandler)
        }
        http.requestHandler{router.accept(it)}.listen(port)
    }

    private fun preHandler() = Handler<RoutingContext> { context ->
        logger.debug("preHandler for ${context.normalisedPath()} ${context.parsedHeaders()}")
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