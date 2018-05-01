package net.contapt.vertek.example.service

import com.mongodb.client.MongoDatabase
import net.contrapt.vertek.example.DatabaseConfig
import net.contrapt.vertek.example.ServiceConfig
import org.junit.Before
import org.springframework.context.support.GenericApplicationContext
import org.springframework.core.io.support.ResourcePropertySource

abstract class AbstractServiceTest {

    val context: GenericApplicationContext
    init {
        context = GenericApplicationContext().apply {
            environment.propertySources.addLast(ResourcePropertySource("classpath:application.test.properties"))
            DatabaseConfig.context().initialize(this)
            ServiceConfig.context().initialize(this)
            refresh()
        }
    }

    inline fun <reified T> autowire() : T = context.getBean(T::class.java)

    @Before
    fun before() {
        val mongoDb = autowire<MongoDatabase>()
        mongoDb.drop()
        DatabaseConfig.startup(context)
    }

}