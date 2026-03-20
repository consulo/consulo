package consulo.execution.debug.stream.trace;

public interface DebuggerCommandLauncher {
  void launchDebuggerCommand(Runnable command);
}
