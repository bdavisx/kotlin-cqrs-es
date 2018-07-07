From the following SO question (https://stackoverflow.com/questions/43408599/choosing-a-nosql-database-for-storing-events-in-a-cqrs-designed-application/43409269#43409269)

Basically the gist is that you need to store the events for a single commit as a single document since mongo doesn't do multi-document transactions. 

***

I have a working, in production implementation of MongoDB as an Event store. It is used by a CQRS + Event sourcing web based CRM application.

In order to provide 100% transaction-less but transaction-like guarantee for persisting multiple events in one go (all events or none of them) I use a MongoDB document as an events commit, with events as nested documents. As you know, MongoDB has document level locking.

For concurrency I use optimistic locking, using a version property for each Aggregate steam. An Aggregate stream is identified by the dublet (Aggregate class x Aggregate ID).

The event store also stores the commits in relative order using a sequence on each commit, incremented on each commit, protected using optimistic locking.

Each commit contains the following:

    * aggregateId : string, probably a GUID,
    * aggregateClass: string,
    * version: integer, incremented for each aggregateId x aggregateClass,
    * sequence, integer, incremented for each commit,
    * createdAt: UTCDateTime,
    * authenticatedUserId: string or null,
    * events: list of EventWithMetadata,

Each EventWithMetadata contains the event class/type and the payload as string (the serialized version of the actual event).

The MongoDB collection has the following indexes:

* aggregateId, aggregateClass, version as unique
* events.eventClass, sequence
* sequence
* other indexes for query optimization

These indexes are used to enforce the general event store rules (no events are stored for the same version of an Aggregate) and for query optimizations (the client can select only certain events - by type - from all streams).

You could use sharding by aggregateId to scale, if you strip the global ordering of events (the sequence property) and you move that responsibility to an event publisher but this complicates things as the event publisher needs to stay synchronized (even in case of failure!) with the event store. I recommend to do it only if you need it.

Benchmarks for this implementation (on Intel I7 with 8GB of RAM):

* total aggregate write time was: 7.99, speed: 12516 events wrote per second
* total aggregate read time was: 1.43, speed: 35036 events read per second
* total read-model read time was: 3.26, speed: 30679 events read per second

I've noticed that MongoDB was slow on counting the number of events in the event store. I don't know why but I don't care as I don't need this feature.

I recommend using MongoDB as an event store.

***
