package com.tartner.postgresql

import kotlinx.coroutines.experimental.*
import org.junit.*

internal class ConnectionTest {
  @Test
  fun test() {
    runBlocking {
      val configuration = Configuration(username = "checklist_user")
      val connection = Connection(configuration)

      val inputArray: ByteArray = connection.connect()
      println(inputArray)

      val text = StringBuilder()
      for(i in 0..20) {
        val value = inputArray[i]
        text.append("${value.toString()} - 0x${value.toString(16)} - ${value.toChar()}\n")
      }
      println(text)
    }
  }
}
