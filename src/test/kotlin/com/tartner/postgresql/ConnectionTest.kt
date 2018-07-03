package com.tartner.postgresql

import arrow.core.*
import io.kotlintest.*
import kotlinx.coroutines.experimental.*
import org.junit.*
import java.lang.Integer.*
import java.nio.*

internal class ConnectionTest {
  @Test
  fun test() {
    runBlocking {
      val configuration = Configuration(username = "checklist_user")
      val connection = Connection(configuration)

      val connectionResult: Either<Throwable, ByteBuffer> = connection.connect()
      when (connectionResult) {
        is Either.Right -> {
          val inputArray = connectionResult.b
          println(inputArray)

          val length = inputArray.getInt(1)
          println("length = $length")

          val text = StringBuilder()
          for(i in 0..min(13, length-1)) {
            val value = inputArray[i]
            text.append("${value.toString()} - 0x${value.toString(16)} - ${value.toChar()}\n")
          }
          println(text)
        }
        is Either.Left -> { fail("${connectionResult.a}") }
      }
    }
  }
}
