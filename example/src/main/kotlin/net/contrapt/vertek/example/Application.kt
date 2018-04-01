package net.contrapt.vertek.example

import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.logging.LoggerFactory
import org.springframework.context.support.GenericApplicationContext
import org.springframework.core.io.support.ResourcePropertySource

//@ComponentScan
//@Configuration
class Application {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
    @Bean
    open fun rabbitConnectionFactory() = ConnectionFactory().apply {
    host = "localhost"
    isAutomaticRecoveryEnabled = true
    networkRecoveryInterval = 5000
    }

    @Bean
    open fun simpleConnector() = RabbitConsumerConnector(rabbitConnectionFactory(), "amq.topic", "consumer.test", "consumer.test", durable = false, autoAck = false)

    @Autowired
    lateinit var simpleConsumer: SimpleConsumer
     */

    fun run() {
        logger.info("Starting the application")
        val context = GenericApplicationContext().apply {
            environment.propertySources.addLast(ResourcePropertySource("classpath:application.properties"))
            RouteConfig.context.initialize(this)
            ServiceConfig.context.initialize(this)
            refresh()
        }

        val vertx = Vertx.vertx()
        startup(vertx, context)
    }

    fun startup(vertx: Vertx, context: GenericApplicationContext) {
        context.getBeansOfType(Verticle::class.java).values.forEach {
            logger.info("Deploying ${it::class.qualifiedName}")
            vertx.deployVerticle(it)
        }
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            LogSetter.intitialize("DEBUG")

            //val context = AnnotationConfigApplicationContext(Application::class.java)
            //context.getBean(Application::class.java).run()
            Application().run()
        }
    }
}