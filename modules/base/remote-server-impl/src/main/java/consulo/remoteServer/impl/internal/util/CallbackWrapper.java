package consulo.remoteServer.impl.internal.util;

/**
 * @author michael.golubev
 */
public interface CallbackWrapper<T> {

    void onSuccess(T result);

    void onError(String message);
}
