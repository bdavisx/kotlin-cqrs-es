package com.tartner.postgresql.scala

/** Represents a row from a database, allows clients to access rows by column number or column name. */
interface RowData: List<Any> {
  /** Number of this row in the query results. Counts start at 0. */
  val rowNumber: Int

  /** Returns a column value by it's position in the originating query. */
  override operator fun get(columnNumber: Int): Any

  /** Returns a column value by it's name in the originating query. */
  operator fun get( columnName: String): Any
}
