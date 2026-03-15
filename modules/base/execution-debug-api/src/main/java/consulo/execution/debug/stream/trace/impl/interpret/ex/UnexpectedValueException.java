// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.impl.interpret.ex;


/**
 * @author Vitaliy.Bibaev
 */
public final class UnexpectedValueException extends ResolveException {
  public UnexpectedValueException(String s) {
    super(s);
  }

  public UnexpectedValueException(String message, Throwable cause) {
    super(message, cause);
  }
}
