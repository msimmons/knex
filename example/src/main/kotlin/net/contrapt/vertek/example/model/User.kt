package net.contrapt.vertek.example.model

import org.bson.codecs.pojo.annotations.BsonCreator
import org.bson.codecs.pojo.annotations.BsonProperty
import org.bson.types.ObjectId
import java.util.*

class User (
        val email: String,
        var password: String,
        var firstName: String,
        val profileId: String,
        val id: ObjectId = ObjectId()
) {

    var createdAt = Date()
    var lastName: String? = null
    var loginAttempts: Int = 0
    var lastLoginAt: Date? = null

    companion object {

        @JvmStatic
        @BsonCreator
        fun fromBson(
                @BsonProperty("email") email: String,
                @BsonProperty("password") password: String,
                @BsonProperty("firstName") firstName: String,
                @BsonProperty("profileId") profileId: String,
                @BsonProperty("id") id: ObjectId
        ) : User {
            return User(email, password, firstName, profileId, id)
        }
    }
}
