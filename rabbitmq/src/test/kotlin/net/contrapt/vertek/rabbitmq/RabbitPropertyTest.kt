package net.contrapt.vertek.rabbitmq

import com.rabbitmq.client.AMQP
import io.vertx.core.json.JsonObject
import org.junit.Assert
import org.junit.Test
import java.time.Instant
import java.util.*

class RabbitPropertyTest {

    @Test
    fun testFromProperties() {
        val properties = AMQP.BasicProperties().builder()
            .contentType("ct")
            .contentEncoding("enc")
            .deliveryMode(1)
            .messageId("id")
            .timestamp(Date())
            .headers(mapOf("key" to "value", "int" to 1))
            .build()
        val json = RabbitProperty.toJson(properties)
        Assert.assertEquals(properties.contentType, json.getString(RabbitProperty.CONTENT_TYPE))
        Assert.assertEquals(properties.contentEncoding, json.getString(RabbitProperty.CONTENT_ENCODING))
        Assert.assertEquals(properties.deliveryMode, json.getInteger(RabbitProperty.DELIVERY_MODE))
        Assert.assertEquals(properties.messageId, json.getString(RabbitProperty.MESSAGE_ID))
        Assert.assertEquals(properties.timestamp, Date.from(json.getInstant(RabbitProperty.TIMESTAMP)))
        Assert.assertEquals(properties.headers, json.getJsonObject(RabbitProperty.HEADERS).map)
    }

    @Test
    fun testFromJson() {
        val json = JsonObject().apply {
            put(RabbitProperty.CONTENT_TYPE, "ct")
            put(RabbitProperty.CONTENT_ENCODING, "enc")
            put(RabbitProperty.DELIVERY_MODE, 1)
            put(RabbitProperty.TIMESTAMP, Instant.now())
            put(RabbitProperty.HEADERS, JsonObject().put("key", "value").put("int", 1).put("time", Instant.now()))

        }
        val properties = RabbitProperty.fromJson(json)
        Assert.assertEquals(properties.contentType, json.getString(RabbitProperty.CONTENT_TYPE))
        Assert.assertEquals(properties.contentEncoding, json.getString(RabbitProperty.CONTENT_ENCODING))
        Assert.assertEquals(properties.deliveryMode, json.getInteger(RabbitProperty.DELIVERY_MODE))
        Assert.assertEquals(properties.messageId, json.getString(RabbitProperty.MESSAGE_ID))
        Assert.assertEquals(properties.timestamp, Date.from(json.getInstant(RabbitProperty.TIMESTAMP)))
        Assert.assertEquals(properties.headers, json.getJsonObject(RabbitProperty.HEADERS).map)
    }
}