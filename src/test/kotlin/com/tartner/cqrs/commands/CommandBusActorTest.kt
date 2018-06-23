package com.tartner.cqrs.commands

import com.tartner.cqrs.actors.*
import io.kotlintest.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import org.junit.*
import org.slf4j.*
import java.util.*

data class TestCommand(val id: Int): Command
data class TestCommand2(val id: Int): Command

private val log = LoggerFactory.getLogger(CommandBusActorTest::class.java)

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
      receivedCommandDeferred.await()
    } }
  }

  @Test
  fun testCommandReceive() {
    runBlocking { withTimeout(500) {
      val commandChannel = Channel<Command>(Channel.UNLIMITED)

      val commandBus = CommandBusActor()

      commandBus.registerCommand(TestCommand::class, commandChannel)

      val receivedCommandDeferred = CompletableDeferred<Command>()

      launch { for (command in commandChannel) receivedCommandDeferred.complete(command) }

      commandBus.sendCommand(TestCommand(1))
      receivedCommandDeferred.await()
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

      receivedCommandDeferred.await()
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

  @Test
  fun testCommandFanOut() {
    runBlocking { withTimeout(15000) {
      val commandChannel = Channel<TestCommand>(Channel.UNLIMITED)
      val totalActors = 500
      val actors = (0..totalActors-1).map {TestFanOutActor(it, commandChannel)}.toList()

      val commandBus = CommandBusActor()

      commandBus.registerCommand(TestCommand::class, commandChannel)

      val totalCommands = 5_000
      for (i in (1..totalCommands)) {
        commandBus.sendCommand(TestCommand(i))
      }

      actors.forEach {it.close()}
      actors.forEach {it.join()}
      actors.forEach {log.debug("$it")}
      val sumOfCounters = actors.map { it.counter }.sum()
      sumOfCounters shouldBe totalCommands
    } }
  }
}


class TestFanOutActor(val id: Int, channel: Channel<TestCommand>)
  : AMonoActor<TestCommand>(mailbox = channel) {
  var random = Random()
  var counter = 0

  override suspend fun onMessage(message: TestCommand) {
    counter++
//    if (id != 0) {
//      val delay = Math.max(100, random.nextInt(id))
//      delay(delay)
//    }
  }

  override fun toString(): String {
    return "TestFanOutActor(id=$id, counter=$counter)"
  }
}
