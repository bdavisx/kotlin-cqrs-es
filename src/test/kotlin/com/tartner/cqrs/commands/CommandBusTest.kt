package com.tartner.cqrs.commands

import com.tartner.cqrs.actors.*
import io.kotlintest.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import org.junit.*
import org.slf4j.*
import java.util.*
import java.util.concurrent.atomic.*

sealed class TestCommands(): Command
data class TestCommand(val id: Int): TestCommands()
data class TestCommand2(val id: Int): TestCommands()

internal class CommandBusTest {
  private val log = LoggerFactory.getLogger(CommandBusTest::class.java)

  @Test
  fun testMessageReceive() {
    runBlocking { withTimeout(500) {
      val commandChannel = Channel<Command>(Channel.UNLIMITED)

      val commandBus = CommandBus()

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

      val commandBus = CommandBus()

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

      val commandBus = CommandBus()

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

      val commandBus = CommandBus()

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

      val commandBus = CommandBus()

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
    runBlocking {
      withTimeout(2500) {
        val totalCommands = 50_000
        val commandChannel = Channel<TestCommand>(Channel.UNLIMITED)
        val totalActors = 10
        val actorJob = Job()
        val actors = (0..totalActors-1).map {TestFanOutActor(it, actorJob, commandChannel)}.toList()

        val commandBus = CommandBus()

        commandBus.registerCommand(TestCommand::class, commandChannel)

        for (i in (1..totalCommands)) {
          commandBus.sendCommand(TestCommand(i))
        }

        yield()
        log.debug("Delaying for catchup")
        delay(250)

        actors.forEach {it.close()}
        actors.forEach {log.debug("$it")}
        val sumOfCounters = actors.map { it.count }.sum()
        sumOfCounters shouldBe totalCommands
        actorJob.joinChildren()
      }
    }
  }

}

class TestFanOutActor(val id: Int, parent: Job, channel: Channel<TestCommand>)
  : AMonoActor<TestCommand>(ActorContext(parent = parent, mailbox = channel)) {
  var random = Random()
  var counter = AtomicInteger(0)

  val count: Int; get() = counter.get()

  override suspend fun onMessage(message: TestCommand) {
//    launch(job) { log.debug("Received $message") }
    counter.getAndIncrement()
  }

  override fun toString(): String {
    return "TestFanOutActor(id=$id, counter=$counter)"
  }
}
