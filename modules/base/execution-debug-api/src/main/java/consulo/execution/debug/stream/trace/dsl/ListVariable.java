// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl;

import consulo.execution.debug.stream.trace.dsl.impl.TextExpression;
import consulo.execution.debug.stream.trace.impl.handler.type.ListType;
import jakarta.annotation.Nonnull;

/**
 * @author Vitaliy.Bibaev
 */
public interface ListVariable extends Variable {
  @Override
  @Nonnull
  ListType getType();

  @Nonnull
  Expression get(@Nonnull Expression index);

  @Nonnull
  default Expression get(int index) {
    return get(new TextExpression(Integer.toString(index)));
  }

  @Nonnull
  Expression set(@Nonnull Expression index, @Nonnull Expression newValue);

  @Nonnull
  default Expression set(int index, @Nonnull Expression newValue) {
    return set(new TextExpression(Integer.toString(index)), newValue);
  }

  @Nonnull
  Expression contains(@Nonnull Expression element);

  @Nonnull
  Expression size();

  @Nonnull
  Expression add(@Nonnull Expression element);

  @Nonnull
  VariableDeclaration defaultDeclaration();
}
