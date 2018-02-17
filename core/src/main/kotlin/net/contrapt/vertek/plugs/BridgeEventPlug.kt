package net.contrapt.vertek.plugs

import io.vertx.ext.web.handler.sockjs.BridgeEvent

interface BridgeEventPlug : Plug<BridgeEvent>