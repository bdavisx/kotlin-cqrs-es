package com.tartner.postgresql.column

import java.nio.*
import java.nio.charset.*

interface ColumnEncoder {
  fun encode(value: Any): String = value.toString()
}

interface ColumnDecoder {
//  fun decode(kind: ColumnData, value: ByteBuffer, charset : Charset) : Any = {
//    val bytes = value.get() new Array[Byte](value.readableBytes())
//    value.readBytes(bytes)
//    decode(new String(bytes, charset))
//  }
//
//  def decode( value : String ) : Any
//
//  def supportsStringDecoding : Boolean = true

}


interface ColumnEncoderDecoder {}
