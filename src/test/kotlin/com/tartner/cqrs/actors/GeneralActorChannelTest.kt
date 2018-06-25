package com.tartner.cqrs.actors

import com.tartner.cqrs.commands.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import java.util.concurrent.atomic.*

//fun produceNumbers() = produce<Int> {
//  var x = 1 // start from 1
//  while (true) {
//    send(x++) // produce next
//    delay(100) // wait 0.1s
//  }
//}

class Receiver(val id: Int, val channel: ReceiveChannel<TestCommands>) {
  val counter = AtomicInteger(0)

  init { launch {
    for (msg in channel) {
//      println("Processor #$id received $msg")
      counter.getAndIncrement()
    }
  } }
}

fun main(args: Array<String>) = runBlocking<Unit> {
  val commandChannel = Channel<TestCommands>(Channel.UNLIMITED)
  val totalReceivers = 30
  val receivers = (1..totalReceivers).map {Receiver(it, commandChannel)}.toList()

  val totalCommands = 5_000_000
  repeat(totalCommands) {
    launch { commandChannel.send(TestCommand(it)) }
    yield()
  }
  commandChannel.close()

  receivers.forEach {println("Receiver: ${it.id} - Count: ${it.counter.get()}")}
  val totalCount = receivers.map {it.counter.get()}.sum()
  println("Total: $totalCount")
}
