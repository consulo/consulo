// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl.impl;

import consulo.execution.debug.stream.trace.dsl.Expression;
import consulo.execution.debug.stream.trace.dsl.Statement;
import consulo.execution.debug.stream.trace.dsl.Variable;
import jakarta.annotation.Nonnull;

/**
 * @author Vitaliy.Bibaev
 */
public interface AssignmentStatement extends Statement {
  @Nonnull
  Variable getVariable();

  @Nonnull
  Expression getExpression();
}
