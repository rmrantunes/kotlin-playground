package org.example.corountines

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * @see - https://kotlinlang.org/docs/coroutines-basics.html
 * */
object CoroutinesBasics {
    suspend fun helloWorld() {
        println("Running: CoroutinesBasics.helloWorld()")
        coroutineScope { // this: CoroutineScope
            launch { // launch a new coroutine and continue
                delay(1000L) // non-blocking delay for 1 second (default time unit is ms)
                println("World") // print after delay
            }

            println("Hello") // main coroutine continues while a previous one is delayed
        }
    }

    suspend fun helloWorld2() = coroutineScope {
        println("Running: CoroutinesBasics.helloWorld2()")
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

    suspend fun explicitJob() = coroutineScope {
        println("Running: CoroutinesBasics.explicitJob()")
        val job = launch { // launch a new coroutine and keep a reference to its Job
            delay(1000L)
            println("World")
        }
        println("Hello")
        job.join() // wait until child coroutine completes
        println("Done")
    }
}

