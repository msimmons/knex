package net.contrapt.vertek.example.service

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import net.contrapt.vertek.example.model.User
import java.util.*

class LoginService(val db: MongoDatabase) {

    operator fun invoke(data: Request) : Response {
        val users = db.getCollection("users", User::class.java)
        val query = eq("email", data.email)
        val user = users.find(query).first()
        if (user?.password != data.password || user.loginAttempts > 3) {
            user.loginAttempts++
            users.replaceOne(query, user)
            return Response(null, null, "Invalid credentials")
        } else {
            user.loginAttempts = 0
            user.lastLoginAt = Date()
            users.replaceOne(query, user)
            // Create a JWT token
            return Response("atoken", user.profileId)
        }
    }

    data class Request(
            var email: String = "",
            var password: String = ""
    )

    data class Response(
            var token: String?,
            var profileId: String?,
            var error: String? = null
    )
}