package org.example.corountines

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeoutOrNull
import org.example.coroutines.OrderProcessingChallenge
import org.example.coroutines.StockMarketRouterChallenge
import kotlin.test.Test

class Channels {
    @Test fun orderProcessingChallenge() = runTest { OrderProcessingChallenge().execute() }

    @Test fun stockMarketRouterChallenge() = runBlocking { StockMarketRouterChallenge().execute() }

    @Test
    fun mutableSharedFlow() = runTest {
        withTimeoutOrNull(5000) {
            val broadcast =
                MutableSharedFlow<Int>(1)

            launch {
                broadcast.collect {
                    delay(100)
                    println("#1 $it")
                }
            }
            launch {
                // while (true) {
                repeat(100) {
                    delay(50)
                    broadcast.emit(it + 1)
                }
                // }
            }

            launch { delay(300)

                broadcast.collect { println("#2 $it") } }

            launch { delay(700)

                broadcast.collect { println("#3 $it") } }
        }
    }
}
