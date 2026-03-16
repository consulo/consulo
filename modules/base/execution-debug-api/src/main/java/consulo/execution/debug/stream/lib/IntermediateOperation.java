// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.lib;

import consulo.execution.debug.stream.trace.IntermediateCallHandler;
import consulo.execution.debug.stream.trace.dsl.Dsl;
import consulo.execution.debug.stream.wrapper.IntermediateStreamCall;

/**
 * @author Vitaliy.Bibaev
 */
public interface IntermediateOperation extends Operation {
  
  IntermediateCallHandler getTraceHandler(int callOrder, IntermediateStreamCall call, Dsl dsl);
}
