package consulo.desktop.awt.execution.terminal;

import com.jediterm.terminal.TtyConnector;
import com.jediterm.terminal.ui.TerminalSession;
import com.jediterm.terminal.ui.TerminalWidget;
import consulo.localize.LocalizeValue;
import consulo.ui.Alerts;

import java.util.concurrent.ExecutionException;

/**
 * @author traff
 */
public abstract class AbstractTerminalRunner<T extends Process> implements consulo.execution.terminal.TerminalSession {
  protected abstract T createProcess(String directory) throws ExecutionException;

  protected abstract TtyConnector createTtyConnector(T process);

  public abstract String getWorkingDirectory();

  public void openSessionInDirectory(TerminalWidget terminal) {
    // Create Server process
    try {
      T process = createProcess(getWorkingDirectory());

      TerminalSession session = terminal.createTerminalSession(createTtyConnector(process));

      session.start();
    }
    catch (Exception e) {
      Alerts.okError(LocalizeValue.of(e.getLocalizedMessage())).showAsync();
    }
  }
}
