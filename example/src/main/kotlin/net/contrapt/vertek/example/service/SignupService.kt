package net.contrapt.vertek.example.service

import net.contrapt.vertek.example.model.User
import net.contrapt.vertek.example.repository.UserRepository
import java.util.*

class SignupService(val repository: UserRepository) {

    operator fun invoke(request: Request) : Response {
        // Hash the password
        // Encrypt the firstname
        // Create and encrypt the profileid
        // Create profileid hash for lookup
        return Response(User())
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