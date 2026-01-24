// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.lib.impl;

import consulo.execution.debug.stream.resolve.ValuesOrderResolver;
import consulo.execution.debug.stream.trace.impl.handler.unified.PeekTraceHandler;
import consulo.execution.debug.stream.trace.impl.interpret.SimplePeekCallTraceInterpreter;
import jakarta.annotation.Nonnull;

/**
 * @author Vitaliy.Bibaev
 */
public class OrderBasedOperation extends IntermediateOperationBase {
  public OrderBasedOperation(@Nonnull String name, @Nonnull ValuesOrderResolver orderResolver) {
    super(name,
          (num, call, dsl) -> new PeekTraceHandler(num, call.getName(), call.getTypeBefore(), call.getTypeAfter(), dsl),
          new SimplePeekCallTraceInterpreter(),
          orderResolver);
  }
}

