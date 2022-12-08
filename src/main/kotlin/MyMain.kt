import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.runBlocking
import kotlin.random.Random
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

fun main() = runBlocking<Unit> {
    val clientList = (1..9).toSet()

    val intFlow = flow {
        var count = 0
        while (true) {
            val value = Random.nextInt()
            println("Goind to emit $value")
            emit(value)
            delay(500.milliseconds)
            count++
            if (count == 4) {
                throw RuntimeException("Server error")
            }
        }
    }
        .retry {
            println(it)
            delay(1.seconds)
            true
        }

    intFlow.collect { value ->
        clientList.forEach { id ->
            delay(100)
            println("Send $value to client with id $id")
        }
    }
}
