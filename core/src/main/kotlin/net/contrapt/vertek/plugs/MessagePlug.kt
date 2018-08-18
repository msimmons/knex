package net.contrapt.knex.plugs

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject

interface MessagePlug : Plug<Message<JsonObject>>