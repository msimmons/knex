package net.contrapt.vertek.example.repository

import net.contrapt.vertek.example.model.User
import org.jdbi.v3.core.Jdbi
import java.util.*

class UserRepository(jdbi: Jdbi) : Repo(jdbi) {

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