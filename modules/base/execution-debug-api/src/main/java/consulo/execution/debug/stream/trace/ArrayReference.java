// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.trace;

import jakarta.annotation.Nullable;

public interface ArrayReference extends Value {
  @Nullable
  Value getValue(int i);

  int length();
}
