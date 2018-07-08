package com.tartner.cqrs.eventsourcing

import java.util.*

typealias AggregateId = UUID
typealias EntityId = UUID

interface Entity {
  val entityId: EntityId
}
