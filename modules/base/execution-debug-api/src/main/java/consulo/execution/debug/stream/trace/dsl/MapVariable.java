// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl;

import consulo.execution.debug.stream.trace.impl.handler.type.MapType;
import jakarta.annotation.Nonnull;

/**
 * @author Vitaliy.Bibaev
 */
public interface MapVariable extends Variable {
  @Override
  @Nonnull
  MapType getType();

  @Nonnull
  Expression get(@Nonnull Expression key);

  @Nonnull
  Expression set(@Nonnull Expression key, @Nonnull Expression newValue);

  @Nonnull
  Expression contains(@Nonnull Expression key);

  @Nonnull
  Expression size();

  @Nonnull
  Expression keys();

  @Nonnull
  CodeBlock computeIfAbsent(@Nonnull Dsl dsl, @Nonnull Expression key, @Nonnull Expression valueIfAbsent, @Nonnull Variable target);

  @Nonnull
  default VariableDeclaration defaultDeclaration() {
    return defaultDeclaration(true);
  }

  @Nonnull
  VariableDeclaration defaultDeclaration(boolean isMutable);

  @Nonnull
  Expression entries();

  @Nonnull
  CodeBlock convertToArray(@Nonnull Dsl dsl, @Nonnull String arrayName);
}
