package net.contrapt.vertek.example

import io.vertx.core.Vertx
import io.vertx.core.logging.LoggerFactory
import org.springframework.context.support.GenericApplicationContext
import org.springframework.core.io.support.ResourcePropertySource

class Application {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun run() {
        logger.info("Starting the application")
        val context = GenericApplicationContext().apply {
            environment.propertySources.addLast(ResourcePropertySource("classpath:application.properties"))
            DatabaseConfig.context().initialize(this)
            BrokerConfig.context().initialize(this)
            ServiceConfig.context().initialize(this)
            RouterConfig.context().initialize(this)
            refresh()
        }

        val vertx = Vertx.vertx()
        startup(vertx, context)
    }

    fun startup(vertx: Vertx, context: GenericApplicationContext) {
        // Start database, do migrations
        DatabaseConfig.startup(vertx, context)

        // Start Broker
        BrokerConfig.startup(vertx, context)

        // Start web
        RouterConfig.startup(vertx, context)
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            LogSetter.intitialize("DEBUG")
            Application().run()
        }
    }
}