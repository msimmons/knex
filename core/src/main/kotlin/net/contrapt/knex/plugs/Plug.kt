package net.contrapt.knex.plugs

interface Plug<T> {

    fun process(message: T)
}