// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl;

/**
 * @author Vitaliy.Bibaev
 */
public interface CompositeCodeBlock extends CodeBlock {
  @Override
  default void add(CodeBlock block) {
    for (Convertable statement : block.getStatements()) {
      addStatement(statement);
    }
  }

  void addStatement(Convertable statement);
}
