package org.example.corountines

fun log(msg: String) = println("[${Thread.currentThread().name}] $msg")
