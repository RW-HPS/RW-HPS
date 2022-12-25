/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.net

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.io.InterruptedIOException

/**
 * Try multiple times to avoid timeouts
 * @property executionCount Int
 * @property retryInterval Long
 * @author RW-HPS/Dr
 */
class MyOkHttpRetryInterceptor internal constructor(builder: Builder) : Interceptor {
    //最大重试次数
    var executionCount : Int

    /**
     * retry间隔时间
     */
    val retryInterval : Long

    init {
        executionCount = builder.executionCount
        retryInterval = builder.retryInterval
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request: Request = chain.request()
        var response: Response? = doRequest(chain, request)
        var retryNum = 0
        while ((response == null || !response.isSuccessful) && retryNum <= executionCount) {
            val nextInterval = retryInterval
            try {
                Thread.sleep(nextInterval)
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                throw InterruptedIOException()
            }
            retryNum++
            // retry the request
            response = doRequest(chain, request)
        }

        if (response == null) {
            throw IOException("OK HTTP ERROR")
        }
        return response
    }

    private fun doRequest(chain: Interceptor.Chain, request: Request): Response? {
        var response: Response? = null
        try {
            response = chain.proceed(request)
        } catch (_: Exception) {
        }
        return response
    }

    class Builder {
        var executionCount = 3
            private set
        var retryInterval: Long = 1000
            private set

        fun executionCount(executionCount: Int): Builder {
            this.executionCount = executionCount
            return this
        }

        fun retryInterval(retryInterval: Long): Builder {
            this.retryInterval = retryInterval
            return this
        }

        fun build(): MyOkHttpRetryInterceptor {
            return MyOkHttpRetryInterceptor(this)
        }
    }
}