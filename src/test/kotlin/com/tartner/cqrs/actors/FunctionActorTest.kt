package com.tartner.cqrs.actors

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import org.junit.jupiter.api.Assertions.*
import org.kodein.di.*
import org.kodein.di.generic.*
import org.slf4j.*

val log = LoggerFactory.getLogger(TestSub::class.java)

fun main(args: Array<String>) {
  val kodein = Kodein {
    bind<TestSub>() with factory {channel: FunctionActorSendChannel -> TestSub(channel)}
  }

  runBlocking {
    log.debug("Starting runBlocking")
    val actor: TestSub = createFunctionActor<TestSub>(kodein).await()
    println(actor)
    actor.operationA()
    actor.operationB()
  }
}

class TestSub(channel: SendChannel<Task<*>>): FunctionActor(channel) {
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

internal class FunctionActorTest {

}
