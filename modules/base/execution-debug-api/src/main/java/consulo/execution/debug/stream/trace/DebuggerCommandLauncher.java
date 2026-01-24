package consulo.execution.debug.stream.trace;

import jakarta.annotation.Nonnull;

public interface DebuggerCommandLauncher {
  void launchDebuggerCommand(@Nonnull Runnable command);
}
