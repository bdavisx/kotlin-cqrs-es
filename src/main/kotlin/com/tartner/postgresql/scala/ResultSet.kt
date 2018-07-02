package com.tartner.postgresql.scala

/**
 Represents the collection of rows that is returned from a statement inside a {@link QueryResult}.
 It's basically a collection of Array[Any]. Mutating fields in this array will not affect the
 database in any way.
 */
interface ResultSet: List<RowData> {

  /** The names of the columns returned by the statement. */
  val columnNames : List<String>

}
