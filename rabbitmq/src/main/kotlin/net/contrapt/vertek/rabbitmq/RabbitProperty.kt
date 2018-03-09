package net.contrapt.vertek.rabbitmq

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.LongString
import io.vertx.core.json.JsonObject
import java.lang.reflect.Method
import java.time.Instant
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.jvm.javaMethod

sealed class RabbitProperty<T>(val key: String, private val getter: Method?) {

    val foo = AMQP.BasicProperties.Builder::contentEncoding.javaMethod
    fun get(properties: AMQP.BasicProperties) : T = getter?.invoke(properties) as T

    object ContentType : RabbitProperty<String>("contentType", AMQP.BasicProperties::getContentType.javaMethod)
    object ContentEncoding : RabbitProperty<String>("contentEncoding", AMQP.BasicProperties::getContentEncoding.javaMethod)
    object DeliveryMode : RabbitProperty<Int>("deliveryMode", AMQP.BasicProperties::getDeliveryMode.javaMethod)
    object Priority : RabbitProperty<Int>("priority", AMQP.BasicProperties::getPriority.javaMethod)
    object CorrelationId : RabbitProperty<String>("correlationId", AMQP.BasicProperties::getCorrelationId.javaMethod)
    object ReplyTo : RabbitProperty<String>("replyTo", AMQP.BasicProperties::getReplyTo.javaMethod)
    object Expiration : RabbitProperty<String>("expiration", AMQP.BasicProperties::getExpiration.javaMethod)
    object MessageId : RabbitProperty<String>("messageId", AMQP.BasicProperties::getMessageId.javaMethod)
    object Timestamp : RabbitProperty<Date>("timestamp", AMQP.BasicProperties::getTimestamp.javaMethod)
    object Type : RabbitProperty<String>("type", AMQP.BasicProperties::getType.javaMethod)
    object UserId : RabbitProperty<String>("userId", AMQP.BasicProperties::getUserId.javaMethod)
    object AppId : RabbitProperty<String>("appId", AMQP.BasicProperties::getAppId.javaMethod)
    object ClusterId : RabbitProperty<String>("clusterId", AMQP.BasicProperties::getClusterId.javaMethod)
    object Headers : RabbitProperty<Map<String, Any?>>("headers", AMQP.BasicProperties::getHeaders.javaMethod)

}

private typealias Properties = AMQP.BasicProperties
private typealias Builder = AMQP.BasicProperties.Builder


enum class FooProperty(private val getter: Method, private val setter: Method, private val klass: KClass<*>) {

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
    }

}