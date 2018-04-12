package net.contrapt.vertek.example.router

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router

class RestRouter(val vertx: Vertx, val path: String)  {

    // How about config with path, method and handler? Or invoke handler via event bus?
    // is there a generic EventBusConnector? or a RestConnector
    init {
        vertx.eventBus().consumer<JsonObject>("my.rest.address", {
            try {
                it.reply(JsonObject().put("body", it.body().encode()).put("headers", it.headers().map { it.key }))
            }
            catch(e: Exception) {
                it.fail(500, e.message)
            }
        })
    }

    fun router() = Router.router(vertx).apply {
        //route().failureHandler {  }
        route("$path/something").blockingHandler {
            it.response().end("else")
        }
        route(HttpMethod.GET, "$path/whatever").handler { context ->
            val body = context.bodyAsString
            val options = DeliveryOptions().apply {
                headers = context.request().headers()
            }
            vertx.eventBus().send("my.rest.address", JsonObject().put("body", body), options, Handler<AsyncResult<Message<JsonObject>>> { reply ->
                if (reply.failed()) {
                    context.fail(reply.cause())
                }
                else {
                    context.response().end(reply.result().body().encode())
                }
            })
        }
    }
}