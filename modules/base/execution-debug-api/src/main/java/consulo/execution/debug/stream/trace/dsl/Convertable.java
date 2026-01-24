// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl;

import jakarta.annotation.Nonnull;
import org.jetbrains.annotations.NonNls;

/**
 * @author Vitaliy.Bibaev
 */
public interface Convertable {
  @NonNls
  @Nonnull
  default String toCode() {
    return toCode(0);
  }

  @NonNls
  @Nonnull
  String toCode(int indent);

  @Nonnull
  default String withIndent(@Nonnull String text, int indent) {
    return "  ".repeat(indent) + text;
  }
}
