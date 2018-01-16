package net.contrapt.vertx.plugs

interface Plug<T> {

    fun process(message: T)
}