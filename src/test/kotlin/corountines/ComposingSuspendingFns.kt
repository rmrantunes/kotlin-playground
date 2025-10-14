package org.example.corountines

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import kotlin.system.measureTimeMillis
import kotlin.test.Test

/**
 * @see - https://kotlinlang.org/docs/composing-suspending-functions.html
 * */
class ComposingSuspendingFns {

    suspend fun doSomethingUsefulOne(): Int {
        delay(1000L)
        return 13
    }

    suspend fun doSomethingUsefulTwo(): Int {
        delay(1000L)
        return 29
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun doSomethingUsefulOneAsync() = GlobalScope.async {
        doSomethingUsefulOne()
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun doSomethingUsefulTwoAsync() = GlobalScope.async {
        doSomethingUsefulTwo()
    }

    @Test
    fun sequentialByDefault() = runTest {
        val time = measureTimeMillis {
            val one = doSomethingUsefulOne()
            val two = doSomethingUsefulTwo()
            println("The answer is ${one + two}")
        }

        println("Completed in $time ms")
    }

    @Test
    fun concurrentUsingAsync() = runTest {
        val time = measureTimeMillis {
            val one = async { doSomethingUsefulOne() }
            val two = async { doSomethingUsefulTwo() }
            println("The answer is ${one.await() + two.await()}")
        }

        println("Completed in $time ms")
    }

    @Test
    fun lazyStartedAsync() = runTest {
        val time = measureTimeMillis {
            val one = async(start = CoroutineStart.LAZY) { doSomethingUsefulOne() }
            val two = async(start = CoroutineStart.LAZY) { doSomethingUsefulTwo() }
            one.start() // you can delegate the beginning of the execution with start()
            two.start()
            println("The answer is ${one.await() + two.await()}")
        }

        println("Completed in $time ms")
    }

    /**
     * Run this function directly inside main (no runBlocking)
     */
    @Test
    fun asyncStyleFunctions() {
        val time = measureTimeMillis {
            val one = doSomethingUsefulOneAsync()
            val two = doSomethingUsefulTwoAsync()

            runBlocking {
                println("The answer is ${one.await() + two.await()}")
            }
        }

        println("Completed in $time ms")
    }

    suspend fun concurrentSum(): Int = coroutineScope {
        val one = async { doSomethingUsefulOne() }
        val two = async { doSomethingUsefulTwo() }
        one.await() + two.await()
    }


    @Test
    fun structuredConcurrencyWithAsync() = runTest {
        val time = measureTimeMillis {
            println("The answer is ${concurrentSum()}")
        }
        println("Completed in $time ms")
    }

    @Test
    fun cancellationPropagation() = runTest {
        try {
            failedConcurrentSum()
        } catch (e: ArithmeticException) {
            println("Computation failed with ArithmeticException")
        }
    }

    suspend fun failedConcurrentSum() = coroutineScope {
        val one = async {
            try {
                delay(Long.MAX_VALUE)
                42
            } finally {
                println("First child was cancelled")
            }
        }

        val two = async<Int> {
            println("Second child throws an exception")
            throw ArithmeticException()
        }

        one.await() + two.await()
    }
}