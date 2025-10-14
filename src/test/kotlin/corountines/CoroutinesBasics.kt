package org.example.corountines

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

/**
 * @see - https://kotlinlang.org/docs/coroutines-basics.html
 * */
class CoroutinesBasics {
    @Test
    fun helloWorld() = runTest {
        println("Running: CoroutinesBasics.helloWorld()")
        coroutineScope { // this: CoroutineScope
            launch { // launch a new coroutine and continue
                delay(1000L) // non-blocking delay for 1 second (default time unit is ms)
                println("World") // print after delay
            }

            println("Hello") // main coroutine continues while a previous one is delayed
        }
    }

    @Test
    fun helloWorld2() = runTest {
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

    @Test
    fun explicitJob() = runTest {
        println("Running: CoroutinesBasics.explicitJob()")
        val job = launch { // launch a new coroutine and keep a reference to its Job
            delay(1000L)
            println("World")
        }
        println("Hello")
        job.join() // wait until child coroutine completes
        println("Done")
    }

    @Test
    fun lightWeight() = runTest {
        println("Running: CoroutinesBasics.lightWeight()")
        repeat(50_000) {
            launch {
                delay(5000L)
                print(".")
            }
        }
    }
}

