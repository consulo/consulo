// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl;

import consulo.execution.debug.stream.trace.dsl.impl.TextExpression;
import consulo.execution.debug.stream.trace.impl.handler.type.ArrayType;
import jakarta.annotation.Nonnull;

/**
 * @author Vitaliy.Bibaev
 */
public interface ArrayVariable extends Variable {
  @Override
  @Nonnull
  ArrayType getType();

  @Nonnull
  Expression get(@Nonnull Expression index);

  @Nonnull
  default Expression get(int index) {
    return get(new TextExpression(Integer.toString(index)));
  }

  @Nonnull
  Expression set(@Nonnull Expression index, @Nonnull Expression value);

  @Nonnull
  default Expression set(int index, @Nonnull Expression value) {
    return set(new TextExpression(Integer.toString(index)), value);
  }

  @Nonnull
  VariableDeclaration defaultDeclaration(@Nonnull Expression size);
}
