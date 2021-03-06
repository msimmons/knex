package net.contapt.knex.example.service

import net.contrapt.knex.example.DatabaseConfig
import net.contrapt.knex.example.ServiceConfig
import net.contrapt.knex.example.repository.SessionManager
import org.junit.After
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
        DatabaseConfig.startup(context)
    }

    inline fun <reified T> autowire() : T = context.getBean(T::class.java)

    @Before
    fun before() {
        val session = autowire<SessionManager>()
        session.beginTransaction(true)
    }

    @After
    fun after() {
        val session = autowire<SessionManager>()
        session.endTransaction()
    }

}