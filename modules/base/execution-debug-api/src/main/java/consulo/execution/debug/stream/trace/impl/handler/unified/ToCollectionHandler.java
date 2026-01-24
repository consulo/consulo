// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.impl.handler.unified;

import consulo.execution.debug.stream.trace.dsl.Dsl;
import consulo.execution.debug.stream.trace.dsl.Expression;
import consulo.execution.debug.stream.wrapper.TerminatorStreamCall;

/**
 * @author Vitaliy.Bibaev
 */
public class ToCollectionHandler extends TerminatorTraceHandler {
  public ToCollectionHandler(TerminatorStreamCall call, Dsl dsl) {
    super(call, dsl);
  }

  @Override
  public Expression getResultExpression() {
    return dsl.newArray(dsl.getTypes().ANY(), super.getResultExpression(), dsl.newArray(dsl.getTypes().INT(), dsl.currentTime()));
  }
}
