package net.contrapt.knex.example.repository

import org.jdbi.v3.core.Handle

abstract class AbstractRepository(private val sessionManager: SessionManager) {

    fun <R> transaction(rollbackOnly: Boolean = false, block: () -> R) : R {
        return sessionManager.inTransaction(rollbackOnly, block)
    }

    fun <R> withHandle(block: Handle.() -> R) : R {
        return sessionManager.withHandle(block)
    }

}