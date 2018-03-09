package net.contrapt.vertek.rabbitmq

import com.rabbitmq.client.AMQP
import org.junit.Assert
import org.junit.Test
import java.util.*

class RabbitPropertyTest {

    @Test
    fun testIt() {
        val properties = AMQP.BasicProperties().builder().contentType("aType").build()
        val property = RabbitProperty.ContentType
        Assert.assertEquals("aType", property.get(properties))
    }

    @Test
    fun testEnum() {
        val properties = AMQP.BasicProperties().builder()
            .contentType("ct")
            .contentEncoding("enc")
            .deliveryMode(1)
            .messageId("id")
            .timestamp(Date()).build()
        val json = FooProperty.toJson(properties)
        val back = FooProperty.fromJson(json)
    }
}