package net.contrapt.vertek.example.service

import com.mongodb.MongoWriteException
import com.mongodb.client.MongoDatabase
import net.contrapt.vertek.example.model.User
import java.util.*

class SignupService(val db: MongoDatabase) {

    operator fun invoke(request: Request) : Response {
        val users = db.getCollection("users", User::class.java)
        // Hash the password
        // Encrypt the firstname
        // Create and encrypt the profileid
        // Create profileid hash for lookup
        val user = User(request.email, request.password, request.firstName, UUID.randomUUID().toString())
        try {
            users.insertOne(user)
        } catch (e: MongoWriteException) {
            when (e.error.code) {
                11000 -> return Response(null, "User ${request.email} exists already")
                else -> throw e
            }
        }
        return Response(user)
    }

    data class Request(
            var email: String = "",
            var password: String = "",
            var firstName: String = ""
    )

    data class Response(
            var user: User?,
            var error: String? = null
    )
}