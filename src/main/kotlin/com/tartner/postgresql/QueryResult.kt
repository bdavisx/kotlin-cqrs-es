package com.tartner.postgresql

/**
 *
 * This is the result of the execution of a statement, contains basic information as the number or rows
 * affected by the statement and the rows returned if there were any.
 *
 * @param rowsAffected
 * @param statusMessage
 * @param rows
 */

class QueryResult(val rowsAffected: Long, val statusMessage: String, val rows: ResultSet? = null) {
  override fun toString(): String = "QueryResult{rows -> $rowsAffected,status -> $statusMessage}"
}
