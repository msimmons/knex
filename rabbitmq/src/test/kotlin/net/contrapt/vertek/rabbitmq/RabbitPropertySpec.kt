package net.contrapt.vertek.rabbitmq

import com.rabbitmq.client.AMQP
import io.kotlintest.matchers.shouldBe
import io.kotlintest.specs.BehaviorSpec
import io.vertx.core.json.JsonObject
import java.time.Instant
import java.util.*

class `properties to json` : BehaviorSpec({
    Given("Some AMQP.BasicProperties") {
        val properties = AMQP.BasicProperties().builder()
                .contentType("ct")
                .contentEncoding("enc")
                .deliveryMode(1)
                .messageId("id")
                .timestamp(Date())
                .headers(mapOf("key" to "value", "int" to 1))
                .build()

        When("They are converted to a JsonObject") {
            val json = RabbitProperty.toJson(properties)

            Then("The json attributes are set correctly") {
                json.getString(RabbitProperty.CONTENT_TYPE) shouldBe properties.contentType
                json.getString(RabbitProperty.CONTENT_ENCODING) shouldBe properties.contentEncoding
                json.getInteger(RabbitProperty.DELIVERY_MODE) shouldBe properties.deliveryMode
                json.getString(RabbitProperty.MESSAGE_ID) shouldBe properties.messageId
                Date.from(json.getInstant(RabbitProperty.TIMESTAMP)) shouldBe properties.timestamp
                json.getJsonObject(RabbitProperty.HEADERS).map shouldBe properties.headers
                json.getString(RabbitProperty.CORRELATION_ID) shouldBe null
            }
        }
    }
})

class `json to properties` : BehaviorSpec({
    Given("A JsonObject of properties") {
        val json = JsonObject().apply {
            put(RabbitProperty.CONTENT_TYPE, "Ct")
            put(RabbitProperty.CONTENT_ENCODING, "ENC")
            put(RabbitProperty.DELIVERY_MODE, 1)
            put(RabbitProperty.MESSAGE_ID, "id")
            put(RabbitProperty.TIMESTAMP, Instant.now())
            put(RabbitProperty.HEADERS, mapOf("key" to "value", "int" to 1))
        }
        When("The JSON is converted to properties") {
            val properties = RabbitProperty.fromJson(json)

            Then("The properties are set correctly") {
                properties.contentType shouldBe json.getString(RabbitProperty.CONTENT_TYPE)
                properties.contentEncoding shouldBe json.getString(RabbitProperty.CONTENT_ENCODING)
                properties.deliveryMode shouldBe json.getInteger(RabbitProperty.DELIVERY_MODE)
                properties.messageId shouldBe json.getString(RabbitProperty.MESSAGE_ID)
                properties.timestamp shouldBe Date.from(json.getInstant(RabbitProperty.TIMESTAMP))
                properties.headers shouldBe json.getJsonObject(RabbitProperty.HEADERS).map
                properties.correlationId shouldBe null
            }
        }
    }
})