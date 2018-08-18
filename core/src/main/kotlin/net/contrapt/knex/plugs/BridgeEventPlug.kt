package net.contrapt.knex.plugs

import io.vertx.ext.web.handler.sockjs.BridgeEvent

interface BridgeEventPlug : Plug<BridgeEvent>