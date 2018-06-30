package com.tartner.postgresql.messages

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.util.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.io.*
import java.net.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

class StartupMessage(val protocolVersionNumber: Int, val parameters: List<Pair<String, Any>>) {}

class StartupMessageEncoder() {
}

  fun main(args: Array<String>) {
    runBlocking {
      val socket = aSocket(ActorSelectorManager(CommonPool)).tcp().connect(InetSocketAddress("127.0.0.1", 5432))
      val input = socket.openReadChannel()
      val output = socket.openWriteChannel(autoFlush = true)

      val allBytes = createAllBytes()

      println(allBytes.toString())

      println(output.writeAvailable(allBytes))

      val readIn = ByteArray(1024)
      val response = input.readAvailable(readIn)
      println("Server said: '$response'")
    }
  }

  private fun createAllBytes(): ByteArray {
    val buffer: ByteBuffer = ByteBuffer.allocate(1024)

    buffer.position(Integer.BYTES)

    buffer.putShort(3)
    buffer.putShort(0)
    buffer.put("user".toByteArray())
    buffer.put(0)
    buffer.put("bamboozle".toByteArray())
    buffer.put(0)
    buffer.put(0)
    val size = buffer.position()
    buffer.rewind()
    buffer.putInt(size)
    buffer.position(size)
    buffer.limit(size)

    val byteArray = ByteArray(size) {it -> buffer[it]}
    val array = byteArray.copyOf(size)
    return array
  }

