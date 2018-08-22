package net.contrapt.knex.example.service

import net.contrapt.knex.example.model.User
import net.contrapt.knex.example.repository.UserRepository

class LoginService(val repository: UserRepository) {

    operator fun invoke(data: Request) : Response {
        val user = User()
        val u = repository.transaction {
            repository.insert(user)
            repository.findOne(user.id)
        }
        return Response(u?.id.toString(), u?.id.toString())
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