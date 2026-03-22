package com.trail2.data.remote

import com.trail2.BuildConfig
import com.trail2.auth.TokenManager
import com.trail2.data.remote.dto.NotificationResponseDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NotificationWebSocketClient @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val tokenManager: TokenManager,
    private val json: Json
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _events = MutableSharedFlow<NotificationResponseDto>(extraBufferCapacity = 16)
    val events: SharedFlow<NotificationResponseDto> = _events.asSharedFlow()

    private var socket: WebSocket? = null
    private var shouldReconnect = false
    private var reconnectAttempt = 0

    fun start() {
        shouldReconnect = true
        reconnectAttempt = 0
        connect()
    }

    fun stop() {
        shouldReconnect = false
        reconnectAttempt = 0
        socket?.close(1000, null)
        socket = null
    }

    private fun reconnectDelay(): Long {
        // Exponential backoff: 5s, 10s, 20s, 40s, max 60s
        val delay = (5_000L * (1 shl reconnectAttempt.coerceAtMost(3)))
        reconnectAttempt++
        return delay.coerceAtMost(60_000L)
    }

    private fun connect() {
        val wsUrl = BuildConfig.BASE_URL
            .replace("https://", "wss://")
            .replace("http://", "ws://")
            .let { url ->
                val idx = url.indexOf("/api/v1/")
                if (idx != -1) url.substring(0, idx) else url.trimEnd('/')
            }
            .plus("/ws/notifications")

        val request = Request.Builder().url(wsUrl).build()
        socket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(ws: WebSocket, response: Response) {
                reconnectAttempt = 0
                val token = tokenManager.getAccessToken() ?: return
                ws.send("""{"token":"$token"}""")
            }

            override fun onMessage(ws: WebSocket, text: String) {
                try {
                    val dto = json.decodeFromString<NotificationResponseDto>(text)
                    _events.tryEmit(dto)
                } catch (_: Exception) {}
            }

            override fun onClosing(ws: WebSocket, code: Int, reason: String) {
                ws.close(code, reason)
            }

            override fun onClosed(ws: WebSocket, code: Int, reason: String) {
                if (shouldReconnect && code != 4001) {
                    scope.launch {
                        delay(reconnectDelay())
                        if (shouldReconnect) connect()
                    }
                }
            }

            override fun onFailure(ws: WebSocket, t: Throwable, response: Response?) {
                if (shouldReconnect) {
                    scope.launch {
                        delay(reconnectDelay())
                        if (shouldReconnect) connect()
                    }
                }
            }
        })
    }
}
