package net.contrapt.vertek.example

import io.vertx.core.Vertx
import org.apache.tomcat.jdbc.pool.DataSource
import org.flywaydb.core.Flyway
import org.springframework.context.ApplicationContext
import org.springframework.context.support.beans

object DatabaseConfig {

    fun startup(vertx: Vertx, context: ApplicationContext) {
        val flyway = context.getBean("flyway", Flyway::class.java)
        flyway.migrate()
    }

    fun context() = beans {

        bean("dataSource") {
            val host = env.getProperty("example.db.host", "localhost")
            val port = env.getProperty("example.db.port", "5432")
            val database = env.getProperty("example.db.database", "sandbox")
            DataSource().apply {
                driverClassName = "org.postgresql.Driver"
                url = "jdbc:postgresql://$host:$port/$database"
                username = env.getProperty("example.db.user", "postgres")
                password = env.getProperty("example.db.password", "password")
                isTestOnBorrow = true
                validationQuery = "select 1"
            }
        }

        bean("flyway") {
            val schema = env.getProperty("example.db.schema", "example")
            Flyway().apply {
                dataSource = ref()
                setSchemas(schema)
                isOutOfOrder = true
                isCleanOnValidationError = true
            }
        }
    }
}