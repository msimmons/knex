package net.contrapt.vertx.plugs

import io.vertx.ext.web.handler.sockjs.BridgeEvent

interface ChannelPlug : Plug<BridgeEvent>