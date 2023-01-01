package consulo.index.io;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class IOStatistics {
  static final boolean DEBUG = System.getProperty("io.access.debug") != null;
  static final int MIN_IO_TIME_TO_REPORT = 100;
  static final Logger LOG = LoggerFactory.getLogger(IOStatistics.class);
  static final int KEYS_FACTOR_MASK = 0xFFFF;

  static void dump(String msg) {
    LOG.info(msg);
  }
}
