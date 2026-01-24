// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace;

import consulo.execution.debug.stream.trace.dsl.CodeBlock;
import consulo.execution.debug.stream.trace.dsl.Expression;
import consulo.execution.debug.stream.trace.dsl.VariableDeclaration;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public interface TraceHandler {
  @Nonnull
  List<VariableDeclaration> additionalVariablesDeclaration();

  @Nonnull
  CodeBlock prepareResult();

  @Nonnull
  Expression getResultExpression();
}
