package org.example.corountines

import kotlinx.coroutines.test.runTest
import org.example.coroutines.OrderProcessingChallenge
import org.example.coroutines.StockMarketRouterChallenge
import kotlin.test.Test

class Channels {
    @Test
    fun orderProcessingChallenge() = runTest {
        OrderProcessingChallenge().execute()
    }

    @Test
    fun stockMarketRouterChallenge() = runTest {
        StockMarketRouterChallenge().execute()
    }
}