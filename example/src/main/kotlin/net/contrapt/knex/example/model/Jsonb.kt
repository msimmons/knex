package net.contrapt.knex.example.model

import com.fasterxml.jackson.databind.ObjectMapper
import org.jdbi.v3.core.argument.AbstractArgumentFactory
import org.jdbi.v3.core.argument.Argument
import org.jdbi.v3.core.config.ConfigRegistry
import org.jdbi.v3.core.mapper.ColumnMapper
import org.jdbi.v3.core.statement.StatementContext
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

abstract class JSONBArgument<T> : AbstractArgumentFactory<T>(Types.VARCHAR) {
    private val mapper = ObjectMapper()
    override fun build(value: T, config: ConfigRegistry): Argument? {
        return object : Argument {
            override fun apply(position: Int, statement: PreparedStatement, ctx: StatementContext) {
                statement.setObject(position, mapper.writeValueAsString(value))
            }
        }
    }
}

abstract class JSONBMapper<T>(val klass: Class<T>) : ColumnMapper<T> {
    private val mapper = ObjectMapper()
    override fun map(r: ResultSet, columnNumber: Int, ctx: StatementContext): T? {
        val value = r.getString(columnNumber)
        return when (value) {
            null -> null
            else -> mapper.readValue(value, klass)
        }
    }
}