package org.example.corountines

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds

/** @see - https://kotlinlang.org/docs/coroutine-context-and-dispatchers.html */
class ContextAndDispatchers {
    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    @Test
    fun simple() = runTest {
        launch { // context of parent, main runBlocking coroutine
            println("main runBlocking  -> thread name: ${Thread.currentThread().name}")
        }

        launch(Dispatchers.Unconfined) { // not confined - will work with main thread
            println("unconfined  -> thread name: ${Thread.currentThread().name}")
        }

        launch(Dispatchers.Default) { // will get dispatched to DefaultDispatcher
            println("default  -> thread name: ${Thread.currentThread().name}")
        }

        launch(newSingleThreadContext("MyOwnThread")) { // will get its own thread
            println("newSingleThreadContext  -> thread name: ${Thread.currentThread().name}")
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    @Test
    fun unconfinedVsConfined() = runTest {
        launch(Dispatchers.Unconfined) {
            println("Unconfined  -> working in: ${Thread.currentThread().name}")
            delay(500)
            println("Unconfined  -> after delay in: ${Thread.currentThread().name}")
            launch(newSingleThreadContext("MyOwnThread")) {}.join()
            println("Unconfined  -> after custom thread in: ${Thread.currentThread().name}")
        }

        launch {
            println("main runBlocking  -> working in: ${Thread.currentThread().name}")
            delay(1000)
            println("main runBlocking  -> after delay in: ${Thread.currentThread().name}")
        }

    }

    @OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
    @Test
    fun jumpingBetweenThreads() {
        newSingleThreadContext("Ctx1").use { ctx1 ->
            newSingleThreadContext("Ctx2").use { ctx2 ->
                runBlocking(ctx1) {
                    log("Started in ctx1")

                    withContext(ctx2) {
                        log("Working in ctx2")
                    }

                    log("Back to ctx1")
                }
            }
        }
    }

    @Test
    fun jobInTheContext() = runTest {
        println("My job is ${coroutineContext[Job]}")
    }

    @Test
    fun childrenOfCoroutine() = runTest {
        val request = launch {
            launch(Job()) {
                println("job1: I run in my own job and execute independently!")
                delay(1000)
                println("job1: I am not affected by cancellation of the request!")
            }

            launch {
                delay(100)
                println(
                    "job2: I am a child of the request coroutine!"
                )
                delay(1000)
                println("job2: I will not execute this line if my parent request is cancelled!")
            }
        }

        delay(500)
        request.cancel()
        println("main: Who has survived request cancellation?")
        delay(1000)
    }

    @Test
    fun parentalResponsibilities() = runTest {
        // launch a coroutine to process some kind of incoming request
        val request = launch {
            // launch a few children jobs
            repeat(3) {
                launch {
                    delay((it + 1) * 200L) // variable delay 200ms, 400ms, 600ms
                    println("Coroutine $it is done")
                }
            }

            println("request: I'm done and I don't explicitly join my children that are still active")
        }

        request.join() // wait for completion of the request, including all its children
        println("Now the processing of the request is complete!")
    }

    @Test
    fun namingCoroutinesForDebugging() = runTest {
        val v1 = async(CoroutineName("v1coroutine")) {
            delay(500)
            log("Computing v1")
            6
        }

        val v2 = async(CoroutineName("v2coroutine")) {
            delay(1000)
            log("Computing v2")
            7
        }

        println("The answer for v1 * v2 is ${v1.await() * v2.await()}")
    }

    @Test
    fun combiningContextElements() = runTest {
        launch(Dispatchers.Default + CoroutineName("test")) {
            println("I'm working in the thread ${Thread.currentThread().name}")
        }
    }

    @Test
    fun coroutineScopes() = runBlocking {
        class Activity {
            private val mainScope = CoroutineScope(Dispatchers.Default)

            fun destroy() {
                mainScope.cancel()
            }

            fun doSomething() {
                repeat(10) {
                    mainScope.launch {
                        delay((it + 1) * 200L)
                        println("main: Coroutine $it is done!")
                    }
                }
            }
        }

        val activity = Activity()
        activity.doSomething()
        println("Launched coroutines")
        delay(0.5.seconds)
        println("Destroying activity")
        activity.destroy()
        delay(1.seconds)
    }

    @Test
    fun threadLocalData() = runTest {
        val threadLocal = ThreadLocal<String?>() // declare thread-local variable

        threadLocal.set("main")
        println("Pre-main, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
        val job = launch(Dispatchers.Default + threadLocal.asContextElement("launch")) {
            println("Launch start, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
            yield()
            println("After yield, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
        }
        job.join()
        println("Post-main, current thread: ${Thread.currentThread()}, thread local value: '${threadLocal.get()}'")
    }
}


fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")
