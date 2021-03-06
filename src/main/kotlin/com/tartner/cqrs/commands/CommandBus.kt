package com.tartner.cqrs.commands

import kotlinx.coroutines.experimental.*
import kotlinx.coroutines.experimental.channels.*
import org.slf4j.*
import java.util.concurrent.*
import kotlin.reflect.*

/**
Class that handles commands in the system. It allows registering a channel(s) to handle commands
that come to a particular address. It also allows registering for a particular command class, which
registers the channel(s) at the fully qualified name of the command class.
 */
class CommandBus() {
  private val addressToChannel = ConcurrentHashMap<String, SendChannel<*>>()
  private val log = LoggerFactory.getLogger(CommandBus::class.java)

  fun <T: Any> registerCommand(commandClass: KClass<T>, channel: SendChannel<T>) =
    registerChannelAtAddress<T>(commandClass.qualifiedName!!, channel)

  fun <T: Any> registerChannelAtAddress(address: String, channel: SendChannel<T>) =
    addressToChannel.put(address, channel)

  suspend fun <T: Any> sendCommand(command: T) =
    sendCommandToAddress(command::class.qualifiedName!!, command)

  suspend fun <T: Any> sendCommandToAddress(address: String, command: T) {
    val channel = addressToChannel[address]
    if (channel == null) {
      log.error("Command sent to address without handler: address: ${address}; command: ${command}")
    } else {
      // launch to keep from blocking if the channel is "full"
      launch { (channel as SendChannel<T>).send(command) }
      yield()
    }
  }
}
