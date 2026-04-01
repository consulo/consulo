package consulo.desktop.awt.execution.terminal;

import com.jediterm.core.util.TermSize;
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
    public void resize(TermSize termSize) {
        if (isConnected()) {
            myProcess.setWinSize(new WinSize(termSize.getColumns(), termSize.getRows()));
        }
    }

    @Override
    public boolean isConnected() {
        return myProcess.isAlive();
    }

    @Override
    public String getName() {
        return myConnectorName;
    }
}
