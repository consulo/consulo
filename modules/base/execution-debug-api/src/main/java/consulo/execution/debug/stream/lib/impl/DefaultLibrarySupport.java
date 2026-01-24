// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.lib.impl;

import consulo.execution.debug.stream.lib.HandlerFactory;
import consulo.execution.debug.stream.lib.InterpreterFactory;
import consulo.execution.debug.stream.lib.LibrarySupport;
import consulo.execution.debug.stream.lib.ResolverFactory;
import consulo.execution.debug.stream.resolve.EmptyResolver;
import consulo.execution.debug.stream.resolve.ValuesOrderResolver;
import consulo.execution.debug.stream.trace.CallTraceInterpreter;
import consulo.execution.debug.stream.trace.IntermediateCallHandler;
import consulo.execution.debug.stream.trace.TerminatorCallHandler;
import consulo.execution.debug.stream.trace.dsl.Dsl;
import consulo.execution.debug.stream.trace.impl.handler.unified.PeekTraceHandler;
import consulo.execution.debug.stream.trace.impl.handler.unified.TerminatorTraceHandler;
import consulo.execution.debug.stream.trace.impl.interpret.SimplePeekCallTraceInterpreter;
import consulo.execution.debug.stream.wrapper.IntermediateStreamCall;
import consulo.execution.debug.stream.wrapper.StreamCallType;
import consulo.execution.debug.stream.wrapper.TerminatorStreamCall;

import jakarta.annotation.Nonnull;

public class DefaultLibrarySupport implements LibrarySupport {
  @Nonnull
  @Override
  public HandlerFactory createHandlerFactory(@Nonnull Dsl dsl) {
    return new HandlerFactory() {
      @Nonnull
      @Override
      public IntermediateCallHandler getForIntermediate(int number, @Nonnull IntermediateStreamCall call) {
        return new PeekTraceHandler(number, call.getName(), call.getTypeBefore(), call.getTypeAfter(), dsl);
      }

      @Nonnull
      @Override
      public TerminatorCallHandler getForTermination(@Nonnull TerminatorStreamCall call, @Nonnull String resultExpression) {
        return new TerminatorTraceHandler(call, dsl);
      }
    };
  }

  @Nonnull
  @Override
  public InterpreterFactory getInterpreterFactory() {
    return new InterpreterFactory() {
      @Nonnull
      @Override
      public CallTraceInterpreter getInterpreter(@Nonnull String callName, @Nonnull StreamCallType callType) {
        return new SimplePeekCallTraceInterpreter();
      }
    };
  }

  @Nonnull
  @Override
  public ResolverFactory getResolverFactory() {
    return new ResolverFactory() {
      @Nonnull
      @Override
      public ValuesOrderResolver getResolver(@Nonnull String callName, @Nonnull StreamCallType callType) {
        return new EmptyResolver();
      }
    };
  }
}
