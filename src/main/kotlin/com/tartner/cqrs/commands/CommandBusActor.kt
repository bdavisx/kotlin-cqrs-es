package com.tartner.cqrs.commands

import kotlinx.coroutines.experimental.channels.*
import org.slf4j.*
import java.util.concurrent.*

sealed class CommandBus
data class RegisterChannelAtAddress<T: Command>(val channel: SendChannel<T>, val address: String): CommandBus()
data class SendCommandToAddress<T: Command>(val address: String, val command: T): CommandBus()

/**
 Class that handles commands in the system. It allows registering a channel(s) to handle commands
 that come to a particular address. It also allows registering for a particular command class, which
 registers the channel(s) at the fully qualified name of the command class.
 */
class CommandBusActor {
  private val addressToChannels = ConcurrentHashMap<String, ChannelContainer>()
  private val log = LoggerFactory.getLogger(CommandBusActor::class.java)

  companion object {
    fun create() {
      val channel = actor<CommandBus> {
        val me = CommandBusActor()

        for (message in channel) me.onReceive(message)
      }
    }
  }

  private suspend fun onReceive(message: CommandBus) {
    when (message) {
      is RegisterChannelAtAddress<out Command> -> registerChannel(message)
      is SendCommandToAddress<*> -> sendCommandToAddress(message)
    }
  }

  private fun registerChannel(message: RegisterChannelAtAddress<out Command>) {
    val channels: ChannelContainer = addressToChannels.getOrElse(message.address, {ChannelContainer()})
    val channel: SendChannel<Nothing> = message.channel
    channels.add(channel)
  }

  private suspend fun sendCommandToAddress(message: SendCommandToAddress<*>) {
    val channels = addressToChannels[message.address]
    if (channels == null) {
      log.error("Command sent to address without handler: address: ${message.address}; command: ${message.command}")
    } else {
      channels.sendMessageToNextChannel(message.command)
    }
  }
}

class ChannelContainer {
  private var currentChannel = 0
  private val channels = mutableListOf<SendChannel<*>>()

  fun <T: Command> add(channel: SendChannel<T>) {
    channels.add(channel)
  }

  suspend fun sendMessageToNextChannel(command: Command) {
    // we're assuming there will *always* be at least 1 channel, if we allow remove, then this needs
    // to be updated
    if (currentChannel >= channels.size) { currentChannel = 0 }
    // TODO: There's got to be a better way to do this, but I've got something wrong in the generics
    // or need to specify 'out' somewhere
    val sendChannel: SendChannel<Command> = channels[currentChannel] as SendChannel<Command>
    sendChannel.send(command)
    currentChannel++
  }
}
