package com.tartner.cqrs.eventsourcing

import com.mongodb.async.client.*
import kotlinx.coroutines.experimental.*
import org.bson.*
import org.bson.types.*
import org.junit.*
import org.junit.Test
import org.litote.kmongo.coroutine.*
import java.time.*
import kotlin.reflect.*
import kotlin.test.*

internal class EventSourcedAggregateDataMongoActorTest: KMongoCoroutineBaseTest<Friend>() {
  @Test
  fun canRunACommand() = runBlocking {
    val document = database.runCommand<Document>("{ ping: 1 }") ?: throw AssertionError("Document must not null!")
    assertEquals(1.0, document["ok"])
  }
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
  open fun getDefaultCollectionClass(): KClass<T>
    = Friend::class as KClass<T>

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
