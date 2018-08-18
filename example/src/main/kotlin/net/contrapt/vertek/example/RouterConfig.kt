package net.contrapt.knex.example

import io.vertx.core.Vertx
import net.contrapt.knex.endpoints.ws.WebSocketConnector
import net.contrapt.knex.endpoints.ws.WebSocketRouter
import net.contrapt.knex.example.route.WsConsumer
import net.contrapt.knex.example.router.MainRouter
import net.contrapt.knex.example.router.RestRouter
import net.contrapt.knex.example.service.LoginService
import net.contrapt.knex.example.service.SignupService
import net.contrapt.knex.example.service.VerifyService
import org.springframework.context.ApplicationContext
import org.springframework.context.support.beans

object RouterConfig {

    fun startup(vertx: Vertx, context: ApplicationContext) {
        val wsRouter = WebSocketRouter(vertx, "/ws/V1", setOf())
        val restRouter = RestRouter(vertx, "/api/V1", context.getBean(SignupService::class.java), context.getBean(VerifyService::class.java), context.getBean(LoginService::class.java))
        vertx.deployVerticle(context.getBean(WsConsumer::class.java))
        val router = MainRouter(listOf(wsRouter.router(), restRouter.router()))
        vertx.deployVerticle(router)
    }

    fun context() = beans {
        bean {
            val connector = WebSocketConnector("example.ws1.whatever")
            WsConsumer(connector)
        }
    }
}