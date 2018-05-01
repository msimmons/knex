package net.contrapt.vertek.example.service

import com.mongodb.client.MongoDatabase
import com.mongodb.client.model.Filters.eq
import net.contrapt.vertek.example.model.User

class VerifyService(val db: MongoDatabase) {

    operator fun invoke(request: Request) : Response {
        val users = db.getCollection("users", User::class.java)
        val user = users.find(eq("email", request.email)).first()
        // Test the verify code
        return Response("atoken")
    }

    data class Request(
            var email: String = "",
            var verifyCode: String = ""
    )

    data class Response(
            var token: String?,
            var error: String? = null
    )
}