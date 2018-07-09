package com.tartner.cqrs.eventsourcing

import com.mongodb.async.client.*
import com.natpryce.hamkrest.*
import com.tartner.cqrs.commands.*
import com.tartner.utilities.jackson.*
import io.kotlintest.*
import kotlinx.coroutines.experimental.*
import org.bson.*
import org.bson.types.*
import org.junit.*
import org.litote.kmongo.coroutine.*
import org.slf4j.*
import java.time.*
import java.util.*
import kotlin.reflect.*

data class TestCreateEvent(override val aggregateId: UUID, override val aggregateVersion: Long): AggregateEvent
data class TestCreateSnapshot(override val aggregateId: UUID, override val aggregateVersion: Long): AggregateSnapshot

internal class EventSourcedAggregateDataMongoActorTest() {
  private val log = LoggerFactory.getLogger(EventSourcedAggregateDataMongoActorTest::class.java)

  val aggregateId = UUID.fromString("c562d873-7d21-4191-ac4c-fdc762f2eed4")

  @Test
  fun canStoreAndReadEvents() { runBlocking {
    val database = EventSourcingMongoDatabaseFactory()
    val actor = EventSourcedAggregateDataMongoActor(database, TypedObjectMapper())

    val commandId = UUID.randomUUID()
    val aggregateVersion: Long = 1

    val events = listOf(TestCreateEvent(aggregateId, aggregateVersion))

    actor.storeAggregateEvents(aggregateId, EventSourcedAggregateDataMongoActor::class, commandId,
      events)

    val eventsIterator = actor.loadAggregateEvents(aggregateId, 0)
    val loadedEvents = eventsIterator.toList()
    log.debug("loadedEvents = $loadedEvents")
    val document: Document = loadedEvents[0]
    log.debug("$document")
    loadedEvents.size shouldBe greaterThan(0)
  } }
}

open class KMongoCoroutineBaseTest<T : Any> {

  @Suppress("LeakingThis")
  @Rule
  @JvmField
  val rule = CoroutineFlapdoodleRule(getDefaultCollectionClass())

  val col by lazy { rule.col }

  val database by lazy { rule.database }

  inline fun <reified T : Any> getCollection(): MongoCollection<T> = rule.getCollection<T>()

  @Suppress("UNCHECKED_CAST")
  open fun getDefaultCollectionClass(): KClass<T> = Friend::class as KClass<T>
}

data class Coordinate( val lat: Int, val lng : Int)

data class Friend(
  var name: String?,
  val address: String?,
  val _id: ObjectId? = null,
  val coordinate: Coordinate? = null,
  val tags: List<String> = emptyList(),
  val creationDate: Instant? = null
) {

  constructor(name: String) : this(name, null, null)

  constructor(name: String?, coordinate: Coordinate) : this(name, null, null, coordinate)

  constructor(_id: ObjectId, name: String) : this(name, null, _id)
}
