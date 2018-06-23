package com.tartner.cqrs.actors

import com.tartner.cqrs.actors.FunctionActor.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import org.slf4j.*
import kotlin.coroutines.experimental.*

typealias FunctionActorSendChannel = Channel<Task<*>>

/**
 An actor that is designed to allow it's functions to be called directly (not thru the Channel),
 and then the actor itself will send the code thru the channel.

 WARNING: Do no use the actor function default capacity of 0 or the actor will block on sending
 itself a message.
 */
abstract class FunctionActor(
  context: CoroutineContext = DefaultDispatcher,
  parent: Job? = null,
  start: CoroutineStart = CoroutineStart.DEFAULT,
  mailbox: Channel<Task<*>> = Channel(Channel.UNLIMITED)
  ) : AAbstractActor<Task<*>>(context, parent, start, mailbox) {

  suspend fun <T: Any> act(block: suspend () -> T): Deferred<T> {
    val task = Task(block)
    mailbox.send(task)
    return task
  }

  suspend fun <T: Any> actAndReply(block: suspend () -> T): T = act(block).await()

  override suspend fun onMessage(message: Task<*>) = message.invoke()

  /** Used to run the code for an `act` or `actAndReply` functions. */
  class Task<T>(val block: suspend () -> T) : CompletableDeferred<T> by CompletableDeferred() {
    companion object {
      private val log = LoggerFactory.getLogger(FunctionActor::class.java)
    }

    suspend operator fun invoke() {
      try {
        log.debug("In invoke, calling complete")
        complete(block())
      } catch (t: Throwable) {
        log.debug("In invoke, caught exception", t)
        completeExceptionally(t)
      }
    }
  }
}
