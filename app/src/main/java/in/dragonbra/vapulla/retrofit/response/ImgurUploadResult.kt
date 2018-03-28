package `in`.dragonbra.vapulla.retrofit.response

data class ImgurUploadResult(val success: Boolean, val status: Int, val data: ImgurUploadData)

data class ImgurUploadData(val link: String)