package consulo.ide.impl.idea.tasks.impl;

import javax.annotation.Nonnull;

import consulo.ide.impl.idea.tasks.TaskBundle;

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
		return new RequestFailedException(TaskBundle.message("failure.http.error", code, message));
	}

	@Nonnull
	public static RequestFailedException forServerMessage(@Nonnull String message)
	{
		return new RequestFailedException(TaskBundle.message("failure.server.message", message));
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
