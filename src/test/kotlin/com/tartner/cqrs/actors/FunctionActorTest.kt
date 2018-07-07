package com.tartner.cqrs.actors

import com.tartner.cqrs.commands.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import org.junit.*
import org.slf4j.*
import java.util.concurrent.*

val log = LoggerFactory.getLogger(TestSub::class.java)

internal class FunctionActorTest {
  private val log = LoggerFactory.getLogger(FunctionActorTest::class.java)

  @Test
  fun test() {
    log.debug("Start")
    runBlocking {
      val actors = ConcurrentLinkedQueue<TestSub>()
      for (x in 1..10000) {
        val actor = TestSub(x, Channel(Channel.UNLIMITED))
        actors.add(actor)
        launch {
          com.tartner.cqrs.actors.log.debug("TestSub actor: $actor, calling opA")
          actor.operationA()
          com.tartner.cqrs.actors.log.debug("TestSub actor: $actor, calling opB")
          val opB = actor.operationB()
          com.tartner.cqrs.actors.log.debug("OpB result = $opB")
          com.tartner.cqrs.actors.log.debug("TestSub actor: $actor, after calling opB")
          actor.close()
        }
      }
      actors.forEach {it.join()}
      com.tartner.cqrs.actors.log.debug("Done")
    }
  }

}

class TestSub(val id: Int, mailbox: Channel<Task<*>>)
  : FunctionActor(ActorContext(mailbox = mailbox)) {
  
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

