package net.contrapt.vertek.endpoints

import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject

interface ExceptionHandler {

    /**
     * Handle an exception in message processing returning whether or not to consider the exception as handled.  If
     * the exception is handled, message flow continues as successful, otherwise as failure
     *
     * @param message The message being handled
     * @param exception The exception thrown
     * @return Whether the exception was handled
     */
    fun handle(message: Message<JsonObject>, exception: Throwable) : Boolean
}