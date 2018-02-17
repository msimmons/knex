package net.contrapt.vertek.plugs

interface Plug<T> {

    fun process(message: T)
}