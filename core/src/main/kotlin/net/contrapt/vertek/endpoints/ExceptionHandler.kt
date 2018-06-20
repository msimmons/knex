package net.contrapt.vertek.endpoints

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject

interface ExceptionHandler {

    /**
     * Handle an exception in message processing returning whether or not to consider the exception as handled.  If
     * the exception is handled, message flow continues as successful, otherwise subsequent handlers are called until
     * exception is handled or there are no more handlers in which case processing is considered _failed_
     *
     * @param message The message being handled
     * @param exception The exception thrown
     * @return Whether the exception was handled
     */
    fun handle(message: Message<JsonObject>, exception: Throwable) : Boolean
}