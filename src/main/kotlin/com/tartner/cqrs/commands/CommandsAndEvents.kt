package com.tartner.cqrs.commands

import com.fasterxml.jackson.annotation.*
import com.tartner.vertx.functional.*
import java.io.*
import java.util.*
import kotlin.reflect.*

/** Marker interface for objects that are internally serialized. */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
interface SerializableCQRSObject: Serializable

interface HasAggregateId {
  val aggregateId: UUID
}

interface HasAggregateVersion: HasAggregateId {
  val aggregateVersion: Long
}

/** Also acts as a correlation Id when needed. */
typealias CommandId = UUID

annotation class EventHandler
interface DomainEvent: SerializableCQRSObject

interface AggregateEvent: DomainEvent, HasAggregateVersion

/** This indicates an error happened that needs to be handled at a higher/different level. */
interface ErrorEvent: DomainEvent

annotation class CommandHandler
interface Command: SerializableCQRSObject
interface DomainCommand: Command
interface AggregateCommand: DomainCommand, HasAggregateId
interface CommandResponse: SerializableCQRSObject

interface Query: SerializableCQRSObject
interface QueryResponse: SerializableCQRSObject

interface AggregateSnapshot: SerializableCQRSObject, HasAggregateVersion

object SuccessReply: SerializableCQRSObject
val successReplyRight = SuccessReply.createRight()

interface FailureReply: SerializableCQRSObject
data class ErrorReply(val message: String, val sourceClass: KClass<*>): FailureReply
