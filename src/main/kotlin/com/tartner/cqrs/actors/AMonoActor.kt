package com.tartner.cqrs.actors

import kotlinx.coroutines.experimental.actors.*
import kotlinx.coroutines.experimental.channels.*

/**
 * [MonoActor] is the base for all stateful actors, who have to process one [type][T] of message
 * [MonoActor] has well-defined lifecycle described in [ActorTraits].
 * [MonoActor.receive] method is used to declare a message handler, which is parametrized by [T]
 * to provide better compile-type safety.
 *
 * Example:
 * ```
 * class ExampleActor : MonoActor<String>() {
 *
 *   override suspend fun receive(string: String) = act {
 *     println("Received $string")
 *   }
 * }
 *
 * // Sender
 * exampleActor.send("foo")
 * ```
 *
 * @param T type of the message this actor can handle
 */
abstract class AMonoActor<T>(context: ActorContext<T> = ActorContext()): AAbstractActor<T>(context) {
  /**
   * Sends the message to the actor, which later will be processed by [receive]
   *
   * @throws ClosedSendChannelException if actor is [closed][close]
   */
  suspend fun send(message: T) {
    job.start()
    context.mailbox.send(message)
  }
}

