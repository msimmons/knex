package net.contrapt.vertek.example

import com.zaxxer.hikari.HikariDataSource
import io.vertx.core.Vertx
import net.contrapt.vertek.example.model.User
import net.contrapt.vertek.example.model.UserData
import net.contrapt.vertek.example.repository.UserRepository
import org.flywaydb.core.Flyway
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.springframework.context.ApplicationContext
import org.springframework.context.support.beans
import java.time.Instant
import java.util.*
import javax.sql.DataSource

object DatabaseConfig {

    fun startup(vertx: Vertx, context: ApplicationContext) {
        val flyway = context.getBean("flyway", Flyway::class.java)
        flyway.migrate()

        val repo = context.getBean(UserRepository::class.java)
        val user = User(UUID.randomUUID(), Instant.now())
        user.data = UserData("mark")
        repo.inTransaction { repo.insert(user) }
        repo.inTransaction {
            repo.insert(user)
            val u2 = repo.findOne(user.id)
            assert(u2?.data?.name == user.data?.name, {"not the same ${u2?.data}"})
        }
    }

    fun context() = beans {

        bean("dataSource") {
            val host = env.getProperty("example.db.host", "localhost")
            val port = env.getProperty("example.db.port", "5432")
            val database = env.getProperty("example.db.database", "sandbox")
            HikariDataSource().apply {
                //dataSourceClassName = "org.postgresql.ds.PGSimpleDataSource"
                driverClassName = "org.postgresql.Driver"
                jdbcUrl = "jdbc:postgresql://$host:$port/$database"
                username = env.getProperty("example.db.user", "postgres")
                password = env.getProperty("example.db.password", "password")
                poolName = "Example Pool"
                addDataSourceProperty("applicationName", "example")
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

        bean("jdbi") {
            val dataSource: DataSource = ref()
            Jdbi.create(dataSource).apply {
                installPlugin(KotlinPlugin())
                registerArgument(UserData.argFactory)
                registerColumnMapper(UserData.colMapper)
            }
        }

        bean() {
            UserRepository(ref())
        }
    }
}