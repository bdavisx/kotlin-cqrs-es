package com.tartner.postgresql

import java.io.*

enum class Mode(val value: String) {
  Disable("disable"),      // only try a non-SSL connection
  Prefer("prefer"),       // first try an SSL connection; if that fails, try a non-SSL connection
  Require("require"),      // only try an SSL connection, but don't verify Certificate Authority
  VerifyCA("verify-ca"),    // only try an SSL connection, and verify that the server certificate is issued by a trusted certificate authority (CA)
  VerifyFull("verify-full")  // only try an SSL connection, verify that the server certificate is issued by a trusted CA and that the server host name matches that in the certificate
}

data class SSLConfiguration(val mode: Mode = Mode.Disable, val rootCert: File? = null)
