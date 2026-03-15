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


public class DefaultLibrarySupport implements LibrarySupport {
  
  @Override
  public HandlerFactory createHandlerFactory(Dsl dsl) {
    return new HandlerFactory() {
      
      @Override
      public IntermediateCallHandler getForIntermediate(int number, IntermediateStreamCall call) {
        return new PeekTraceHandler(number, call.getName(), call.getTypeBefore(), call.getTypeAfter(), dsl);
      }

      
      @Override
      public TerminatorCallHandler getForTermination(TerminatorStreamCall call, String resultExpression) {
        return new TerminatorTraceHandler(call, dsl);
      }
    };
  }

  
  @Override
  public InterpreterFactory getInterpreterFactory() {
    return new InterpreterFactory() {
      
      @Override
      public CallTraceInterpreter getInterpreter(String callName, StreamCallType callType) {
        return new SimplePeekCallTraceInterpreter();
      }
    };
  }

  
  @Override
  public ResolverFactory getResolverFactory() {
    return new ResolverFactory() {
      
      @Override
      public ValuesOrderResolver getResolver(String callName, StreamCallType callType) {
        return new EmptyResolver();
      }
    };
  }
}
