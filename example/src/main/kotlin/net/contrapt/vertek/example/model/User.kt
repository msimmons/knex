package net.contrapt.vertek.example.model

import java.time.Instant
import java.util.*

class User(
    val id: UUID = UUID.randomUUID(),
    val createdAt: Instant = Instant.now()
) {
    var updatedAt: Instant? = null
    var data: UserData? = null
}

