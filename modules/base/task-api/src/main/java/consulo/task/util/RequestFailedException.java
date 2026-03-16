package consulo.task.util;

import consulo.task.localize.TaskLocalize;

import consulo.task.TaskBundle;

/**
 * @author Mikhail Golubev
 */
public class RequestFailedException extends RuntimeException
{
	
	public static RequestFailedException forStatusCode(int code)
	{
		return new RequestFailedException(TaskBundle.messageForStatusCode(code));
	}

	
	public static RequestFailedException forStatusCode(int code, String message)
	{
		return new RequestFailedException(TaskLocalize.failureHttpError(code, message).get());
	}

	
	public static RequestFailedException forServerMessage(String message)
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
