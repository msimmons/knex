package net.contrapt.vertek.example.service

import net.contrapt.vertek.example.model.User
import net.contrapt.vertek.example.repository.UserRepository
import java.util.*

class LoginService(val repository: UserRepository) {

    operator fun invoke(data: Request) : Response {
        return Response("", "")
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