package com.tartner.cqrs.actors

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.actors.*
import kotlinx.coroutines.experimental.channels.*
import org.slf4j.*
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
  private val log = LoggerFactory.getLogger(AAbstractActor::class.java)

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
      while (true) {
        val message = mailbox.receive()
        onMessage(message)
      }
    } catch (e: ClosedReceiveChannelException) {
      // just means that the mailbox was closed
      log.debug("Mailbox closed")
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
