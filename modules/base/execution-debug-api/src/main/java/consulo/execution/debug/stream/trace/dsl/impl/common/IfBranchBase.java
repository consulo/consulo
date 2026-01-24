// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl.impl.common;

import consulo.execution.debug.stream.trace.dsl.*;
import jakarta.annotation.Nullable;

import java.util.function.Consumer;

/**
 * @author Vitaliy.Bibaev
 */
public abstract class IfBranchBase implements IfBranch {
  protected final Expression condition;
  protected final CodeBlock thenBlock;
  private final StatementFactory statementFactory;

  @Nullable
  protected Statement elseBlock;

  public IfBranchBase(Expression condition, CodeBlock thenBlock, StatementFactory statementFactory) {
    this.condition = condition;
    this.thenBlock = thenBlock;
    this.statementFactory = statementFactory;
  }

  @Override
  public void elseBranch(Consumer<CodeBlock> init) {
    CodeBlock block = statementFactory.createEmptyCodeBlock();
    init.accept(block);
    elseBlock = block;
  }

  @Override
  public IfBranch elseIfBranch(Expression condition, Consumer<CodeBlock> init) {
    CodeBlock block = statementFactory.createEmptyCodeBlock();
    init.accept(block);
    IfBranch elseIfStatement = statementFactory.createIfBranch(condition, block);
    CodeBlock codeBlock = statementFactory.createEmptyCompositeCodeBlock();
    codeBlock.addStatement(elseIfStatement);
    elseBlock = codeBlock;
    return elseIfStatement;
  }
}
