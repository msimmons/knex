package net.contrapt.vertek.example.router

import io.vertx.core.AsyncResult
import io.vertx.core.Handler
import io.vertx.core.Vertx
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import net.contrapt.vertek.example.service.LoginService
import net.contrapt.vertek.example.service.SignupService
import net.contrapt.vertek.example.service.VerifyService

class RestRouter(
        val vertx: Vertx,
        val path: String,
        val signupService: SignupService,
        val verifyService: VerifyService,
        val loginService: LoginService
) {

    // How about config with path, method and handler? Or invoke handler via event bus?
    // is there a generic EventBusConnector? or a RestConnector
    init {
        vertx.eventBus().consumer<JsonObject>("my.http.address", {
            try {
                it.reply(JsonObject().put("body", it.body().encode()).put("headers", it.headers().map { it.key }))
            }
            catch(e: Exception) {
                it.fail(500, e.message)
            }
        })
    }

    fun router() = Router.router(vertx).apply {
        route().handler(BodyHandler.create())
        //route().failureHandler {  }
        post("$path/signup").blockingHandler { context ->
            val data = context.bodyAsJson.mapTo(SignupService.Request::class.java)
            val user = signupService(data)
            context.response().end(JsonObject.mapFrom(user).encode())
        }
        post("$path/verify").blockingHandler { context ->
            val data = context.bodyAsJson.mapTo(VerifyService.Request::class.java)
            val user = verifyService(data)
            context.response().end(JsonObject.mapFrom(user).encode())
        }
        post("$path/login").blockingHandler { context ->
            val data = context.bodyAsJson.mapTo(LoginService.Request::class.java)
            val user = loginService(data)
            context.response().end(JsonObject.mapFrom(user).encode())
        }
        route(HttpMethod.GET, "$path/whatever").handler { context ->
            val body = context.bodyAsString
            val options = DeliveryOptions().apply {
                headers = context.request().headers()
            }
            vertx.eventBus().send("my.http.address", JsonObject().put("body", body), options, Handler<AsyncResult<Message<JsonObject>>> { reply ->
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