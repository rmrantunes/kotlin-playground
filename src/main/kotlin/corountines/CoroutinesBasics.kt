package org.example.corountines

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @see - https://kotlinlang.org/docs/coroutines-basics.html
 * */
object CoroutinesBasics {
    suspend fun helloWorld() {
        coroutineScope { // this: CoroutineScope
            launch { // launch a new coroutine and continue
                delay(1000L) // non-blocking delay for 1 second (default time unit is ms)
                println("World") // print after delay
            }

            println("Hello") // main coroutine continues while a previous one is delayed
        }
    }

    suspend fun helloWorld2() = coroutineScope {
        launch {
            delay(2000L)
            println("World 2")
        }
        launch {
            delay(1000L)
            println("World 1")
        }

        println("Hello")
    }
}

