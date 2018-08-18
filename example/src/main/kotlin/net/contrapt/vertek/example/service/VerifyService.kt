package net.contrapt.vertek.example.service

import net.contrapt.vertek.example.model.User
import net.contrapt.vertek.example.repository.UserRepository

class VerifyService(val repository: UserRepository) {

    operator fun invoke(request: Request) : Response {
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