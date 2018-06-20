package net.contrapt.vertek.example

import com.rabbitmq.client.ConnectionFactory
import io.vertx.core.Vertx
import net.contrapt.vertek.endpoints.AbstractEndpoint
import net.contrapt.vertek.example.route.ResultConsumer
import net.contrapt.vertek.example.route.SimpleConsumer
import net.contrapt.vertek.example.route.SimpleProducer
import net.contrapt.vertek.rabbitmq.RabbitConsumerConnector
import net.contrapt.vertek.rabbitmq.RabbitProducerConnector
import org.springframework.context.ApplicationContext
import org.springframework.context.support.beans

object BrokerConfig {

    fun startup(vertx: Vertx, context: ApplicationContext) {
        context.getBeansOfType(AbstractEndpoint::class.java).values.forEach {
            //logger.info("Deploying ${it::class.qualifiedName}")
            vertx.deployVerticle(it)
        }
    }

    fun context() = beans {
        val actor = "example"

        bean {
            ConnectionFactory().apply {
                host = env.getProperty("rabbit.host", "localhost")
                isAutomaticRecoveryEnabled = true
                networkRecoveryInterval = 5000
            }
        }

        bean("error") {
            RabbitProducerConnector(ref(), "amq.topic", "${actor}.created.error")
        }

        bean {
            val connector = RabbitConsumerConnector(ref(), "amq.topic", "#.created.simple", "${actor}.simple")
            SimpleConsumer(connector, ref("error"), ref())
        }

        bean {
            val connector = RabbitConsumerConnector(ref(), "amq.topic", "#.created.question", "${actor}.question")
            val resultConnector = RabbitProducerConnector(ref(), "amq.topic", "${actor}.created.result")
            ResultConsumer(connector, resultConnector, ref("error"), ref())
        }

        bean {
            val connector = RabbitProducerConnector(ref(), "amq.topic", "${actor}.created.foo")
            SimpleProducer(connector)
        }

    }
}