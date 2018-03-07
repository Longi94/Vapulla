package `in`.dragonbra.vapulla.retrofit.response

data class AppDetailsResponse(val success: Boolean, val data: AppData)

data class AppData(val name: String)