package com.tartner.cqrs.actors

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import org.kodein.di.*
import org.kodein.di.generic.*
import kotlin.coroutines.experimental.*

typealias FunctionActorSendChannel = SendChannel<FunctionActor.Task<*>>

/**
 The default values for the parameters are the same as the `actor` function except for `capacity`.
 WARNING: Do no use the actor function default capacity of 0 or the actor will block on sending
 itself a message.
 */
inline fun <reified T: FunctionActor> createFunctionActor(kodein: Kodein,
  context: CoroutineContext = DefaultDispatcher, capacity: Int = Channel.UNLIMITED,
  start: CoroutineStart = CoroutineStart.DEFAULT, parent: Job? = null
  ): Deferred<T> {
  val actorDeferred = CompletableDeferred<T>()

  actor<FunctionActor.Task<*>>(context, capacity, start, parent) {
    val factory = kodein.direct.factory<FunctionActorSendChannel, T>()
    val theActor = factory(channel)
    actorDeferred.complete(theActor)

    for (message in channel) theActor.runTask(message)
  }

  return actorDeferred
}

/**
 An actor that is designed to allow it's functions to be called directly (not thru the Channel),
 and then the actor itself will send the code thru the channel.
 */
open class FunctionActor(private val channel: FunctionActorSendChannel) {

  suspend fun runTask(task: Task<*>) = task()

  suspend fun <T> act(block: suspend () -> T): Deferred<T> {
    val task = Task(block)
    channel.send(task)
    return task
  }

  suspend fun <T> actAndReply(block: suspend () -> T): T = act(block).await()

  /** Used to run the code for an `act` or `actAndReply` functions. */
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
