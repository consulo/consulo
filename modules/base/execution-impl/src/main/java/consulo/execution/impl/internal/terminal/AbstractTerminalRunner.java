package consulo.execution.impl.internal.terminal;

import com.jediterm.terminal.TtyConnector;
import consulo.execution.terminal.TerminalSession;

import java.util.concurrent.ExecutionException;

/**
 * @author traff
 */
public abstract class AbstractTerminalRunner<T extends Process> implements TerminalSession {
    protected abstract T createProcess(String directory) throws ExecutionException;

    protected abstract TtyConnector createTtyConnector(T process);

    public abstract String getWorkingDirectory();

    @Override
    public TtyConnector connect() throws ExecutionException {
        return createTtyConnector(createProcess(getWorkingDirectory()));
    }
}
