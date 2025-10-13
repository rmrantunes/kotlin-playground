package org.example.corountines

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

object ChannelsBasics {
//    fun channelTypes() {
//        val rendezvousChannel = Channel<String>()
//        val bufferedChannel = Channel<String>(10)
//        val conflatedChannel = Channel<String>(CONFLATED)
//        val unlimitedChannel = Channel<String>(UNLIMITED)
//    }

    suspend fun simpleChannels() = coroutineScope {
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