package org.example.corountines

import kotlinx.coroutines.test.runTest
import org.example.coroutines.OrderProcessingChallenge
import kotlin.test.Test

class Channels {
    @Test
    fun orderProcessingChallenge() = runTest {
        OrderProcessingChallenge().execute()
    }
}