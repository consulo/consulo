package consulo.desktop.awt.execution.terminal;

import com.jediterm.terminal.*;
import com.jediterm.terminal.emulator.JediEmulator;
import consulo.ide.impl.idea.ide.GeneralSettings;
import consulo.virtualFileSystem.LocalFileSystem;

/**
 * @author traff
 */
public class JBTerminalStarter extends TerminalStarter {
  public JBTerminalStarter(Terminal terminal, TtyConnector ttyConnector) {
    super(terminal, ttyConnector, new TtyBasedArrayDataStream(ttyConnector));
  }

  @Override
  protected JediEmulator createEmulator(TerminalDataStream dataStream, Terminal terminal) {
    return new JediEmulator(dataStream, terminal) {
      @Override
      protected void unsupported(char... sequenceChars) {
        if (sequenceChars[0] == 7) { //ESC BEL
          refreshAfterExecution();
        }
        else {
          super.unsupported();
        }
      }
    };
  }

  public static void refreshAfterExecution() {
    if (GeneralSettings.getInstance().isSyncOnFrameActivation()) {
      //we need to refresh local file system after a command has been executed in the terminal
      LocalFileSystem.getInstance().refresh(true);
    }
  }
}
