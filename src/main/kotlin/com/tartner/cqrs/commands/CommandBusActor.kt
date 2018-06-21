package com.tartner.cqrs.commands

import com.tartner.cqrs.actors.*
import kotlinx.coroutines.experimental.channels.*
import org.slf4j.*
import java.util.concurrent.*

 /**
 Class that handles commands in the system. It allows registering a channel(s) to handle commands
 that come to a particular address. It also allows registering for a particular command class, which
 registers the channel(s) at the fully qualified name of the command class.
 */
class CommandBusActor(channel: FunctionActorSendChannel): FunctionActor(channel) {
  private val addressToChannels = ConcurrentHashMap<String, ChannelContainer>()
  private val log = LoggerFactory.getLogger(CommandBusActor::class.java)

  suspend fun <T: Command> registerChannel(channel: SendChannel<T>, address: String) = act {
    val channels: ChannelContainer = addressToChannels.getOrElse(address, {ChannelContainer()})
    channels.add(channel)
  }

  suspend fun <T: Command> sendCommandToAddress(address: String, command: T) {
    val channels = addressToChannels[address]
    if (channels == null) {
      log.error("Command sent to address without handler: address: ${address}; command: ${command}")
    } else {
      // TODO: fix - this could end up blocking if the actor/channel being "called" blocks, we need
      // to figure out a way around this - can't we just launch a new coroutine for each call?
      // TODO: we may not need to worry about multiple channels, according to a doc I saw, multiple
      // actors (coroutines) can listen to the same channel
      channels.sendMessageToNextChannel(command)
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
