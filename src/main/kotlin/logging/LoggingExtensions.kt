package logging

import org.slf4j.*

inline fun Logger.debugIf(messageFactory: () -> String) {
  if (isDebugEnabled) {
    debug(messageFactory.invoke())
  }
}

inline fun Logger.infoIf(messageFactory: () -> String) {
  if (isInfoEnabled) {
    info(messageFactory.invoke())
  }
}
