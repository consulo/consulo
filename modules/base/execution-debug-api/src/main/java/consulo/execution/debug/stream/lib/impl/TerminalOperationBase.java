// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.lib.impl;

import consulo.execution.debug.stream.lib.TerminalOperation;
import consulo.execution.debug.stream.resolve.ValuesOrderResolver;
import consulo.execution.debug.stream.trace.CallTraceInterpreter;
import consulo.execution.debug.stream.trace.TerminatorCallHandler;
import consulo.execution.debug.stream.trace.dsl.Dsl;
import consulo.execution.debug.stream.wrapper.TerminatorStreamCall;


/**
 * @author Vitaliy.Bibaev
 */
public abstract class TerminalOperationBase implements TerminalOperation {
  private final String name;
  private final HandlerFactory handlerFactory;
  private final CallTraceInterpreter traceInterpreter;
  private final ValuesOrderResolver valuesOrderResolver;

  protected TerminalOperationBase(String name,
                                  HandlerFactory handlerFactory,
                                  CallTraceInterpreter traceInterpreter,
                                  ValuesOrderResolver valuesOrderResolver) {
    this.name = name;
    this.handlerFactory = handlerFactory;
    this.traceInterpreter = traceInterpreter;
    this.valuesOrderResolver = valuesOrderResolver;
  }

  
  @Override
  public String getName() {
    return name;
  }

  
  @Override
  public CallTraceInterpreter getTraceInterpreter() {
    return traceInterpreter;
  }

  
  @Override
  public ValuesOrderResolver getValuesOrderResolver() {
    return valuesOrderResolver;
  }

  
  @Override
  public TerminatorCallHandler getTraceHandler(TerminatorStreamCall call, String resultExpression, Dsl dsl) {
    return handlerFactory.create(call, resultExpression, dsl);
  }

  @FunctionalInterface
  protected interface HandlerFactory {
    
    TerminatorCallHandler create(TerminatorStreamCall call, String resultExpression, Dsl dsl);
  }
}
