// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl.impl;

import consulo.execution.debug.stream.trace.dsl.Expression;
import consulo.execution.debug.stream.trace.dsl.Variable;
import consulo.execution.debug.stream.trace.impl.handler.type.GenericType;
import jakarta.annotation.Nonnull;

/**
 * @author Vitaliy.Bibaev
 */
public class VariableImpl implements Variable {
  private final GenericType type;
  private final String name;

  public VariableImpl(@Nonnull GenericType type, @Nonnull String name) {
    this.type = type;
    this.name = name;
  }

  @Nonnull
  @Override
  public GenericType getType() {
    return type;
  }

  @Nonnull
  @Override
  public String getName() {
    return name;
  }

  @Nonnull
  @Override
  public Expression call(@Nonnull String callName, @Nonnull Expression... args) {
    StringBuilder sb = new StringBuilder();
    sb.append(name).append(".").append(callName).append("(");
    for (int i = 0; i < args.length; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(args[i].toCode());
    }
    sb.append(")");
    return new TextExpression(sb.toString());
  }

  @Override
  public String toString() {
    return toCode();
  }
}
