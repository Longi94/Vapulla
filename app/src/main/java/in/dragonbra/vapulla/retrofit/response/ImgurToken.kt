package `in`.dragonbra.vapulla.retrofit.response

data class ImgurToken(
    val access_token: String,
    val refresh_token: String,
    val expires_in: Long,
    val token_type: String,
    val account_username: String
)