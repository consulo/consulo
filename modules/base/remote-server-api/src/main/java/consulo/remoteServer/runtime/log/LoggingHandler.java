package consulo.remoteServer.runtime.log;

import consulo.process.ProcessHandler;
import jakarta.annotation.Nonnull;

/**
 * @author nik
 */
public interface LoggingHandler {
  void print(@Nonnull String s);

  void attachToProcess(@Nonnull ProcessHandler handler);
}
