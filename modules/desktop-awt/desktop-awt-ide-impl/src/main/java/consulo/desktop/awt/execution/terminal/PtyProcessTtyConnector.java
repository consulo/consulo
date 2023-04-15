package consulo.desktop.awt.execution.terminal;

import com.jediterm.terminal.ProcessTtyConnector;
import com.pty4j.PtyProcess;
import com.pty4j.WinSize;

import java.nio.charset.Charset;

/**
 * @author traff
 */
public class PtyProcessTtyConnector extends ProcessTtyConnector {
  private final PtyProcess myProcess;
  private final String myConnectorName;

  public PtyProcessTtyConnector(PtyProcess process, Charset charset, String connectorName) {
    super(process, charset);
    myProcess = process;
    myConnectorName = connectorName;
  }

  @Override
  protected void resizeImmediately() {
    if (getPendingTermSize() != null && getPendingPixelSize() != null) {
      myProcess.setWinSize(new WinSize(getPendingTermSize().width,
                                       getPendingTermSize().height,
                                       getPendingPixelSize().width,
                                       getPendingPixelSize().height));
    }
  }

  @Override
  public boolean isConnected() {
    return myProcess.isRunning();
  }

  @Override
  public String getName() {
    return myConnectorName;
  }
}
