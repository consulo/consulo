// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.trace;

import consulo.execution.debug.stream.wrapper.StreamCall;

public interface CallTransformer<T extends StreamCall> {
  default T transformCall(T call) {
    return call;
  }
}