package com.tartner.cqrs.actors

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import org.slf4j.*
import java.util.concurrent.*

val log = LoggerFactory.getLogger(TestSub::class.java)

fun main(args: Array<String>) {
//  val kodein = Kodein {
//    bind<TestSub>() with factory {channel: FunctionActorSendChannel -> TestSub(channel)}
//  }

  log.debug("Start")
  runBlocking {
    val actors = ConcurrentLinkedQueue<TestSub>()
    for (x in 1..10000) {
      val actor = TestSub(x, Channel(Channel.UNLIMITED))
      actors.add(actor)
      launch {
        log.debug("Starting runBlocking $x")
        log.debug("TestSub actor: $actor, calling opA")
        actor.operationA()
        log.debug("TestSub actor: $actor, calling opB")
        val opB = actor.operationB()
        log.debug("OpB result = $opB")
        log.debug("TestSub actor: $actor, after calling opB")
        actor.close()
      }
    }
    actors.forEach {it.join()}
    log.debug("Done")
  }
}

class TestSub(val id: Int, mailbox: Channel<Task<*>>): FunctionActor(mailbox = mailbox) {
  suspend fun operationA(): Deferred<Unit> {
    log.debug("Outside op a act $id")
    return act {
      if (id % 10 == 0) throw RuntimeException("Erroring $id")
      log.debug("In operation A act $id")
      delay(100)
      log.debug("After delay in operation A act $id")
    }
  }

  // public actor operation with result
  suspend fun operationB(): Int = actAndReply {
    log.debug("In operation B actAndReply $id")
    delay(100)
    id
  }

  override fun toString(): String {
    return "TestSub(id=$id)"
  }

}

internal class FunctionActorTest {

}
