package consulo.remoteServer.impl.internal.runtime;

import consulo.application.util.concurrent.SequentialTaskExecutor;
import consulo.logging.Logger;
import consulo.remoteServer.runtime.RemoteOperationCallback;
import consulo.remoteServer.runtime.ServerTaskExecutor;
import consulo.util.lang.function.ThrowableRunnable;
import jakarta.annotation.Nonnull;

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
    public void execute(@Nonnull Runnable command) {
        myTaskExecutor.execute(command);
    }

    @Override
    public void submit(@Nonnull Runnable command) {
        execute(command);
    }

    @Override
    public void submit(@Nonnull ThrowableRunnable<?> command, @Nonnull RemoteOperationCallback callback) {
        execute(() -> {
            try {
                command.run();
            }
            catch (Throwable e) {
                LOG.info(e);
                callback.errorOccurred(e.getMessage());
            }
        });
    }
}
