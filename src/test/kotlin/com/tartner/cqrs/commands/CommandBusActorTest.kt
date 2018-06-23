package com.tartner.cqrs.commands

import io.kotlintest.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import org.junit.*
import org.slf4j.*

data class TestCommand(val id: Int): Command

internal class CommandBusActorTest {
  private val log = LoggerFactory.getLogger(CommandBusActorTest::class.java)
  @Test
  fun testMessageReceive() {
    runBlocking { withTimeout(500) {
      val commandChannel = Channel<Command>(Channel.UNLIMITED)

      val commandBus = CommandBusActor()

      val address = "1"
      commandBus.registerChannelAtAddress(address, commandChannel)

      val receivedCommandDeferred = CompletableDeferred<Command>()

      launch { for (command in commandChannel) receivedCommandDeferred.complete(command) }

      commandBus.sendCommandToAddress(address, TestCommand(1))
      val receivedCommand =  receivedCommandDeferred.await()

      receivedCommand shouldNotBe null
    } }
  }

  @Test
  fun testNotBlocking() {
    runBlocking { withTimeout(500) {
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

      receivedCommand shouldNotBe null

      log.debug(receivedCommand.toString())
    } }
  }

  @Test
  fun testMultipleCommands() {
    runBlocking { withTimeout(500) {
      val commandChannel1 = Channel<Command>(1)
      val commandChannel2 = Channel<Command>(1)

      val commandBus = CommandBusActor()

      commandBus.registerChannelAtAddress("1", commandChannel1)
      commandBus.registerChannelAtAddress("2", commandChannel2)

      val launches = listOf<CompletableDeferred<Command>>(CompletableDeferred<Command>(),
        CompletableDeferred<Command>(), CompletableDeferred<Command>())

      launch { var count = 0; for (command in commandChannel1) launches[count++].complete(command) }
      launch { for (command in commandChannel2) launches[2].complete(command) }

      commandBus.sendCommandToAddress("1", TestCommand(1))
      commandBus.sendCommandToAddress("1", TestCommand(2))
      commandBus.sendCommandToAddress("2", TestCommand(3))

      launches.awaitAll()
    } }
  }
}
