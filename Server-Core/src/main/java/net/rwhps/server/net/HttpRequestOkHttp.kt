/*
 * Copyright 2020-2022 RW-HPS Team and contributors.
 *
 * 此源代码的使用受 GNU AFFERO GENERAL PUBLIC LICENSE version 3 许可证的约束, 可以在以下链接找到该许可证.
 * Use of this source code is governed by the GNU AGPLv3 license that can be found through the following link.
 *
 * https://github.com/RW-HPS/RW-HPS/blob/master/LICENSE
 */

package net.rwhps.server.net

import net.rwhps.server.util.log.Log
import net.rwhps.server.util.log.Log.error
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Request.Builder
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.*
import net.rwhps.server.util.log.Log.error

/**
 * HTTP
 * @author RW-HPS/Dr
 */
object HttpRequestOkHttp {
    private const val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/80.0.3987.132 Safari/537.36"
    private val CLIENT = OkHttpClient()

    /**
     * Send a GET request and get back
     * @param url HTTP URL
     * @return    Data
     */
    @JvmStatic
    fun doGet(url: String?): String {
        if (url.isNullOrBlank()) {
            error("[GET URL] NULL")
            return ""
        }

        val request: Request = Builder()
            .url(url)
            .addHeader("User-Agent", USER_AGENT)
            .build()
        try {
            CLIENT.newCall(request).execute().use { response -> return response.body?.string() ?:"" }
        } catch (e: Exception) {
            error(e)
        }
        return ""
    }

    /**
     * Send a POST request with Parameter and get back
     * @param url    HTTP URL
     * @param param  Parameter (A=B&C=D)
     * @return       Data
     */
    @JvmStatic
    fun doPost(url: String?, param: String): String {
        val formBody = FormBody.Builder()
        val paramArray = param.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (pam in paramArray) {
            val keyValue = pam.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            formBody.add(keyValue[0], keyValue[1])
        }
        return doPost(url, formBody)
    }

    /**
     * Send a POST request with Parameter and get back
     * @param url    HTTP URL
     * @param data    FormBody.Builder Parameter
     * @return        Data
     */
    @JvmStatic
    fun doPost(url: String?, data: FormBody.Builder): String {
        if (url.isNullOrBlank()) {
            error("[POST URL] NULL")
            return ""
        }

        val request: Request = Builder()
            .url(url)
            .addHeader("User-Agent", USER_AGENT)
            .post(data.build())
            .build()
        return getHttpResultString(request)
    }

    /**
     * Send POST request with JSON and return
     * @param url    HTTP URL
     * @param param  JSON
     * @return       Data
     */
    @JvmStatic
    fun doPostJson(url: String?, param: String?): String {
        if (url.isNullOrBlank() || param.isNullOrBlank()) {
            error("[POST Json URL] NULL")
            return ""
        }

        val body: RequestBody = param.toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
        val request: Request = Builder()
            .url(url)
            .addHeader("User-Agent", USER_AGENT)
            .post(body)
            .build()
        return getHttpResultString(request)
    }

    /**
     * Request and Return
     * @param request     Request
     * @return            Result
     */
    private fun getHttpResultString(request: Request): String {
        return try {
            getHttpResultString(request,false)
        } catch (e: Exception) {
            error("[HttpResult]",e)
            ""
        }
    }
    /**
     * Request and Return
     * @param request     Request
     * @param resultError Print Error
     * @return            Result
     */
    @Throws(IOException::class)
    private fun getHttpResultString(request: Request, resultError: Boolean): String {
        var result = ""
        try {
            CLIENT.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    error("Unexpected code", IOException())
                }
                result = response.body?.string() ?:""
                response.body?.close()
            }
        } catch (e: Exception) {
            if (resultError) {
                throw e
            }
        }
        return result
    }

    @JvmStatic
    fun doPostRw(url: String, param: String): String {
        val formBody = FormBody.Builder()
        val paramArray = param.split("&".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        for (pam in paramArray) {
            val keyValue = pam.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            formBody.add(keyValue[0], keyValue[1])
        }
        val request: Request = Builder()
            .url(url)
            .addHeader("User-Agent", "rw android 151 zh")
            .addHeader("Language", "zh")
            .addHeader("Connection", "close")
            .post(formBody.build())
            .build()
        try {
            return getHttpResultString(request, true)
        } catch (e: Exception) {
            error("[UpList Error] CF CDN Error? (Ignorable)")
        }
        return ""
    }

    @JvmStatic
    fun downUrl(url: String?, file: File?): Boolean {
        Log.clog(url!!)
        if (url.isBlank() || file == null) {
            error("[DownUrl URL] NULL")
            return false
        }

        var output: FileOutputStream? = null
        val request: Request = Builder()
            .url(url)
            .addHeader("User-Agent", USER_AGENT)
            .build()
        try {
            CLIENT.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw FileNotFoundException()
                }

                if (response.body == null) {
                    error("[Down File] Response.body NPE")
                    return false
                }
                output = FileOutputStream(file)
                output!!.write(response.body!!.bytes())
                output!!.flush()
                return true
            }
        } catch (e: Exception) {
            error(e)
        } finally {
            try {
                output?.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return false
    }
}
