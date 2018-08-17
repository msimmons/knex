package net.contrapt.vertek.example.repository

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.Jdbi
import java.io.Closeable

class Repo(val jdbi: Jdbi) {

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

    fun <R> inTransaction(rollbackOnly: Boolean = false, block: () -> R) : R {
        val txn = txns.get()
        return when (txn) {
            null -> beginTxn(rollbackOnly).use { t ->
                val result = block()
                endTxn(t)
                result
            }
            else -> block()
        }
    }

    fun <R> withHandle(block: Handle.() -> R) : R {
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
            val txn = txns.get()
            when (txn) {
                null -> {}
                else -> {
                    println("Closing the txn ${handle}")
                    txns.remove()
                    txn.handle.close()
                }
            }
        }
    }
}