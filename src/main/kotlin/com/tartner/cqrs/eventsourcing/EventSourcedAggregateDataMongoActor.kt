package com.tartner.cqrs.eventsourcing

import arrow.core.*
import com.mongodb.async.client.*
import com.tartner.cqrs.actors.*
import com.tartner.cqrs.commands.*
import com.tartner.utilities.jackson.*
import com.tartner.vertx.functional.*
import org.bson.*
import org.litote.kmongo.coroutine.*
import java.time.*
import kotlin.reflect.*

data class EventWrapper(
  val aggregateId: AggregateId,
  val aggregateClassName: String,
  /** The aggregate version at the start of the events. */
  val aggregateVersion: Long,
  val createdAt: Instant,
  val authenticatedUserId: String?,
  /** The Command that caused the creation of these events. */
  val commandId: CommandId?,
  val events: List<DomainEvent>)

class EventSourcedAggregateDataMongoActor(
  // TODO: need to "encapsulate" with a type like the JDBC classes
  val databaseFactory: EventSourcingDatabaseFactory,
  val mapper: TypedObjectMapper,
  context: ActorContext<Task<*>> = ActorContext()
): FunctionActor(context) {
  companion object {
    // TODO: is this "standard" naming for Mongo? Should we have "namespaces"?
    const val eventsCollectionName = "AggregateEvents"
    const val snapshotsCollectionName = "AggregateSnapshots"

  }

  suspend fun loadAggregateEvents(aggregateId: AggregateId, aggregateVersion: Long) = actAndReply {
    val collection = databaseFactory.database.getCollectionOfName<Document>(eventsCollectionName)
    val events: FindIterable<Document> = collection.find()
//    val events = collection.find("{ aggregateId: '$aggregateId', aggregateVersion: { gte: $aggregateVersion } }")
//    for (event in events) {
//
//    }
  }

  /**
  Will store as a single document (see /docs/MongoDBEventStorage.md).

  @param commandId The Command that caused the creation of these events.
  @param events Can not be empty
   */
  suspend fun storeAggregateEvents(
    aggregateId: AggregateId,
    aggregateClass: KClass<*>,
    commandId: CommandId?,
    events: List<AggregateEvent>,
    authenticatedUserId: String? = null
    ): Either<FailureReply, SuccessReply> = actAndReply {

    val aggregateVersion: Long = events.first().aggregateVersion
    val createdAt = Instant.now()

    val wrapper = EventWrapper(aggregateId, aggregateClass.qualifiedName!!, aggregateVersion,
      createdAt, authenticatedUserId, commandId, events)
    val serializedWrapper = mapper.writeValueAsString(wrapper)

    val collection = databaseFactory.database.getCollectionOfName<EventWrapper>(eventsCollectionName)
    collection.insertOne(serializedWrapper)

    ErrorReply("Unimplemented", this::class).createLeft()
  }

}
