// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.impl.interpret.ex;

import jakarta.annotation.Nonnull;

public class ResolveException extends IllegalStateException {
  ResolveException(@Nonnull String s) {
    super(s);
  }

  ResolveException(@Nonnull String message, @Nonnull Throwable cause) {
    super(message, cause);
  }
}
