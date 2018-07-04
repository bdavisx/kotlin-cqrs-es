package com.tartner.postgresql

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException

/**
 * MD5-based utility function to obfuscate passwords before network transmission.
 *
 * @author Jeremy Wohl
 *
 * NOTE: Copied from postgresql driver
 */
object MD5Digest {

  /**
   * Encodes user/password/salt information in the following way: MD5(MD5(password + user) + salt)
   *
   * @param user The connecting user.
   * @param password The connecting user's password.
   * @param salt A four-salt sent by the server.
   * @return A 35-byte array, comprising the string "md5" and an MD5 digest.
   */
  fun encode(user: ByteArray, password: ByteArray, salt: ByteArray): ByteArray {
    val hexDigest = ByteArray(35)

    try {
      val md: MessageDigest = MessageDigest.getInstance("MD5")

      md.update(password)
      md.update(user)

      val tempDigest: ByteArray = md.digest()
      bytesToHex(tempDigest, hexDigest, 0)
      md.update(hexDigest, 0, 32)
      md.update(salt)

      val passwordDigest: ByteArray = md.digest()
      bytesToHex(passwordDigest, hexDigest, 3)
      hexDigest[0] = 'm'.toByte()
      hexDigest[1] = 'd'.toByte()
      hexDigest[2] = '5'.toByte()
    } catch (e: NoSuchAlgorithmException) {
      throw IllegalStateException("Unable to encode password with MD5", e)
    }

    return hexDigest
  }

  /*
   * Turn 16-byte stream into a human-readable 32-byte hex string
   */
  private fun bytesToHex(bytes: ByteArray, hex: ByteArray, offset: Int) {
    val lookup =
      charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f')

    var i: Int
    var c: Int
    var j: Int
    var pos = offset

    i = 0
    while (i < 16) {
      val currentByte = bytes[i]
      c = (currentByte.toInt() and 0xFF)
      j = c shr 4
      hex[pos++] = lookup[j].toByte()
      j = c and 0xF
      hex[pos++] = lookup[j].toByte()
      i++
    }
  }
}
