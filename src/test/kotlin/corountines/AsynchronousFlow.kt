package org.example.corountines

import kotlin.test.Test
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
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

        (1..3).asFlow().map { value -> performRequest(value) }.collect { value -> println(value) }
    }

    @Test
    fun transform() = runTest {
        suspend fun performRequest(id: Int): String {
            delay(1000L)
            return "request #$id"
        }

        (1..3).asFlow().transform { value ->
            emit("Making request #$value")
            emit(performRequest(value))
        }.collect { value -> println(value) }
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

    @Test
    fun flowContextPitfall() = runTest {
        fun simple() = flow<Int> {
            // The WRONG way to change context for CPU-consuming code in flow builder
            withContext(Dispatchers.Default) { // emit() will throw an exception
                for (i in 1..3) {
                    Thread.sleep(500L)
                    emit(i)
                }
            }
        }

        simple().collect { value -> println(value) }
    }

    @Test
    fun flowOnOperators() = runTest {
        fun simple() = flow<Int> {
            for (i in 1..3) {
                Thread.sleep(500L)
                log("Emitting: $i")
                emit(i)
            }
        }.flowOn(Dispatchers.Default)

        simple().collect { value -> log("Collected: $value") }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun buffering() = runTest {
        fun simple() = flow {
            for (i in 1..3) {
                delay(150)
                emit(i)
            }
        }

        val time = currentTime

        simple().collect { value ->
            delay(300)
            println(value)
        }

        println("Collected in ${currentTime - time} ms - without buffer")

        val time2 = currentTime
        simple().buffer().collect { value ->
            delay(300)
            println(value)
        }

        println("Collected in ${currentTime - time2} ms - with buffer")

    }

    @Test
    fun conflation() = runTest {
        fun simple() = flow {
            for (i in 1..3) {
                delay(150)
                emit(i)
            }
        }

        val time = currentTime
        simple().conflate() // conflate emissions, don't process each one
            .collect { value ->
                delay(300)
                println(value)
            }
        // while the first number was still being processed the second, and third were already produced,
        // so the second one was conflated and only the most recent (the third one) was delivered to the collector
        println("Collected in ${currentTime - time} ms")
    }

    @Test
    fun processingLatestValue() = runTest {
        fun simple() = flow {
            for (i in 1..3) {
                delay(100)
                emit(i)
            }
        }

        val time = currentTime
        simple().collectLatest { value ->
            println("Collecting $value")
            delay(300) // While suspended, if a new value is emitted, then the current value collection
            // execution is cancelled
            println("Collected indeed $value")
        }
        println("Collected in ${currentTime - time} ms")
    }

    @Test
    fun composeFlowsWithZip() = runTest {
        val nums = (1..3).asFlow()
        val strings = flowOf("one", "two", "three")
        nums.zip(strings) { a, b ->
            "$a -> $b"
        }.collect { value -> println(value) }
    }

    @Test
    fun composeFlowsWithCombine() = runTest {
        println("When both values are match in order:")
        val startTime = currentTime
        val nums = (1..3).asFlow().onEach { delay(300) }
        val strings = flowOf("one", "two", "three").onEach { delay(400) }
        nums.zip(strings) { a, b ->
            "$a -> $b"
        }.collect { value ->
            println("with zip: '$value' at ${currentTime - startTime} ms from start")
        }
        println("-".repeat(50))
        println("When any value is emitted:")
        val startTime2 = currentTime
        nums.combine(strings) { a, b ->
            "$a -> $b"
        }.collect { value ->
            println("with combine: '$value' at ${currentTime - startTime2} ms from start")
        }
    }

    @Test
    fun flatteningFlows() = runTest {
        fun requestFlow(i: Int): Flow<String> = flow {
            emit("$i: First")
            delay(500) // wait 500 ms
            emit("$i: Second")
        }

        val startTime = currentTime
        (1..3).asFlow().onEach { delay(100) }.flatMapConcat { requestFlow(it) }.collect { value ->
            println("flatMapConcat: $value collected ${currentTime - startTime} ms from start")
        }

        println("-".repeat(50))

        val startTime2 = currentTime
        (1..3).asFlow().onEach { delay(100) }.flatMapMerge { requestFlow(it) }.collect { value ->
            println("flatMapMerge: $value collected ${currentTime - startTime2} ms from start")
        }

        println("-".repeat(50))

        val startTime3 = currentTime
        (1..3).asFlow().onEach { delay(100) }.flatMapLatest { requestFlow(it) }.collect { value ->
            println("flatMapLatest: $value collected ${currentTime - startTime3} ms from start")
        }
    }

    @Test
    fun collectorTryAndCatch() = runTest {
        fun simple() = flow {
            for (i in 1..3) {
                println("Emitting $i")
                emit(i)
            }
        }

        try {
            simple().collect { value ->
                println(value)
                check(value <= 1) { "Collected $value" }
            }
        } catch (e: Throwable) {
            println("Caught $e")
        }
    }

    @Test
    fun everythingIsCaught() = runTest {
        fun simple(): Flow<String> = flow {
            for (i in 1..3) {
                println("Emitting $i")
                emit(i)
            }
        }.map { value ->
            check(value <= 1) { "Collected $value" }
            "string $value"
        }

        try {
            simple().collect { value ->
                println(value)
            }
        } catch (e: Throwable) {
            println("Caught $e")
        }
    }

    @Test
    fun transparentCatch() = runTest {
        fun simple() = flow {
            for (i in 1..3) {
                println("Emitting $i")
                emit(i)
            }
        }

        simple().catch { e -> println("Caught $e") } // Does not catch downstream exceptions
            .collect { value ->
                check(value > 1) { "Collected $value" }
                println(value)
            }
    }

    @Test
    fun catchingDeclaratively() = runTest {
        fun simple() = flow {
            for (i in 1..3) {
                println("Emitting $i")
                emit(i)
            }
        }

        simple().onEach { value ->
            check(value <= 1) { "Collected $value" }
            println(value)
        }.catch { e -> println("Caught $e") }.collect()
    }

    @Test
    fun imperativeFinallyBlock() = runTest {
        fun simple() = (1..3).asFlow()

        try {
            simple().collect { value -> println(value) }
        } finally {
            println("Done")
        }
    }

    @Test
    fun declarativeFinallyBlock() = runTest {
        fun simple() = (1..3).asFlow()
        fun simpleWithException() = flow {
            emit(1)
            throw RuntimeException()
        }

        simple().onCompletion { println("Done") }.collect { value -> println(value) }

        simpleWithException().onCompletion { cause -> if (cause != null) println("Flow completed exceptionally") }
            .catch { e -> println("Caught $e") }.collect { value -> println(value) }
    }

    @Test
    fun launchingFlow() = runTest {
        fun events(): Flow<Int> = (1..3).asFlow().onEach { delay(100) }

        events().onEach { value -> println("Event: $value") }.collect() // <- awaits for completion

        println("Done. I waited")

        events().onEach { value -> println("Event in coroutine: $value") }
            .launchIn(this) // <- collects in a new coroutine

        println("Done too. I ain't waiting")
    }

    @Test
    fun flowCancellationChecks1() = runTest {
        fun builderFlow(): Flow<Int> = flow {
            for (i in 1..5) {
                println("Emitting $i")
                emit(i)
            }
        }

        builderFlow()
            .collect { value ->
                if (value == 3) cancel()
                println(value)
            }
    }

    @Test
    fun flowCancellationChecks2() = runTest {
        // most other flow operators (such as IntRange.asFlow()) do not do additional cancellation checks on their own
        // for performance reasons
        fun intRangeFlow() = (1..5).asFlow()
        intRangeFlow()
            .collect { value ->
            if (value == 3) cancel()
            println(value)
        }
    }

    @Test
    fun flowCancellationChecks3() = runTest {
        // To make it work you can add `.onEach { currentCoroutineContext().ensureActive() }` or `.cancellable()`
        fun intRangeFlow() = (1..5).asFlow()
        intRangeFlow()
            .cancellable()
            .collect { value ->
                if (value == 3) cancel()
                println(value)
            }
    }

    @OptIn(FlowPreview::class)
    @Test
    fun sensorReadingChallenge() = runTest {
        data class SensorReading(
            val sensorId: String,
            val temperature: Double,
            val timestamp: Long,
        )

        suspend fun monitorSensors(readings: Flow<SensorReading>): Flow<String> = coroutineScope {
            val meanReadingsQuantity = 5
            val map = mutableMapOf<String, MutableList<SensorReading>>()

            readings
                .onEach {
                    if (map[it.sensorId] != null) map[it.sensorId]?.add(it)
                    else map[it.sensorId] = mutableListOf(it)
                }
                .map { reading ->
                    val sensorReadings = map[reading.sensorId]
                    if (sensorReadings?.size?.rem(meanReadingsQuantity) == 0) {
                        sensorReadings
                            .takeLast(meanReadingsQuantity)
                            .sumOf { rec -> rec.temperature }
                            .let { sum ->
                                val mean = sum / meanReadingsQuantity
                                if (sum / meanReadingsQuantity > 70.0)
                                    return@map "🔥 Média alta: Sensor ${reading.sensorId} média=${mean}°C"
                            }
                    }

                    if (reading.temperature >= 75.0)
                        "⚠\uFE0F Alerta: Sensor ${reading.sensorId} com ${reading.temperature}°C às ${reading.timestamp}\""
                    else ""
                }
                .filter { it.isNotEmpty() }
        }

        val readings = flow {
            emit(SensorReading("A1", 60.0, 1))
            emit(SensorReading("A1", 78.5, 2))
            emit(SensorReading("A1", 79.0, 3))
            emit(SensorReading("A1", 71.0, 4))
            emit(SensorReading("A1", 73.0, 5))
            emit(SensorReading("A1", 74.0, 6))
            emit(SensorReading("A1", 74.0, 7))
        }

        monitorSensors(readings).collect { println(it) }
    }
}
