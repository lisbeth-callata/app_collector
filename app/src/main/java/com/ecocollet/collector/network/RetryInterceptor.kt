package com.ecocollet.collector.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class RetryInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        var response: Response? = null
        var exception: IOException? = null

        for (attempt in 1..3) {
            try {
                response = chain.proceed(request)
                if (response.isSuccessful) {
                    return response
                }
            } catch (e: IOException) {
                exception = e
                if (attempt == 3) {
                    throw exception!!
                }
            }

            try {
                Thread.sleep((attempt * 1000).toLong())
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw IOException("Interrupted during retry", e)
            }
        }

        return response ?: throw exception ?: IOException("Unknown error")
    }
}