package com.tartner.cqrs.eventsourcing

import com.mongodb.async.client.*
import org.litote.kmongo.async.*

interface MongoDatabaseFactory {
  val database: MongoDatabase
}

interface EventSourcingDatabaseFactory: MongoDatabaseFactory

class EventSourcingMongoDatabaseFactory: EventSourcingDatabaseFactory {
  override val database: MongoDatabase

  init {
    val client = KMongo.createClient() //get com.mongodb.MongoClient new instance
    database = client.getDatabase("EventSourcing") //normal java driver usage
  }
}
