// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.impl.handler.unified;

import consulo.execution.debug.stream.trace.IntermediateCallHandler;
import consulo.execution.debug.stream.trace.TerminatorCallHandler;
import consulo.execution.debug.stream.trace.TraceHandler;
import consulo.execution.debug.stream.trace.dsl.Dsl;

/**
 * @author Vitaliy.Bibaev
 */
public abstract class HandlerBase implements TraceHandler {
  protected final Dsl dsl;

  private HandlerBase(Dsl dsl) {
    this.dsl = dsl;
  }

  public abstract static class Intermediate extends HandlerBase implements IntermediateCallHandler {
    public Intermediate(Dsl dsl) {
      super(dsl);
    }
  }

  public abstract static class Terminal extends HandlerBase implements TerminatorCallHandler {
    public Terminal(Dsl dsl) {
      super(dsl);
    }
  }
}
