package jamiecraane.dev.plugins

import io.ktor.network.sockets.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.collections.LinkedHashSet
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
    }

    routing {
        webSocket("/myws/echo") {
            send(Frame.Text("Hello from the server"))

            val random = Random(System.currentTimeMillis())
            launch {
                while (true) {
                    outgoing.send(Frame.Text("This is a random message: " + random.nextInt()))
                    delay(1.seconds)
                }
            }

            while (true) {
                incoming.receiveCatching().getOrNull().let { frame ->
                    when (frame) {
                        is Frame.Text -> {
                            send(Frame.Text("Server received: " + frame.readText()))
                        }

                        is Frame.Binary -> {
                            send(Frame.Binary(true, frame.buffer))
                        }

                        else -> {
                            send(Frame.Close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "Unexpected frame")))
                            close(CloseReason(CloseReason.Codes.PROTOCOL_ERROR, "Unexpected frame"))
                        }
                    }
                }
            }
        }
    }
}

class Connection(val session: DefaultWebSocketSession) {
    companion object {
        val lastId = AtomicInteger(0)
    }
    val name = "user${lastId.getAndIncrement()}"
}
