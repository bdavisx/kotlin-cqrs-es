package com.tartner.cqrs.commands

import com.natpryce.hamkrest.assertion.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import org.junit.*

data class TestCommand(val id: Int): Command

internal class CommandBusActorTest {
  @Test
  fun testMessageReceive() {
    runBlocking {
      val commandChannel = Channel<Command>(Channel.UNLIMITED)

      val commandBus = CommandBusActor()

      val address = "1"
      commandBus.registerChannelAtAddress(address, commandChannel)

      val receivedCommandDeferred = CompletableDeferred<Command>()

      launch { for (command in commandChannel) receivedCommandDeferred.complete(command) }

      commandBus.sendCommandToAddress(address, TestCommand(1))
      val receivedCommand =  receivedCommandDeferred.await()
      println(receivedCommand)
    }
  }

  @Test
  fun testNotBlocking() {
    runBlocking {
      val commandChannel1 = Channel<Command>(1)
      val commandChannel2 = Channel<Command>(1)

      val commandBus = CommandBusActor()

      commandBus.registerChannelAtAddress("1", commandChannel1)
      commandBus.registerChannelAtAddress("2", commandChannel2)

      val receivedCommandDeferred = CompletableDeferred<Command>()

      launch { for (command in commandChannel2) receivedCommandDeferred.complete(command) }

      commandBus.sendCommandToAddress("1", TestCommand(1))
      commandBus.sendCommandToAddress("1", TestCommand(2))
      commandBus.sendCommandToAddress("2", TestCommand(3))

      val receivedCommand =  receivedCommandDeferred.await()
      println(receivedCommand)

    }
  }
}
