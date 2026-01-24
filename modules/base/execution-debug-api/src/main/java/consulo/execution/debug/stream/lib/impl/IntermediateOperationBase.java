// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.lib.impl;

import consulo.execution.debug.stream.lib.IntermediateOperation;
import consulo.execution.debug.stream.resolve.ValuesOrderResolver;
import consulo.execution.debug.stream.trace.CallTraceInterpreter;
import consulo.execution.debug.stream.trace.IntermediateCallHandler;
import consulo.execution.debug.stream.trace.dsl.Dsl;
import consulo.execution.debug.stream.wrapper.IntermediateStreamCall;


import jakarta.annotation.Nonnull;

/**
 * @author Vitaliy.Bibaev
 */
public abstract class IntermediateOperationBase implements IntermediateOperation {
  private final String name;
  private final IntermediateCallHandlerFactory handlerFactory;
  private final CallTraceInterpreter traceInterpreter;
  private final ValuesOrderResolver valuesOrderResolver;

  protected IntermediateOperationBase(@Nonnull String name,
                                      @Nonnull IntermediateCallHandlerFactory handlerFactory,
                                      @Nonnull CallTraceInterpreter traceInterpreter,
                                      @Nonnull ValuesOrderResolver valuesOrderResolver) {
    this.name = name;
    this.handlerFactory = handlerFactory;
    this.traceInterpreter = traceInterpreter;
    this.valuesOrderResolver = valuesOrderResolver;
  }

  @Nonnull
  @Override
  public String getName() {
    return name;
  }

  @Nonnull
  @Override
  public CallTraceInterpreter getTraceInterpreter() {
    return traceInterpreter;
  }

  @Nonnull
  @Override
  public ValuesOrderResolver getValuesOrderResolver() {
    return valuesOrderResolver;
  }

  @Nonnull
  @Override
  public IntermediateCallHandler getTraceHandler(int callOrder, @Nonnull IntermediateStreamCall call, @Nonnull Dsl dsl) {
    return handlerFactory.create(callOrder, call, dsl);
  }

}
