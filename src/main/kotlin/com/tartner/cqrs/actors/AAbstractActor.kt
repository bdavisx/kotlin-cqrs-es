package com.tartner.cqrs.actors

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.actors.*
import kotlinx.coroutines.experimental.channels.*
import kotlin.coroutines.experimental.*

/**
 * Base class for actors implementation, which provides implementation for [ActorTraits]
 *
 * @param T type of messages which are stored in the mailbox
 */
abstract class AAbstractActor<T>(
  protected val context: CoroutineContext = DefaultDispatcher,
  protected val parent: Job? = null,
  protected val start: CoroutineStart = CoroutineStart.DEFAULT,
  protected val mailbox: Channel<T> = Channel<T>(Channel.UNLIMITED)
): ActorTraits() {

  final override val job: Job

  init {
    job = launch(context, start, parent) {
      actorLoop()
    }
    job.invokeOnCompletion { onClose() }
  }

  override fun close() {
    mailbox.close()
  }

  override fun kill() {
    job.cancel()
    mailbox.cancel()
  }

  private suspend fun actorLoop() {
    var exception: Throwable? = null
    try {
      for (message in mailbox) {
        onMessage(message)
      }
    } catch (e: Throwable) {
      exception = e
      handleCoroutineException(coroutineContext, e)
    } finally {
      job.cancel(exception)
      mailbox.close()
    }
  }

  protected abstract suspend fun onMessage(message: T)
}
