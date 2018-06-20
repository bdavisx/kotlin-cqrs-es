package com.tartner.cqrs.actors

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import org.kodein.di.*
import org.kodein.di.generic.*
import org.slf4j.*
import kotlin.coroutines.experimental.*

val log = LoggerFactory.getLogger(TestSub::class.java)

fun main(args: Array<String>) {
  val kodein = Kodein {
    bind<TestSub>() with factory { channel: SendChannel<CoroutineActor.Task<*>> -> TestSub(channel) }
  }

  runBlocking {
    log.debug("Starting runBlocking")
    val actor: TestSub = create<TestSub>(kodein).await()
    println(actor)
    actor.operationA()
    actor.operationB()
  }
}

class TestSub(channel: SendChannel<Task<*>>): CoroutineActor(channel) {
  suspend fun operationA(): Deferred<Unit> {
    log.debug("Outside op a act")
    return act {
      log.debug("In operation A act")
      delay(100)
      log.debug("After delay in operation A act")
    }
  }

  // public actor operation with result
  suspend fun operationB(): Int = actAndReply {
    log.debug("In operation B actAndReply")
    delay(100)
    1
  }
}

/**
 The default values for the parameters are the same as the `actor` function except for `capacity`.
 WARNING: Do no use the default capacity of 0 or the actor will block on sending itself a message.
 */
inline fun <reified T: CoroutineActor> create(kodein: Kodein,
  context: CoroutineContext = DefaultDispatcher, capacity: Int = Channel.UNLIMITED,
  start: CoroutineStart = CoroutineStart.DEFAULT, parent: Job? = null
  ): Deferred<T> {
  val actorDeferred = CompletableDeferred<T>()

  actor<CoroutineActor.Task<*>>(context, capacity, start, parent) {
    val factory = kodein.direct.factory<SendChannel<CoroutineActor.Task<*>>, T>()
    val theActor = factory(channel)
    actorDeferred.complete(theActor)

    for (message in channel) theActor.runTask(message)
  }

  return actorDeferred
}

open class CoroutineActor(private val channel: SendChannel<Task<*>>) {

  suspend fun runTask(task: Task<*>) = task()

  suspend fun <T> act(block: suspend () -> T): Deferred<T> {
    val task = Task(block)
    channel.send(task)
    return task
  }

  suspend fun <T> actAndReply(block: suspend () -> T): T = act(block).await()

  class Task<T>(val block: suspend () -> T) : CompletableDeferred<T> by CompletableDeferred() {
    suspend operator fun invoke() {
      try {
        complete(block())
      } catch (t: Throwable) {
        completeExceptionally(t)
      }
    }
  }
}
