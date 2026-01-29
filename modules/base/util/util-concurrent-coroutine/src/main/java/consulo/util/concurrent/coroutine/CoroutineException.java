//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
// This file is a part of the 'coroutines' project.
// Copyright 2018 Elmar Sonnenschein, esoco GmbH, Flensburg, Germany
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//	  http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
package consulo.util.concurrent.coroutine;

import java.util.concurrent.CompletionException;

/**
 * The base class of unchecked exceptions that may be thrown by
 * {@link Coroutine} executions.
 *
 * @author eso
 */
public class CoroutineException extends CompletionException {

	private static final long serialVersionUID = 1L;

	/**
	 * Creates a new instance.
	 *
	 * @param cause The causing exception
	 */
	public CoroutineException(Throwable cause) {
		super(cause);
	}

	/**
	 * Creates a new instance with a formatted message.
	 *
	 * @param messageFormat The error message format string
	 * @param args          The format arguments
	 */
	public CoroutineException(String messageFormat, Object... args) {
		super(String.format(messageFormat, args));
	}

	/**
	 * Creates a new instance.
	 *
	 * @param message The error message
	 * @param cause   The causing exception
	 */
	public CoroutineException(String message, Throwable cause) {
		super(message, cause);
	}
}
