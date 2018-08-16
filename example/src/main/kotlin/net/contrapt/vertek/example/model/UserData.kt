package net.contrapt.vertek.example.model

data class UserData(
    var name: String = ""
) {

    companion object {
        val argFactory = object : JSONBArgument<UserData>() {}
        val colMapper = object : JSONBMapper<UserData>(UserData::class.java) {}
    }
}
