package net.contrapt.knex.example.service

import net.contrapt.knex.example.model.User
import net.contrapt.knex.example.repository.UserRepository
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