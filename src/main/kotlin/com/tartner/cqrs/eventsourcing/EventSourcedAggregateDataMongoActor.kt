package com.tartner.cqrs.eventsourcing

import arrow.core.*
import com.tartner.cqrs.actors.*
import com.tartner.cqrs.commands.*

class EventSourcedAggregateDataMongoActor(context: ActorContext<Task<*>>): FunctionActor(context) {

  /** Will store as a single document (see /docs/MongoDBEventStorage.md). */
  fun storeAggregateEvents(aggregateId: AggregateId, events: List<AggregateEvent>)/* :
    Either<FailureReply, SuccessReply> */ {


  }

}
