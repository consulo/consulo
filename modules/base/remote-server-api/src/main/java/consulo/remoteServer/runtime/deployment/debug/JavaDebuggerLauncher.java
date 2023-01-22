package consulo.remoteServer.runtime.deployment.debug;

import consulo.application.Application;

/**
 * @author nik
 */
@Deprecated
public abstract class JavaDebuggerLauncher implements DebuggerLauncher<JavaDebugConnectionData> {
  public static JavaDebuggerLauncher getInstance() {
    return Application.get().getInstance(JavaDebuggerLauncher.class);
  }
}
