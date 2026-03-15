// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.lib;

import consulo.execution.debug.stream.trace.CallTraceInterpreter;
import consulo.execution.debug.stream.wrapper.StreamCallType;

/**
 * @author Vitaliy.Bibaev
 */
public interface InterpreterFactory {
  
  CallTraceInterpreter getInterpreter(String callName, StreamCallType callType);
}
