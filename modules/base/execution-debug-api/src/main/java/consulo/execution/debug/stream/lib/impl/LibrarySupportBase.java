// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.lib.impl;

import consulo.execution.debug.stream.lib.*;
import consulo.execution.debug.stream.resolve.ValuesOrderResolver;
import consulo.execution.debug.stream.trace.CallTraceInterpreter;
import consulo.execution.debug.stream.trace.IntermediateCallHandler;
import consulo.execution.debug.stream.trace.TerminatorCallHandler;
import consulo.execution.debug.stream.trace.dsl.Dsl;
import consulo.execution.debug.stream.wrapper.IntermediateStreamCall;
import consulo.execution.debug.stream.wrapper.StreamCallType;
import consulo.execution.debug.stream.wrapper.TerminatorStreamCall;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Vitaliy.Bibaev
 */
public abstract class LibrarySupportBase implements LibrarySupport {
  public static final LibrarySupport EMPTY = new DefaultLibrarySupport();

  private final LibrarySupport compatibleLibrary;
  private final Map<String, IntermediateOperation> mySupportedIntermediateOperations = new HashMap<>();
  private final Map<String, TerminalOperation> mySupportedTerminalOperations = new HashMap<>();

  protected LibrarySupportBase() {
    this(EMPTY);
  }

  protected LibrarySupportBase(LibrarySupport compatibleLibrary) {
    this.compatibleLibrary = compatibleLibrary;
  }

  
  @Override
  public final HandlerFactory createHandlerFactory(Dsl dsl) {
    final HandlerFactory compatibleLibraryFactory = compatibleLibrary.createHandlerFactory(dsl);
    return new HandlerFactory() {
      
      @Override
      public IntermediateCallHandler getForIntermediate(int number, IntermediateStreamCall call) {
        IntermediateOperation operation = mySupportedIntermediateOperations.get(call.getName());
        if (operation != null) {
          return operation.getTraceHandler(number, call, dsl);
        }
        return compatibleLibraryFactory.getForIntermediate(number, call);
      }

      
      @Override
      public TerminatorCallHandler getForTermination(TerminatorStreamCall call, String resultExpression) {
        TerminalOperation terminalOperation = mySupportedTerminalOperations.get(call.getName());
        if (terminalOperation != null) {
          return terminalOperation.getTraceHandler(call, resultExpression, dsl);
        }
        return compatibleLibraryFactory.getForTermination(call, resultExpression);
      }
    };
  }

  
  @Override
  public final InterpreterFactory getInterpreterFactory() {
    return new InterpreterFactory() {
      
      @Override
      public CallTraceInterpreter getInterpreter(String callName, StreamCallType callType) {
        Operation operation = findOperation(callName, callType);
        if (operation != null) {
          return operation.getTraceInterpreter();
        }
        return compatibleLibrary.getInterpreterFactory().getInterpreter(callName, callType);
      }
    };
  }

  
  @Override
  public final ResolverFactory getResolverFactory() {
    return new ResolverFactory() {
      
      @Override
      public ValuesOrderResolver getResolver(String callName, StreamCallType callType) {
        Operation operation = findOperation(callName, callType);
        if (operation != null) {
          return operation.getValuesOrderResolver();
        }
        return compatibleLibrary.getResolverFactory().getResolver(callName, callType);
      }
    };
  }

  protected final void addIntermediateOperationsSupport(IntermediateOperation... operations) {
    for (IntermediateOperation operation : operations) {
      mySupportedIntermediateOperations.put(operation.getName(), operation);
    }
  }

  protected final void addTerminationOperationsSupport(TerminalOperation... operations) {
    for (TerminalOperation operation : operations) {
      mySupportedTerminalOperations.put(operation.getName(), operation);
    }
  }

  private Operation findOperation(String name, StreamCallType callType) {
    switch (callType) {
      case INTERMEDIATE:
        return mySupportedIntermediateOperations.get(name);
      case TERMINATOR:
        return mySupportedTerminalOperations.get(name);
      default:
        throw new IllegalStateException("Unsupported call type: " + callType + " for call: " + name);
    }
  }
}
