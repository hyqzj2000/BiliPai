package com.android.purebilibili.feature.cast

import fi.iki.elonen.NanoHTTPD
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.InputStream
import android.util.Log

/**
 * 运行在手机上的轻量级 HTTP 代理服务器。
 * 作用：拦截 DLNA 设备的播放请求，转发给 Bilibili 服务器并修改请求头，从而绕过防盗链 (403 Forbidden)。
 *
 * 原理：
 * 1. 电视/DLNA 设备请求: http://<手机IP>:<端口>/proxy?url=<编码后的B站视频URL>
 * 2. 代理服务器解析 `url` 参数。
 * 3. 代理服务器伪装成合法客户端（添加 User-Agent, Referer）向 B站请求数据。
 * 4. 代理服务器将 B 站返回的数据流（InputStream）直接流式传输给电视。
 */
class LocalProxyServer(port: Int = 8901) : NanoHTTPD(port) {

    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true) // 虽然电视可能只发起 HTTP 请求，但我们需要从 B 站获取 HTTPS 数据
        .build()

    override fun serve(session: IHTTPSession): Response {
        val uri = session.uri
        // 仅处理 /proxy 路径的请求
        if (uri != "/proxy") {
            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not Found")
        }

        val params = session.parms
        val targetUrl = params["url"]
        
        // 基础校验：必须包含目标 URL
        if (targetUrl.isNullOrEmpty()) {
             return newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Missing 'url' parameter")
        }
        
        Log.d("LocalProxyServer", "正在代理请求: $targetUrl")

        try {
            // 构建发往 Bilibili 的请求
            // 关键点：设置 Referer 和 User-Agent 以绕过 B 站的防盗链检查
            val referer = params["referer"] ?: "https://www.bilibili.com"
            val userAgent = params["ua"] ?: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"

            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", userAgent)
                .header("Referer", referer)
                .build()

            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Upstream Error: ${response.code}")
            }

            // 获取 B 站返回的视频流和元数据
            val inputStream = response.body?.byteStream() ?: return newFixedLengthResponse(Response.Status.NO_CONTENT, MIME_PLAINTEXT, "")
            val contentType = response.header("Content-Type") ?: "video/mp4"
            val contentLength = response.body?.contentLength() ?: -1L

            // 构造返回给电视的响应
            // 使用 ChunkedResponse 以支持流式传输，避免将整个视频加载到内存中
            val nanoResponse = newChunkedResponse(Response.Status.OK, contentType, inputStream)
            
            // 转发关键响应头 (如 Content-Length)，这对播放器的进度条显示和拖动至关重要
            if (contentLength != -1L) {
                 nanoResponse.addHeader("Content-Length", contentLength.toString())
            }
            
            return nanoResponse

        } catch (e: Exception) {
            Log.e("LocalProxyServer", "代理请求处理失败", e)
            return newFixedLengthResponse(Response.Status.INTERNAL_ERROR, MIME_PLAINTEXT, "Error: ${e.message}")
        }
    }

    companion object {
        const val PORT = 8901
        
        /**
         * 生成代理 URL供 DLNA 设备使用
         * @param context 用于获取 Wi-Fi IP 地址
         * @param targetUrl 实际的 B 站视频 URL
         * @return 代理服务器的完整 URL
         */
        fun getProxyUrl(context: android.content.Context, targetUrl: String): String {
            // 获取本机 IP 地址
            val wifiManager = context.applicationContext.getSystemService(android.content.Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            // 注意：这种获取 IP 的方式在 IPv6 或复杂网络下可能不准确，但通常适用于家庭 Wi-Fi 环境
            val ipAddress = android.text.format.Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
            
            // 对目标 URL 进行编码，作为参数传递
            val encodedUrl = java.net.URLEncoder.encode(targetUrl, "UTF-8")
            
            return "http://$ipAddress:$PORT/proxy?url=$encodedUrl"
        }
    }
}
