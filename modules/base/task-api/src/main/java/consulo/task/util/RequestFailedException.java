package consulo.task.util;

import consulo.task.localize.TaskLocalize;
import jakarta.annotation.Nonnull;

import consulo.task.TaskBundle;

/**
 * @author Mikhail Golubev
 */
public class RequestFailedException extends RuntimeException
{
	@Nonnull
	public static RequestFailedException forStatusCode(int code)
	{
		return new RequestFailedException(TaskBundle.messageForStatusCode(code));
	}

	@Nonnull
	public static RequestFailedException forStatusCode(int code, @Nonnull String message)
	{
		return new RequestFailedException(TaskLocalize.failureHttpError(code, message).get());
	}

	@Nonnull
	public static RequestFailedException forServerMessage(@Nonnull String message)
	{
		return new RequestFailedException(TaskLocalize.failureServerMessage(message).get());
	}

	public RequestFailedException(String message)
	{
		super(message);
	}

	public RequestFailedException(String message, Throwable cause)
	{
		super(message, cause);
	}

	public RequestFailedException(Throwable cause)
	{
		super(cause);
	}
}
