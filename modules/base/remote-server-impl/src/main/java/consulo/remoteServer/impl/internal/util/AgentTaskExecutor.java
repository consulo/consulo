package consulo.remoteServer.impl.internal.util;

import consulo.application.util.function.Computable;
import consulo.remoteServer.agent.shared.util.CloudAgentErrorHandler;
import consulo.util.lang.ObjectUtil;
import jakarta.annotation.Nonnull;

import java.util.concurrent.CancellationException;
import java.util.function.Supplier;

/**
 * @author michael.golubev
 */
public class AgentTaskExecutor implements CloudAgentErrorHandler {

    private String myErrorMessage;

    @Override
    public void onError(String message) {
        myErrorMessage = message;
    }

    private void clear() {
        myErrorMessage = null;
    }

    public <T> T execute(Supplier<T> task) throws ServerRuntimeException {
        clear();
        T result;
        try {
            result = task.get();
        }
        catch (CancellationException e) {
            throw new ServerRuntimeException(safeMessage(e));
        }
        if (myErrorMessage == null) {
            return result;
        }
        else {
            throw new ServerRuntimeException(myErrorMessage);
        }
    }

    public <T> void execute(Computable<? extends T> task, CallbackWrapper<T> callback) {
        clear();
        T result;
        try {
            result = task.compute();
        }
        catch (CancellationException e) {
            onError(safeMessage(e));
            result = null;
        }

        if (myErrorMessage == null) {
            callback.onSuccess(result);
        }
        else {
            callback.onError(myErrorMessage);
        }
    }

    private static String safeMessage(@Nonnull CancellationException ex) {
        return ObjectUtil.notNull(ex.getMessage(), "Operation cancelled");
    }
}
