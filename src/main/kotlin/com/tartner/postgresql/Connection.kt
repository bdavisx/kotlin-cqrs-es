package com.tartner.postgresql

import arrow.core.*
import com.tartner.cqrs.actors.*
import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.io.*
import org.slf4j.*
import java.nio.ByteBuffer
import java.nio.channels.*

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
  companion object {
    const val majorVersion: Short = 3
    const val minorVersion: Short = 0
  }
  private val log = LoggerFactory.getLogger(Connection::class.java)

  private enum class State { Unconnected, Connected, Error }

  // TODO: set by configuration
  private val inputArray = ByteBuffer.allocate(configuration.maximumMessageSize)!!

  private lateinit var socket: Socket
  private lateinit var input: ByteReadChannel
  private lateinit var output: ByteWriteChannel

  suspend fun connect(): Either<Throwable, ByteBuffer> = actAndReply {
    // TODO: if already connected, error; need to check for errors in connection too
    // TODO: I don't think we should hard code CommonPool here
    socket = aSocket(ActorSelectorManager(CommonPool)).tcp()
      .connect(configuration.host, configuration.port)

    input = socket.openReadChannel()
    output = socket.openWriteChannel(autoFlush = true)

    val startupMessage = createStartupMessage(configuration)

    output.writeAvailable(startupMessage)

    var response = input.readAvailable(inputArray)
    if (response == -1) {
      // TODO: channel closed, need to return a failure, should it be this or something else?
      Either.left(ClosedChannelException())
    } else {
      val messageType = inputArray.get(0)
      val length = inputArray.getInt(1)

      // 52 == 'R' or Authentication message, has subtype
      if (messageType == 82.toByte()) {
        val messageSubtype = inputArray.getInt(1 + Integer.BYTES)

        // 5 == AuthenticationMD5Password
        if (messageSubtype == 5) {
          val saltArray = ByteArray(4)
          inputArray.position(1 + (2*Integer.BYTES))
          val salt = inputArray.get(saltArray, 0, 4)

          // TODO: check to make sure password exists
          val passwordBytes = MD5Digest.encode(
            configuration.username.toByteArray(),
            configuration.password!!.toByteArray(),
            saltArray)

          val messageSize = 1 + Integer.BYTES + passwordBytes.size
          val passwordMessage = ByteBuffer.allocate(messageSize+1)
          passwordMessage.put('p'.toByte())
          passwordMessage.putInt(messageSize)
          passwordMessage.put(passwordBytes)
          passwordMessage.put(0)
          passwordMessage.position(0)

          log.debug("Getting ready to write password message: $passwordMessage")
          val bytesWritten = output.writeAvailable(passwordMessage)

          log.debug("Getting ready to read the password message resposne. Bytes written: $bytesWritten")
          inputArray.position(0)
          response = input.readAvailable(inputArray)
          log.debug("Read response: $response; inputArray = $inputArray")
        }
      } else {
        Either.left(RuntimeException("Unknown messageType == $messageType"))
      }

      log.debug("Returning input array")
      Either.right(inputArray)
    }
  }

  private fun createStartupMessage(configuration: Configuration): ByteArray {
    val buffer: ByteBuffer = ByteBuffer.allocate(1024)

    buffer.position(Integer.BYTES)

    buffer.putShort(majorVersion)
    buffer.putShort(minorVersion)
    buffer.put("user".toByteArray())
    buffer.put(0)
    buffer.put(configuration.username.toByteArray())
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
