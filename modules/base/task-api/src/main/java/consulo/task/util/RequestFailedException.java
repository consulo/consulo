package consulo.task.util;

import consulo.localize.LocalizeValue;
import consulo.task.localize.TaskLocalize;

import org.apache.http.HttpStatus;

/**
 * @author Mikhail Golubev
 */
public class RequestFailedException extends RuntimeException {
    public static RequestFailedException forStatusCode(int code) {
        return new RequestFailedException(messageForStatusCode(code));
    }

    public static RequestFailedException forStatusCode(int code, String message) {
        return new RequestFailedException(TaskLocalize.failureHttpError(code, message));
    }

    public static RequestFailedException forServerMessage(String message) {
        return new RequestFailedException(TaskLocalize.failureServerMessage(message));
    }

    public RequestFailedException(LocalizeValue message) {
        super(message.get());
    }

    public RequestFailedException(String message) {
        super(message);
    }

    public RequestFailedException(String message, Throwable cause) {
        super(message, cause);
    }

    public RequestFailedException(Throwable cause) {
        super(cause);
    }

    private static LocalizeValue messageForStatusCode(int statusCode) {
        if (statusCode == HttpStatus.SC_UNAUTHORIZED) {
            return TaskLocalize.failureLogin();
        }
        else if (statusCode == HttpStatus.SC_FORBIDDEN) {
            return TaskLocalize.failurePermissions();
        }
        return TaskLocalize.failureHttpError(statusCode, "");
    }
}
