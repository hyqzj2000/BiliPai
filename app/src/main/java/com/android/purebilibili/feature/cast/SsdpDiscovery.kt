package com.android.purebilibili.feature.cast

import com.android.purebilibili.core.util.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface

/**
 * 手动 SSDP 发现实现
 * 用于在 Cling 库不工作时作为备用方案
 */
object SsdpDiscovery {
    private const val TAG = "SsdpDiscovery"
    
    // SSDP 多播地址和端口
    private const val SSDP_ADDRESS = "239.255.255.250"
    private const val SSDP_PORT = 1900
    
    // M-SEARCH 请求 - 搜索所有设备
    private val M_SEARCH_ALL = """
        M-SEARCH * HTTP/1.1
        HOST: 239.255.255.250:1900
        MAN: "ssdp:discover"
        MX: 3
        ST: ssdp:all
        
    """.trimIndent().replace("\n", "\r\n")
    
    // M-SEARCH 请求 - 仅搜索 MediaRenderer
    private val M_SEARCH_RENDERER = """
        M-SEARCH * HTTP/1.1
        HOST: 239.255.255.250:1900
        MAN: "ssdp:discover"
        MX: 3
        ST: urn:schemas-upnp-org:device:MediaRenderer:1
        
    """.trimIndent().replace("\n", "\r\n")
    
    data class SsdpDevice(
        val location: String,
        val server: String,
        val usn: String,
        val st: String
    )
    
    /**
     * 执行 SSDP 发现
     * @param timeoutMs 超时时间（毫秒）
     * @return 发现的设备列表
     */
    suspend fun discover(timeoutMs: Int = 5000): List<SsdpDevice> = withContext(Dispatchers.IO) {
        val devices = mutableListOf<SsdpDevice>()
        var socket: DatagramSocket? = null
        
        try {
            Logger.i(TAG, "📺 [DLNA] Starting SSDP discovery (timeout: ${timeoutMs}ms)")
            
            // 创建 UDP socket
            socket = DatagramSocket(null)
            socket.reuseAddress = true
            socket.broadcast = true
            socket.soTimeout = timeoutMs
            socket.bind(InetSocketAddress(0))
            
            Logger.d(TAG, "📺 [DLNA] Socket bound to local port ${socket.localPort}")
            
            // 发送 M-SEARCH 请求
            val searchData = M_SEARCH_ALL.toByteArray()
            val multicastAddress = InetAddress.getByName(SSDP_ADDRESS)
            val searchPacket = DatagramPacket(searchData, searchData.size, multicastAddress, SSDP_PORT)
            
            socket.send(searchPacket)
            Logger.i(TAG, "📺 [DLNA] M-SEARCH (ssdp:all) sent to multicast address")
            
            // 也发送一个针对 MediaRenderer 的搜索
            val rendererData = M_SEARCH_RENDERER.toByteArray()
            val rendererPacket = DatagramPacket(rendererData, rendererData.size, multicastAddress, SSDP_PORT)
            socket.send(rendererPacket)
            Logger.i(TAG, "📺 [DLNA] M-SEARCH (MediaRenderer) sent")
            
            // 接收响应
            val buffer = ByteArray(2048)
            val startTime = System.currentTimeMillis()
            val seenUsns = mutableSetOf<String>()
            var responseCount = 0
            
            while (System.currentTimeMillis() - startTime < timeoutMs) {
                try {
                    val responsePacket = DatagramPacket(buffer, buffer.size)
                    socket.receive(responsePacket)
                    responseCount++
                    
                    val response = String(responsePacket.data, 0, responsePacket.length)
                    
                    // 解析响应
                    val device = parseResponse(response)
                    if (device != null && device.usn !in seenUsns) {
                        seenUsns.add(device.usn)
                        devices.add(device)
                        // 隐私安全日志：只显示设备类型和服务器信息，不显示完整 URL 和 IP
                        Logger.i(TAG, "📺 [DLNA] Found device: server=${device.server.take(50)}, type=${device.st.substringAfterLast(":")}")
                    }
                } catch (e: java.net.SocketTimeoutException) {
                    // 超时，结束接收
                    break
                }
            }
            
            val elapsed = System.currentTimeMillis() - startTime
            Logger.i(TAG, "📺 [DLNA] Discovery completed in ${elapsed}ms: received $responseCount responses, found ${devices.size} unique devices")
            
        } catch (e: Exception) {
            Logger.e(TAG, "📺 [DLNA] Discovery error: ${e.javaClass.simpleName} - ${e.message}")
        } finally {
            socket?.close()
        }
        
        devices
    }
    
    private fun parseResponse(response: String): SsdpDevice? {
        val lines = response.split("\r\n", "\n")
        var location = ""
        var server = ""
        var usn = ""
        var st = ""
        
        for (line in lines) {
            when {
                line.startsWith("LOCATION:", ignoreCase = true) -> {
                    location = line.substringAfter(":").trim()
                }
                line.startsWith("SERVER:", ignoreCase = true) -> {
                    server = line.substringAfter(":").trim()
                }
                line.startsWith("USN:", ignoreCase = true) -> {
                    usn = line.substringAfter(":").trim()
                }
                line.startsWith("ST:", ignoreCase = true) -> {
                    st = line.substringAfter(":").trim()
                }
            }
        }
        
        return if (location.isNotEmpty() && usn.isNotEmpty()) {
            SsdpDevice(location, server, usn, st)
        } else {
            null
        }
    }
}
