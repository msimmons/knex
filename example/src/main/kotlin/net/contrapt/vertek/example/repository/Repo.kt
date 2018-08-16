package net.contrapt.vertek.example.repository

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import java.io.Closeable

abstract class Repo(val jdbi: Jdbi) {

    protected val txns = ThreadLocal<Transaction>()

    private fun beginTxn(rollbackOnly: Boolean = false) : Transaction {
        val handle = jdbi.open().apply { begin() }
        txns.set(Transaction(handle, rollbackOnly))
        return txns.get()
    }

    private fun endTxn(txn: Transaction) {
        when (txn.rollbackOnly) {
            true -> txn.handle.rollback()
            else -> txn.handle.commit()
        }
    }

    public fun <R> inTransaction(rollbackOnly: Boolean = false, block: Repo.() -> R) : R {
        val handle = txns.get()
        return when (handle) {
            null -> beginTxn(rollbackOnly).use { t ->
                val result = block()
                endTxn(t)
                result
            }
            else -> block()
        }
    }

    protected fun <R> withHandle(block: Handle.() -> R) : R {
        val txn = txns.get()
        return when (txn) {
            null -> beginTxn(false).use { t ->
                val result = block(t.handle)
                endTxn(t)
                return result
            }
            else -> block(txn.handle)
        }
    }

    inner class Transaction (
        val handle: Handle,
        val rollbackOnly: Boolean
    ) : Closeable {

        override fun close() {
            println("Closing the txn ${handle}")
            this@Repo.txns.remove()
        }
    }
}