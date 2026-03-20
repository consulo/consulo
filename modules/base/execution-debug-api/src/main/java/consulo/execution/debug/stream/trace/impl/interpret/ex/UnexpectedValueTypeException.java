// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.impl.interpret.ex;

/**
 * @author Vitaliy.Bibaev
 */
public class UnexpectedValueTypeException extends ResolveException {
  public UnexpectedValueTypeException(String message) {
    super(message);
  }
}
