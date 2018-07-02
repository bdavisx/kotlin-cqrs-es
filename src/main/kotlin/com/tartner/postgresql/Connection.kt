package com.tartner.postgresql

import com.tartner.cqrs.actors.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.network.sockets.Socket
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.io.*
import java.net.*
import java.nio.ByteBuffer

/*
There needs to be a "central" control. It's the one that has the socket, input, output. The state's
receive function will be called once we have a message. It looks like the message always have the
size as part of the message, so we s/b able to get all of it.

The flow goes:

Connect is called; state must be Unconnected

Then the state is set to startup message sent and the startup message is sent

The StartupMessageSent state watches for the following possible messages:

  ErrorResponse
  AuthenticationMD5Password
  AuthenticationOk

  AuthenticationKerberosV5
  AuthenticationCleartextPassword
  AuthenticationSCMCredential
  AuthenticationGSS
  AuthenticationSSPI
  AuthenticationGSSContinue
  AuthenticationSASL
  AuthenticationSASLContinue
  AuthenticationSASLFinal
  NegotiateProtocolVersion

 */

/*
Each connection will have a state, this is the startup state basically - we need to stream
the output from postgresql (like to a multiple listener capable rx or channel)
There will be a listener that calls the correct state object with either a translated message
or the message buffer. Seems like central translation isn't possible though, there's no id byte
on the startup messages.

For the read, need to launch a coroutine that will fire a message back to the actor's channel
once data is received. As far as writing goes, would we use the suspending write or launch?
We're looking for a response (typically), so suspend seems to make sense - although if the
read channel is on a separate coroutine, then that won't work. I think the separate coroutine
is wrong now.
 */

class Connection(val configuration: Configuration): FunctionActor() {
  private enum class State { Unconnected, Connected, Error }

  // TODO: set by configuration
  private val inputArray = ByteArray(configuration.maximumMessageSize)

  private lateinit var socket: Socket
  private lateinit var input: ByteReadChannel
  private lateinit var output: ByteWriteChannel

  suspend fun connect() = actAndReply {
    // TODO: if already connected, error; need to check for errors in connection too
    // TODO: I don't think we should hard code CommonPool here
    socket = aSocket(ActorSelectorManager(CommonPool)).tcp()
      .connect(configuration.host, configuration.port)

    input = socket.openReadChannel()
    output = socket.openWriteChannel(autoFlush = true)

    val startupMessage = createStartupMessage()

    output.writeAvailable(startupMessage)

    val response = input.readAvailable(inputArray)
    inputArray
  }

  private fun createStartupMessage(): ByteArray {
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
}
