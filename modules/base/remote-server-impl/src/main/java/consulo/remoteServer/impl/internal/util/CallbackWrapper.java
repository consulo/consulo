package consulo.remoteServer.impl.internal.util;

import jakarta.annotation.Nonnull;

/**
 * @author michael.golubev
 */
public interface CallbackWrapper<T> {

    void onSuccess(T result);

    void onError(@Nonnull String message);
}
