package consulo.remoteServer.impl.internal.runtime;

import consulo.application.util.concurrent.SequentialTaskExecutor;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.remoteServer.runtime.RemoteOperationCallback;
import consulo.remoteServer.runtime.ServerTaskExecutor;
import consulo.util.lang.function.ThrowableRunnable;

import java.util.concurrent.ExecutorService;

/**
 * @author nik
 */
public class ServerTaskExecutorImpl implements ServerTaskExecutor {
    private static final Logger LOG = Logger.getInstance(ServerTaskExecutorImpl.class);
    private final ExecutorService myTaskExecutor;

    public ServerTaskExecutorImpl() {
        myTaskExecutor = SequentialTaskExecutor.createSequentialApplicationPoolExecutor("ServerTaskExecutorImpl pool");
    }

    @Override
    public void execute(Runnable command) {
        myTaskExecutor.execute(command);
    }

    @Override
    public void submit(Runnable command) {
        execute(command);
    }

    @Override
    public void submit(ThrowableRunnable<?> command, RemoteOperationCallback callback) {
        execute(() -> {
            try {
                command.run();
            }
            catch (Throwable e) {
                LOG.info(e);
                callback.errorOccurred(LocalizeValue.ofNullable(e.getMessage()));
            }
        });
    }
}
