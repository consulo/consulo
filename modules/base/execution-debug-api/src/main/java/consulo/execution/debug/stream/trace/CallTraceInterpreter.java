// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace;

import consulo.execution.debug.stream.wrapper.StreamCall;
import jakarta.annotation.Nonnull;

/**
 * @author Vitaliy.Bibaev
 */
public interface CallTraceInterpreter {
  @Nonnull
  TraceInfo resolve(@Nonnull StreamCall call, @Nonnull Value value);
}
