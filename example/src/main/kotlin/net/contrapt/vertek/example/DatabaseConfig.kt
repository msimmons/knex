package net.contrapt.vertek.example

import com.mongodb.MongoClient
import com.mongodb.MongoClientOptions
import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import org.bson.codecs.configuration.CodecRegistries.fromProviders
import org.bson.codecs.configuration.CodecRegistries.fromRegistries
import org.bson.codecs.pojo.PojoCodecProvider
import org.springframework.context.ApplicationContext
import org.springframework.context.support.beans

object DatabaseConfig {

    fun startup(context: ApplicationContext) {
        // Here we could do data migrations, ensure indices stuff like that
        val mongoDb = context.getBean(MongoDatabase::class.java)
        mongoDb.getCollection("users").apply {
            createIndex(Indexes.ascending("email"), IndexOptions().unique(true))
        }
    }

    fun context() = beans {
        bean("pojoRegistry") {
            fromRegistries(
                    MongoClient.getDefaultCodecRegistry(),
                    fromProviders(PojoCodecProvider.builder().automatic(true).build())
            )
        }
        bean("mongoClient") {
            val mongoHost = env.getProperty("mongo.host", "localhost")
            val options = MongoClientOptions.builder()
                    .codecRegistry(ref("pojoRegistry"))
                    .build()
            MongoClient(mongoHost, options)
        }
        bean("mongoDb") {
            val mongoDb = env.getProperty("mongo.db", "auth")
            val mongoClient : MongoClient = ref()
            mongoClient.getDatabase(mongoDb)
        }
    }
}