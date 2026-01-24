// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl.impl.common;

import consulo.execution.debug.stream.trace.dsl.CodeBlock;
import consulo.execution.debug.stream.trace.dsl.StatementFactory;
import consulo.execution.debug.stream.trace.dsl.TryBlock;
import consulo.execution.debug.stream.trace.dsl.Variable;
import jakarta.annotation.Nullable;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * @author Vitaliy.Bibaev
 */
public abstract class TryBlockBase implements TryBlock {
  protected final StatementFactory statementFactory;

  @Nullable
  protected CatchBlockDescriptor myCatchDescriptor;

  public TryBlockBase(StatementFactory statementFactory) {
    this.statementFactory = statementFactory;
  }

  @Override
  public boolean isCatchAdded() {
    return myCatchDescriptor != null;
  }

  @Override
  public void doCatch(Variable variable, Consumer<CodeBlock> handler) {
    CodeBlock catchBlock = statementFactory.createEmptyCodeBlock();
    handler.accept(catchBlock);
    myCatchDescriptor = new CatchBlockDescriptor(variable, catchBlock);
  }

  protected static class CatchBlockDescriptor {
    private final Variable variable;
    private final CodeBlock block;

    public CatchBlockDescriptor(Variable variable, CodeBlock block) {
      this.variable = variable;
      this.block = block;
    }

    public Variable getVariable() {
      return variable;
    }

    public CodeBlock getBlock() {
      return block;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      CatchBlockDescriptor that = (CatchBlockDescriptor) o;
      return Objects.equals(variable, that.variable) && Objects.equals(block, that.block);
    }

    @Override
    public int hashCode() {
      return Objects.hash(variable, block);
    }

    @Override
    public String toString() {
      return "CatchBlockDescriptor{" +
             "variable=" + variable +
             ", block=" + block +
             '}';
    }
  }
}
