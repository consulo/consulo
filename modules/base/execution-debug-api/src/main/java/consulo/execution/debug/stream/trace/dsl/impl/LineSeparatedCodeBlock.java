// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl.impl;

import consulo.execution.debug.stream.trace.dsl.Convertable;
import consulo.execution.debug.stream.trace.dsl.Statement;
import consulo.execution.debug.stream.trace.dsl.StatementFactory;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * TODO: Add ability to add braces at the beginning and at the end
 *
 * @author Vitaliy.Bibaev
 */
public abstract class LineSeparatedCodeBlock extends CodeBlockBase {
  private final String statementSeparator;

  protected LineSeparatedCodeBlock(@Nonnull StatementFactory statementFactory) {
    this(statementFactory, "");
  }

  protected LineSeparatedCodeBlock(@Nonnull StatementFactory statementFactory, @Nonnull String statementSeparator) {
    super(statementFactory);
    this.statementSeparator = statementSeparator;
  }

  @Nonnull
  @Override
  public String toCode(int indent) {
    if (getSize() == 0) {
      return "";
    }

    StringBuilder builder = new StringBuilder();
    List<Convertable> statements = getStatements();
    for (Convertable convertable : statements) {
      builder.append(convertable.toCode(indent));
      if (convertable instanceof Statement) {
        builder.append(statementSeparator);
      }
      builder.append("\n");
    }
    return builder.toString();
  }
}
