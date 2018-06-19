package com.tartner.cqrs.actors

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import org.slf4j.*

val log = LoggerFactory.getLogger(TestSub::class.java)

fun main(args: Array<String>) {
  runBlocking {
    log.debug("Starting runBlocking")
    val actor: TestSub = create({TestSub(it)}).await()
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

fun <T: CoroutineActor> create(factory: (channel: SendChannel<CoroutineActor.Task<*>>) -> T): Deferred<T> {
  val x = CompletableDeferred<T>()

  actor<CoroutineActor.Task<*>> {
    val me2 = factory.invoke(channel)
    x.complete(me2)
    for (message in channel) me2.runBlock(message)
  }

  return x
}
open class CoroutineActor(private val channel: SendChannel<Task<*>>) {

  suspend fun runBlock(task: Task<*>) = task()

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
