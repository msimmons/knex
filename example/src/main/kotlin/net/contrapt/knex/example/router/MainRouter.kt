package net.contrapt.knex.example.router

import io.vertx.core.AbstractVerticle
import io.vertx.ext.web.Router

class MainRouter(private val subRouters: Collection<Router>) : AbstractVerticle() {

    override fun start() {

        val router = Router.router(vertx)
        subRouters.forEach {
            router.mountSubRouter("/example", it)
        }
        val server = vertx.createHttpServer()
        server.requestHandler(router::accept).listen(9000)
    }
}