package org.example.corountines

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
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

    @Test
    fun flowCancellationBasics() = runTest {
        fun simple(): Flow<Int> = flow {
            println("Flow started")
            for (i in 1..3) {
                delay(100L)
                println("Emitting $i")
            }
        }

        withTimeoutOrNull(250L) {
            simple().collect { value -> println(value) }
        }

        println("Done")
    }

    @Test
    fun flowBuilders() = runTest {
        flowOf(1, 2, 3).collect { value -> println("flowOf: $value") }
        (1..3).asFlow().collect { value -> println("asFlow: $value") }
    }

    @Test
    fun intermediateFlowOperations() = runTest {
        suspend fun performRequest(id: Int): String {
            delay(1000L)
            return "request #$id"
        }

        (1..3).asFlow()
            .map { value -> performRequest(value) }
            .collect { value -> println(value) }
    }

    @Test
    fun transform() = runTest {
        suspend fun performRequest(id: Int): String {
            delay(1000L)
            return "request #$id"
        }

        (1..3).asFlow()
            .transform { value ->
                emit("Making request #$value")
                emit(performRequest(value))
            }
            .collect { value -> println(value) }
    }

    @Test
    fun sizeLimitingOperators() = runTest {
        fun numbers() = flow<Int> {
            try {
                emit(1)
                emit(2)
                println("This line will not execute")
                emit(3)
            } finally {
                println("Finally in numbers")
            }
        }

        numbers().take(2).collect { value -> println(value) }
    }

    @Test
    /**
     * @see - https://kotlinlang.org/docs/flow.html#terminal-flow-operators
     * */
    fun terminalFlowOperators() = runTest {
        val sum = (1..5).asFlow().map { it * it }.reduce { a, b -> a + b }// sum them (terminal operator)
        println(sum)
    }

    @Test
    fun flowsAreSequential() = runTest {
        (1..5).asFlow().filter {
            println("filter: $it")
            it % 2 == 0
        }.map {
            println("map: $it")
            "string $it"
        }.collect {
            println("collect: $it")
        }
    }
}