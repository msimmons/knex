package net.contrapt.vertek.endpoints.ws

import net.contrapt.vertek.endpoints.AbstractConsumer

/**
 * Base class for web socket consumers
 */
abstract class WebSocketConsumer(address: String) : AbstractConsumer(WebSocketConnector(address))
