package org.example.corountines

import kotlin.test.Test
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.example.coroutines.OrderProcessingChallenge
import org.example.coroutines.StockMarketRouterChallenge

class Channels {
    @Test fun orderProcessingChallenge() = runTest { OrderProcessingChallenge().execute() }

    @Test fun stockMarketRouterChallenge() = runTest { StockMarketRouterChallenge().execute() }

    @Test
    fun mutableSharedFlow() = runTest {
        withTimeoutOrNull(5000) {
            val broadcast = MutableSharedFlow<Int>(1)

            launch {
                broadcast
                    .onEach { println("#1 entered $it") }
                    .collect {
                        delay(300)
                        println("#1 $it")
                    }
            }
            launch {
                // while (true) {
                repeat(100) {
                    //delay(50)
                    broadcast.emit(it + 1)
                }
                // }
            }

            launch {
                delay(300)

                broadcast.collect { println("#2 $it") }
            }

            launch {
                delay(700)

                broadcast.collect { println("#3 $it") }
            }
        }
    }
}
