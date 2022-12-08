package jamiecraane.dev

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.onFailure
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.*
import okhttp3.*
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

fun main() = runBlocking<Unit> {
    val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    val okHttpWebSocketFlow = OkHttpWebSocketFlow(client, "ws://localhost:8080/myws/echo")
    launch {
        okHttpWebSocketFlow
            .wsFlow(this)
            .collect {
                println("Received in 1 $it")
            }
    }

    launch {
        while (true) {
            okHttpWebSocketFlow.send("This is ME")
            delay(2.seconds)
        }
    }

}

class WebSocketFailureException(override val message: String) : RuntimeException(message)

/**
 * Represents a generic web socket flow.
 */
class OkHttpWebSocketFlow(
    private val client: OkHttpClient,
    private val wsUrl: String,
) {
    private var webSocket: WebSocket? = null

    fun wsFlow(scope: CoroutineScope) =
        callbackFlow {
            val request = Request.Builder()
                .url(wsUrl)
                .build()

            webSocket = client.newWebSocket(request, object : WebSocketListener() {
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    cancel(CancellationException("Failure", WebSocketFailureException(t.message ?: "WebSocket failure")))
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    trySendBlocking(text).onFailure {
                        println("Fail to deliver message")
                    }
                }
            })

            awaitClose {
                webSocket?.close(1000, null)
            }
        }
            .retryWhen { cause, attempt ->
                val shouldRetry = cause.cause is WebSocketFailureException && attempt < 5
                if (shouldRetry) {
                    delay(5.seconds)
                    true
                } else {
                    false
                }
            }
            .stateIn(scope = scope, started = SharingStarted.WhileSubscribed(), initialValue = "")

    /**
     * Sends data over the web socket connection.
     *
     * @param text The text to sent.
     */
    fun send(text: String) {
        webSocket?.send(text)
    }
}
