package com.tartner.cqrs.commands

import io.kotlintest.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import org.junit.*
import org.slf4j.*

data class TestCommand(val id: Int): Command
data class TestCommand2(val id: Int): Command

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
  fun testCommandReceive() {
    runBlocking { withTimeout(500) {
      val commandChannel = Channel<Command>(Channel.UNLIMITED)

      val commandBus = CommandBusActor()

      val address = "1"
      commandBus.registerCommand(TestCommand::class, commandChannel)

      val receivedCommandDeferred = CompletableDeferred<Command>()

      launch { for (command in commandChannel) receivedCommandDeferred.complete(command) }

      commandBus.sendCommand(TestCommand(1))
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

      // chanel 1 should be blocked after 1 message (there are no listeners)
      commandBus.sendCommandToAddress("1", TestCommand(1))
      commandBus.sendCommandToAddress("1", TestCommand(2))
      commandBus.sendCommandToAddress("2", TestCommand(3))

      val receivedCommand =  receivedCommandDeferred.await()

      receivedCommand shouldNotBe null

      log.debug(receivedCommand.toString())
    } }
  }

  @Test
  fun testMultipleCommandsAtAddreses() {
    runBlocking { withTimeout(500) {
      val commandChannel1 = Channel<Command>(1)
      val commandChannel2 = Channel<Command>(1)

      val commandBus = CommandBusActor()

      commandBus.registerChannelAtAddress("1", commandChannel1)
      commandBus.registerChannelAtAddress("2", commandChannel2)

      val launches = listOf(CompletableDeferred<Command>(),
        CompletableDeferred(), CompletableDeferred())

      launch { var count = 0; for (command in commandChannel1) launches[count++].complete(command) }
      launch { for (command in commandChannel2) launches[2].complete(command) }

      launch { commandBus.sendCommandToAddress("1", TestCommand(1)) }
      launch { commandBus.sendCommandToAddress("1", TestCommand(2)) }
      launch { commandBus.sendCommandToAddress("2", TestCommand(3)) }

      launches.awaitAll()
    } }
  }

  @Test
  fun testMultipleCommands() {
    runBlocking { withTimeout(500) {
      val commandChannel1 = Channel<Command>(1)
      val commandChannel2 = Channel<Command>(1)

      val commandBus = CommandBusActor()

      commandBus.registerCommand(TestCommand::class, commandChannel1)
      commandBus.registerCommand(TestCommand2::class, commandChannel2)

      val launches = listOf(CompletableDeferred<Command>(),
        CompletableDeferred(), CompletableDeferred())

      launch { var count = 0; for (command in commandChannel1) launches[count++].complete(command) }
      launch { for (command in commandChannel2) launches[2].complete(command) }

      launch { commandBus.sendCommand(TestCommand(1)) }
      launch { commandBus.sendCommand(TestCommand(2)) }
      launch { commandBus.sendCommand(TestCommand2(3)) }

      launches.awaitAll()
    } }
  }
}
