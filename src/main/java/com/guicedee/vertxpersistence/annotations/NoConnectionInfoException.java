package com.guicedee.vertxpersistence.annotations;

/**
 * Signals that required connection information is missing.
 */
public class NoConnectionInfoException
		extends RuntimeException
{
	/**
	 * Creates a new exception without a detail message.
	 */
	public NoConnectionInfoException()
	{
		//Nothing needed
	}

	/**
	 * Creates a new exception with a detail message.
	 *
	 * @param message the detail message
	 */
	public NoConnectionInfoException(String message)
	{
		super(message);
	}

	/**
	 * Creates a new exception with a detail message and cause.
	 *
	 * @param message the detail message
	 * @param cause the underlying cause
	 */
	public NoConnectionInfoException(String message, Throwable cause)
	{
		super(message, cause);
	}

	/**
	 * Creates a new exception with a cause.
	 *
	 * @param cause the underlying cause
	 */
	public NoConnectionInfoException(Throwable cause)
	{
		super(cause);
	}

	/**
	 * Creates a new exception with advanced suppression and stack trace options.
	 *
	 * @param message the detail message
	 * @param cause the underlying cause
	 * @param enableSuppression whether suppression is enabled
	 * @param writableStackTrace whether the stack trace should be writable
	 */
	public NoConnectionInfoException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace)
	{
		super(message, cause, enableSuppression, writableStackTrace);
	}
}
