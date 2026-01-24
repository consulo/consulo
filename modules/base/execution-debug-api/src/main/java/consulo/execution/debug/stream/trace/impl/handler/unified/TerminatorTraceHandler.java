// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.impl.handler.unified;

import consulo.execution.debug.stream.trace.dsl.CodeBlock;
import consulo.execution.debug.stream.trace.dsl.Dsl;
import consulo.execution.debug.stream.trace.dsl.Expression;
import consulo.execution.debug.stream.trace.dsl.VariableDeclaration;
import consulo.execution.debug.stream.wrapper.IntermediateStreamCall;
import consulo.execution.debug.stream.wrapper.TerminatorStreamCall;

import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public class TerminatorTraceHandler extends HandlerBase.Terminal {
  private final PeekTraceHandler myPeekHandler;

  public TerminatorTraceHandler(TerminatorStreamCall call, Dsl dsl) {
    super(dsl);
    this.myPeekHandler = new PeekTraceHandler(Integer.MAX_VALUE, call.getName(), call.getTypeBefore(), dsl.getTypes().ANY(), dsl);
  }

  @Override
  public List<VariableDeclaration> additionalVariablesDeclaration() {
    return myPeekHandler.additionalVariablesDeclaration();
  }

  @Override
  public CodeBlock prepareResult() {
    return myPeekHandler.prepareResult();
  }

  @Override
  public Expression getResultExpression() {
    return myPeekHandler.getResultExpression();
  }

  @Override
  public List<IntermediateStreamCall> additionalCallsBefore() {
    return myPeekHandler.additionalCallsBefore();
  }
}
