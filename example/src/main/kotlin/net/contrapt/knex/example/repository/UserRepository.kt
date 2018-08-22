package net.contrapt.knex.example.repository

import net.contrapt.knex.example.model.User
import java.util.*

class UserRepository(sessionManager: SessionManager) : AbstractRepository(sessionManager) {

    fun insert(user: User) {
        withHandle {
            createUpdate("insert into users (id, created_at, data) values (:id, :createdAt, :data::jsonb)")
                .bindBean(user)
                .execute()
        }
    }

    fun findOne(id: UUID) : User? {
        return withHandle {
            createQuery("select * from users where id = :id")
                .bind("id", id)
                .mapTo(User::class.java)
                .findOnly()
        }
    }
}