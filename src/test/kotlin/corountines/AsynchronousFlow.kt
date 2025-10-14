package org.example.corountines

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class AsynchronousFlow {
    @Test
    fun representingMultipleValues() {
        fun simple() = listOf(1, 2, 3)
        simple().forEach { value -> println(value) }
    }

    @Test
    fun sequences() {
        fun simple(): Sequence<Int> = sequence {
            for (i in 1..3) {
                Thread.sleep(500)
                yield(i)
            }
        }

        simple().forEach { value -> println(value) }
    }

    @Test
    fun flows() = runTest {
        fun simple(): Flow<Int> = flow {
            for (i in 1..3) {
                delay(400L)
                emit(i)
            }
        }

        launch {
            for (i in 1..3) {
                println("I'm not blocked $i")
                delay(400L)
            }
        }

        simple().collect { value -> println(value) }
    }

    @Test
    fun flowsAreCold() = runTest {
        fun simple(): Flow<Int> = flow {
            println("Flow started")
            for (i in 1..3) {
                delay(100L)
                println("Emitting $i")
            }
        }

        println("Calling simple function...")
        val flow = simple()
        println("Calling collect...")
        flow.collect { value -> println(value) }
        println("Calling collect again...")
        flow.collect { value -> println(value) }
    }
}