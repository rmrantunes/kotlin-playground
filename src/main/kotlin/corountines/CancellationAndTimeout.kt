package org.example.corountines

import kotlinx.coroutines.*

var acquired = 0

class CancellationAndTimeoutResource {
    init {
        acquired++
    }

    fun close() {
        acquired--
    }
}

object CancellationAndTimeout {
    suspend fun cancellingExecution() = coroutineScope {
        println("Running: CancellationAndTimeout.cancellingExecution()")

        val job = launch {
            repeat(1000) { i ->
                println("job: I'm sleeping $i...")
                delay(500L)
            }
        }

        delay(1300L)
        println("I'm tired of waiting!")
        job.cancel() // cancels the job
        job.join() // waits for job's completion
        // job.cancelAndJoin() // same thing as one
        println("main: Now I can quit.")
    }

    suspend fun cancellationIsCooperative1() = coroutineScope {
        val startTime = System.currentTimeMillis()
        val job = launch(Dispatchers.Default) {
            var nextPrintTime = startTime
            var i = 0

            while (i < 5) {
                if (System.currentTimeMillis() >= nextPrintTime) {
                    println("job: I'm sleeping ${i++}")
                    nextPrintTime += 500L
                }
            }
        }

        delay(1300L)
        println("main: I'm tired of waiting!")
        job.cancelAndJoin()
        println("main: Now I can quit.")
    }

    suspend fun cancellationIsCooperative2() = coroutineScope {
        val job = launch(Dispatchers.Default) {

            repeat(5) { i ->
                try {
                    println("job: I'm sleeping $i")
                    delay(500L)
                } catch (e: Exception) {
                    println(e)
                }
            }
        }

        delay(1300L)
        println("main: I'm tired of waiting!")
        job.cancelAndJoin()
        println("main: Now I can quit.")
    }

    suspend fun makingCoputationCodeCancellable() = coroutineScope {
        val startTime = System.currentTimeMillis()
        val job = launch(Dispatchers.Default) {
            var nextPrintTime = startTime
            var i = 0

            while (isActive) { // cancellable computation loop
                if (System.currentTimeMillis() >= nextPrintTime) {
                    println("job: I'm sleeping ${i++}")
                    nextPrintTime += 500L
                }
            }
        }

        delay(1300L)
        println("main: I'm tired of waiting!")
        job.cancelAndJoin()
        println("main: Now I can quit.")
    }

    suspend fun closingResourcesWithFinally() = coroutineScope {
        val job = launch(Dispatchers.Default) {

            try {
                repeat(1000) { i ->
                    println("job: I'm sleeping $i")
                    delay(500L)
                }
            } finally {
                println("job: I'm running finally")
            }
        }

        delay(1300L)
        println("main: I'm tired of waiting!")
        job.cancelAndJoin()
        println("main: Now I can quit.")
    }

    suspend fun runNonCancellableBlock() = coroutineScope {
        val job = launch(Dispatchers.Default) {

            try {
                repeat(1000) { i ->
                    println("job: I'm sleeping $i")
                    delay(500L)
                }
            } finally {
                withContext(NonCancellable) {
                    println("job: I'm running finally")
                    delay(1000L)
                    println("job: And I've just delayed for 1 sec because I'm non-cancellable")
                }
            }
        }

        delay(1300L)
        println("main: I'm tired of waiting!")
        job.cancelAndJoin()
        println("main: Now I can quit.")
    }

    suspend fun timeout() = coroutineScope {
        withTimeout(1300L) {
            repeat(1000) { i ->
                println("I'm sleeping $i...")
                delay(500L)
            }
        }
    }

    suspend fun timeoutOrNull() = coroutineScope {
        val result = withTimeoutOrNull(1300L) {
            repeat(1000) { i ->
                println("I'm sleeping $i...")
                delay(500L)
            }

            "Done"
        }

        println("Result is $result")
    }

    suspend fun asynchronousTimeoutAndResources() {
        coroutineScope {
            repeat(10_000) {
                launch {
                    val resource = withTimeout(60) { // Timeout of 60 ms
                        delay(50) // Delay for 50 ms
                        CancellationAndTimeoutResource() // Acquire a resource and return it from withTimeout block
                    }
                    resource.close() // Release the resource
                }
            }
        }
        println(acquired)
    }

    suspend fun asynchronousTimeoutAndResourcesWorkaround() {
        coroutineScope {
            repeat(10_000) {
                var resource: CancellationAndTimeoutResource? = null
                launch {
                    try {
                        withTimeout(60) { // Timeout of 60 ms
                            delay(50) // Delay for 50 ms
                            resource =
                                CancellationAndTimeoutResource() // Acquire a resource and return it from withTimeout block
                        }
                    } finally {
                        resource?.close() // Release the resource
                    }
                }
            }
        }
        println(acquired)
    }
}