package net.contrapt.vertek.rabbitmq

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.LongString
import io.vertx.core.json.JsonObject
import java.lang.reflect.Method
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.jvm.javaMethod

private typealias Properties = AMQP.BasicProperties
private typealias Builder = AMQP.BasicProperties.Builder

/**
 * Helps to manage transfer of rabbit message properties to and from message representation
 */
enum class RabbitProperty(private val getter: Method, private val setter: Method, val klass: KClass<*>) {

    contentType(Properties::getContentType.javaMethod!!, Builder::contentType.javaMethod!!, String::class),
    contentEncoding(Properties::getContentEncoding.javaMethod!!, Builder::contentEncoding.javaMethod!!, String::class),
    deliveryMode(Properties::getDeliveryMode.javaMethod!!, Builder::deliveryMode.javaMethod!!, Int::class),
    priority(Properties::getPriority.javaMethod!!, Builder::priority.javaMethod!!, Int::class),
    correlationId(Properties::getCorrelationId.javaMethod!!, Builder::correlationId.javaMethod!!, String::class),
    replyTo(Properties::getReplyTo.javaMethod!!, Builder::replyTo.javaMethod!!, String::class),
    expiration(Properties::getExpiration.javaMethod!!, Builder::expiration.javaMethod!!, String::class),
    messageId(Properties::getMessageId.javaMethod!!, Builder::messageId.javaMethod!!, String::class),
    timestamp(Properties::getTimestamp.javaMethod!!, Builder::timestamp.javaMethod!!, Date::class),
    type(Properties::getType.javaMethod!!, Builder::type.javaMethod!!, String::class),
    userId(Properties::getUserId.javaMethod!!, Builder::userId.javaMethod!!, String::class),
    appId(Properties::getAppId.javaMethod!!, Builder::appId.javaMethod!!, String::class),
    clusterId(Properties::getClusterId.javaMethod!!, Builder::clusterId.javaMethod!!, String::class),
    headers(Properties::getHeaders.javaMethod!!, Builder::headers.javaMethod!!, Map::class);

    companion object {

        val CONTENT_TYPE = contentType.name
        val CONTENT_ENCODING = contentEncoding.name
        val DELIVERY_MODE = deliveryMode.name
        val PRIORITY = priority.name
        val CORRELATION_ID = correlationId.name
        val REPLY_TO = replyTo.name
        val EXPIRATION = expiration.name
        val MESSAGE_ID = messageId.name
        val TIMESTAMP = timestamp.name
        val TYPE = type.name
        val USER_ID = userId.name
        val CLUSTER_ID = clusterId.name
        val HEADERS = headers.name

        fun toJson(properties: AMQP.BasicProperties) : JsonObject {

            return JsonObject().apply {
                values().forEach {
                    safePut(this, it.name, it.getter.invoke(properties))
                }
            }
        }

        fun fromJson(json: JsonObject) : AMQP.BasicProperties {
            return AMQP.BasicProperties().builder().apply {
                values().forEach {
                    val jsonValue = safeGet<Any?>(json, it.name)
                    val value = when (jsonValue) {
                        null -> null
                        is String -> when (it.klass) {
                            Date::class -> Date.from(Instant.parse(jsonValue))
                            else -> jsonValue
                        }
                        is JsonObject -> jsonValue.map
                        else -> jsonValue
                    }
                    it.setter.invoke(this, value)
                }
            }.build()
        }

        private fun safePut(json: JsonObject, key: String, value: Any?) {
            when ( value ) {
                null -> {}
                is LongString -> json.put(key, String(value.bytes))
                is Date -> json.put(key, value.toInstant())
                is Map<*,*> -> json.put(key, mapToJson(value as Map<String, Any?>))
                else -> json.put(key, value)
            }
        }

        private inline fun <reified T> safeGet(json: JsonObject, key: String) : T? {
            val value = json.getValue(key)
            return when ( value ) {
                null -> null
                is Instant -> Date.from(value) as T
                is T -> value
                else -> null
            }
        }

        /**
         * Convert a [Map] (headers) to a [JsonObject]
         */
        private fun mapToJson(map: Map<String, Any?>?) : JsonObject {
            val json = JsonObject()
            map?.entries?.forEach {
                safePut(json, it.key, it.value)
            }
            return json
        }


    }

}