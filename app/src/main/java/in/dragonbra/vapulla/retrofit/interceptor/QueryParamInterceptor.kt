package `in`.dragonbra.vapulla.retrofit.interceptor

import okhttp3.Interceptor
import okhttp3.Response

class QueryParamInterceptor(val name: String, val value: String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url().newBuilder().addQueryParameter(name, value).build()
        return chain.proceed(request.newBuilder().url(url).build())
    }
}