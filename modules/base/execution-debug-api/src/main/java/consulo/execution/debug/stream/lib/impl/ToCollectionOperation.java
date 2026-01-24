// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.lib.impl;

import consulo.execution.debug.stream.resolve.IdentityResolver;
import consulo.execution.debug.stream.trace.impl.handler.unified.ToCollectionHandler;
import consulo.execution.debug.stream.trace.impl.interpret.CollectIdentityTraceInterpreter;

import jakarta.annotation.Nonnull;

/**
 * @author Vitaliy.Bibaev
 */
public class ToCollectionOperation extends TerminalOperationBase {
  public ToCollectionOperation(@Nonnull String name) {
    super(name,
          (call, resultExpression, dsl) -> new ToCollectionHandler(call, dsl),
          new CollectIdentityTraceInterpreter(),
          new IdentityResolver());
  }
}
