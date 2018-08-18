package net.contrapt.knex.example.repository

import org.jdbi.v3.core.Handle

abstract class AbstractRepository(private val repo: Repo) {

    fun <R> transaction(rollbackOnly: Boolean = false, block: () -> R) : R {
        return repo.inTransaction(rollbackOnly, block)
    }

    fun <R> withHandle(block: Handle.() -> R) : R {
        return repo.withHandle(block)
    }

}