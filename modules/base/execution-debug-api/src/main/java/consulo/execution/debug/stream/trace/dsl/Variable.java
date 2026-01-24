// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl;

import consulo.execution.debug.stream.trace.impl.handler.type.GenericType;
import jakarta.annotation.Nonnull;

/**
 * @author Vitaliy.Bibaev
 */
public interface Variable extends Expression {
  @Nonnull
  GenericType getType();

  @Nonnull
  String getName();

  @Override
  @Nonnull
  default String toCode(int indent) {
    return withIndent(getName(), indent);
  }
}
