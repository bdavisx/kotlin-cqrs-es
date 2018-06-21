package com.tartner.cqrs.actors

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*

fun produceNumbers() = produce<Int> {
  var x = 1 // start from 1
  while (true) {
    send(x++) // produce next
    delay(100) // wait 0.1s
  }
}

fun launchProcessor(id: Int, channel: ReceiveChannel<Int>) = launch {
  channel.consumeEach {
    println("Processor #$id received $it; ${Thread.currentThread().name}")
  }
}

fun main(args: Array<String>) = runBlocking<Unit> {
  val producer = produceNumbers()
  repeat(5) { launchProcessor(it, producer) }
  delay(950)
  producer.cancel() // cancel producer coroutine and thus kill them all
}
