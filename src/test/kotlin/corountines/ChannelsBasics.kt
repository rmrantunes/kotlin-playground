package org.example.corountines

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ChannelsBasics {
//    fun channelTypes() {
//        val rendezvousChannel = Channel<String>()
//        val bufferedChannel = Channel<String>(10)
//        val conflatedChannel = Channel<String>(CONFLATED)
//        val unlimitedChannel = Channel<String>(UNLIMITED)
//    }

    @Test
    fun simpleChannels() = runTest {
        val channel = Channel<String>()

        launch {
            channel.send("A1")
            channel.send("A2")
            channel.send("A3")
            log("A done")
        }

        launch {
            channel.send("B1")
            channel.send("B2")
            log("B done")
        }

        launch {
            repeat(5) {
                val x = channel.receive()
                log(x)
            }
        }
    }

    private fun log(message: Any?) {
        println("[${Thread.currentThread().name}] $message")
    }
}