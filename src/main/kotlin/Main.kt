package org.example

import kotlinx.coroutines.runBlocking
import org.example.corountines.ChannelsBasics
import org.example.corountines.CoroutinesBasics

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
fun main() {
    runBlocking { // this: CoroutineScope, bridges coroutine world with main thread world, blocking the thread
//        CoroutinesBasics.helloWorld2()
//        CoroutinesBasics.helloWorld2()
//        println("Done")
//        CoroutinesBasics.explicitJob()
//        CoroutinesBasics.lightWeight()

        ChannelsBasics.simpleChannels()
    }
}