package net.contrapt.knex.example.repository

import net.contrapt.knex.example.model.User
import java.util.*

class UserRepository(repo: Repo) : AbstractRepository(repo) {

    fun insert(user: User) {
        withHandle {
            createUpdate("insert into example.users (id, created_at, data) values (:id, :createdAt, :data::jsonb)")
                .bindBean(user)
                .execute()
        }
    }

    fun findOne(id: UUID) : User? {
        return withHandle {
            createQuery("select * from example.users where id = :id")
                .bind("id", id)
                .mapTo(User::class.java)
                .findOnly()
        }
    }
}