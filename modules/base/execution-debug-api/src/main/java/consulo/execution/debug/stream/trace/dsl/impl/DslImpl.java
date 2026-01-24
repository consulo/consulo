// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl.impl;

import consulo.execution.debug.stream.trace.dsl.*;
import consulo.execution.debug.stream.trace.impl.handler.type.GenericType;
import consulo.execution.debug.stream.wrapper.IntermediateStreamCall;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Vitaliy.Bibaev
 */
public class DslImpl implements Dsl {
  private final StatementFactory statementFactory;
  private final Expression nullExpression;
  private final Expression thisExpression;

  public DslImpl(@Nonnull StatementFactory statementFactory) {
    this.statementFactory = statementFactory;
    this.nullExpression = new TextExpression("null");
    this.thisExpression = new TextExpression("this");
  }

  @Nonnull
  @Override
  public Expression getNullExpression() {
    return nullExpression;
  }

  @Nonnull
  @Override
  public Expression getThisExpression() {
    return thisExpression;
  }

  @Nonnull
  @Override
  public Types getTypes() {
    return statementFactory.getTypes();
  }

  @Nonnull
  @Override
  public Variable variable(@Nonnull GenericType type, @Nonnull String name) {
    return statementFactory.createVariable(type, name);
  }

  @Nonnull
  @Override
  public String code(@Nonnull Consumer<CodeContext> init) {
    MyContext fragment = new MyContext();
    init.accept(fragment);
    return fragment.toCode(0);
  }

  @Nonnull
  @Override
  public CodeBlock block(@Nonnull Consumer<CodeContext> init) {
    MyContext fragment = new MyContext();
    init.accept(fragment);
    return fragment;
  }

  @Nonnull
  @Override
  public ArrayVariable array(@Nonnull GenericType elementType, @Nonnull String name) {
    return statementFactory.createArrayVariable(elementType, name);
  }

  @Nonnull
  @Override
  public Expression newArray(@Nonnull GenericType elementType, @Nonnull Expression... args) {
    return statementFactory.createNewArrayExpression(elementType, args);
  }

  @Nonnull
  @Override
  public Expression newSizedArray(@Nonnull GenericType elementType, @Nonnull Expression size) {
    return statementFactory.createNewSizedArray(elementType, size);
  }

  @Nonnull
  @Override
  public MapVariable map(@Nonnull GenericType keyType,
                         @Nonnull GenericType valueType,
                         @Nonnull String name,
                         @Nonnull Expression... args) {
    return statementFactory.createMapVariable(keyType, valueType, name, false, args);
  }

  @Nonnull
  @Override
  public ListVariable list(@Nonnull GenericType elementType, @Nonnull String name) {
    return statementFactory.createListVariable(elementType, name);
  }

  @Nonnull
  @Override
  public Expression newList(@Nonnull GenericType elementType, @Nonnull Expression... args) {
    return statementFactory.createNewListExpression(elementType, args);
  }

  @Nonnull
  @Override
  public MapVariable linkedMap(@Nonnull GenericType keyType,
                                @Nonnull GenericType valueType,
                                @Nonnull String name,
                                @Nonnull Expression... args) {
    return statementFactory.createMapVariable(keyType, valueType, name, true, args);
  }

  @Nonnull
  @Override
  public Lambda lambda(@Nonnull String argName, @Nonnull BiConsumer<LambdaBody, Expression> init) {
    LambdaBody lambdaBody = statementFactory.createEmptyLambdaBody(argName);
    init.accept(lambdaBody, expr(argName));
    return statementFactory.createLambda(argName, lambdaBody);
  }

  @Nonnull
  @Override
  public VariableDeclaration declaration(@Nonnull Variable variable, @Nonnull Expression init, boolean isMutable) {
    return statementFactory.createVariableDeclaration(variable, init, isMutable);
  }

  @Nonnull
  @Override
  public VariableDeclaration timeDeclaration() {
    return statementFactory.createTimeVariableDeclaration();
  }

  @Nonnull
  @Override
  public Expression currentTime() {
    return statementFactory.currentTimeExpression();
  }

  @Nonnull
  @Override
  public Expression updateTime() {
    return statementFactory.updateCurrentTimeExpression();
  }

  @Nonnull
  @Override
  public Expression currentNanoseconds() {
    return statementFactory.currentNanosecondsExpression();
  }

  @Nonnull
  @Override
  public IntermediateStreamCall createPeekCall(@Nonnull GenericType elementType, @Nonnull Lambda lambda) {
    return statementFactory.createPeekCall(elementType, lambda);
  }

  @Nonnull
  @Override
  public Expression and(@Nonnull Expression left, @Nonnull Expression right) {
    return statementFactory.and(left, right);
  }

  @Nonnull
  @Override
  public Expression equals(@Nonnull Expression left, @Nonnull Expression right) {
    return statementFactory.equals(left, right);
  }

  @Nonnull
  @Override
  public Expression same(@Nonnull Expression left, @Nonnull Expression right) {
    return statementFactory.same(left, right);
  }

  @Nonnull
  @Override
  public Expression not(@Nonnull Expression expression) {
    return statementFactory.not(expression);
  }

  private class MyContext implements CodeContext, CodeBlock {
    private final CompositeCodeBlock delegate;

    private MyContext() {
      this.delegate = statementFactory.createEmptyCompositeCodeBlock();
    }

    // Dsl methods delegation
    @Nonnull
    @Override
    public Expression getNullExpression() {
      return DslImpl.this.getNullExpression();
    }

    @Nonnull
    @Override
    public Expression getThisExpression() {
      return DslImpl.this.getThisExpression();
    }

    @Nonnull
    @Override
    public Types getTypes() {
      return DslImpl.this.getTypes();
    }

    @Nonnull
    @Override
    public Variable variable(@Nonnull GenericType type, @Nonnull String name) {
      return DslImpl.this.variable(type, name);
    }

    @Nonnull
    @Override
    public CodeBlock block(@Nonnull Consumer<CodeContext> init) {
      return DslImpl.this.block(init);
    }

    @Nonnull
    @Override
    public String code(@Nonnull Consumer<CodeContext> init) {
      return DslImpl.this.code(init);
    }

    @Nonnull
    @Override
    public ArrayVariable array(@Nonnull GenericType elementType, @Nonnull String name) {
      return DslImpl.this.array(elementType, name);
    }

    @Nonnull
    @Override
    public Expression newArray(@Nonnull GenericType elementType, @Nonnull Expression... args) {
      return DslImpl.this.newArray(elementType, args);
    }

    @Nonnull
    @Override
    public Expression newSizedArray(@Nonnull GenericType elementType, @Nonnull Expression size) {
      return DslImpl.this.newSizedArray(elementType, size);
    }

    @Nonnull
    @Override
    public MapVariable map(@Nonnull GenericType keyType,
                           @Nonnull GenericType valueType,
                           @Nonnull String name,
                           @Nonnull Expression... args) {
      return DslImpl.this.map(keyType, valueType, name, args);
    }

    @Nonnull
    @Override
    public ListVariable list(@Nonnull GenericType elementType, @Nonnull String name) {
      return DslImpl.this.list(elementType, name);
    }

    @Nonnull
    @Override
    public Expression newList(@Nonnull GenericType elementType, @Nonnull Expression... args) {
      return DslImpl.this.newList(elementType, args);
    }

    @Nonnull
    @Override
    public MapVariable linkedMap(@Nonnull GenericType keyType,
                                  @Nonnull GenericType valueType,
                                  @Nonnull String name,
                                  @Nonnull Expression... args) {
      return DslImpl.this.linkedMap(keyType, valueType, name, args);
    }

    @Nonnull
    @Override
    public Lambda lambda(@Nonnull String argName, @Nonnull BiConsumer<LambdaBody, Expression> init) {
      return DslImpl.this.lambda(argName, init);
    }

    @Nonnull
    @Override
    public VariableDeclaration declaration(@Nonnull Variable variable, @Nonnull Expression init, boolean isMutable) {
      return DslImpl.this.declaration(variable, init, isMutable);
    }

    @Nonnull
    @Override
    public VariableDeclaration timeDeclaration() {
      return DslImpl.this.timeDeclaration();
    }

    @Nonnull
    @Override
    public Expression currentTime() {
      return DslImpl.this.currentTime();
    }

    @Nonnull
    @Override
    public Expression updateTime() {
      return DslImpl.this.updateTime();
    }

    @Nonnull
    @Override
    public Expression currentNanoseconds() {
      return DslImpl.this.currentNanoseconds();
    }

    @Nonnull
    @Override
    public IntermediateStreamCall createPeekCall(@Nonnull GenericType elementType, @Nonnull Lambda lambda) {
      return DslImpl.this.createPeekCall(elementType, lambda);
    }

    @Nonnull
    @Override
    public Expression and(@Nonnull Expression left, @Nonnull Expression right) {
      return DslImpl.this.and(left, right);
    }

    @Nonnull
    @Override
    public Expression equals(@Nonnull Expression left, @Nonnull Expression right) {
      return DslImpl.this.equals(left, right);
    }

    @Nonnull
    @Override
    public Expression same(@Nonnull Expression left, @Nonnull Expression right) {
      return DslImpl.this.same(left, right);
    }

    @Nonnull
    @Override
    public Expression not(@Nonnull Expression expression) {
      return DslImpl.this.not(expression);
    }

    // CodeBlock methods delegation
    @Override
    public int getSize() {
      return delegate.getSize();
    }

    @Override
    public void addStatement(@Nonnull Statement statement) {
      delegate.addStatement(statement);
    }

    @Override
    public void statement(@Nonnull Supplier<Statement> statement) {
      delegate.statement(statement);
    }

    @Nonnull
    @Override
    public Variable declare(@Nonnull Variable variable, boolean isMutable) {
      return delegate.declare(variable, isMutable);
    }

    @Nonnull
    @Override
    public Variable declare(@Nonnull Variable variable, @Nonnull Expression init, boolean isMutable) {
      return delegate.declare(variable, init, isMutable);
    }

    @Nonnull
    @Override
    public Variable declare(@Nonnull VariableDeclaration declaration) {
      return delegate.declare(declaration);
    }

    @Override
    public void forLoop(@Nonnull VariableDeclaration initialization,
                        @Nonnull Expression condition,
                        @Nonnull Expression afterThought,
                        @Nonnull Consumer<ForLoopBody> init) {
      delegate.forLoop(initialization, condition, afterThought, init);
    }

    @Nonnull
    @Override
    public TryBlock tryBlock(@Nonnull Consumer<CodeBlock> init) {
      return delegate.tryBlock(init);
    }

    @Nonnull
    @Override
    public IfBranch ifBranch(@Nonnull Expression condition, @Nonnull Consumer<CodeBlock> init) {
      return delegate.ifBranch(condition, init);
    }

    @Nonnull
    @Override
    public Expression call(@Nonnull Expression receiver, @Nonnull String methodName, @Nonnull Expression... args) {
      return delegate.call(receiver, methodName, args);
    }

    @Override
    public void forEachLoop(@Nonnull Variable iterateVariable,
                            @Nonnull Expression collection,
                            @Nonnull Consumer<ForLoopBody> init) {
      delegate.forEachLoop(iterateVariable, collection, init);
    }

    @Override
    public void scope(@Nonnull Consumer<CodeBlock> init) {
      delegate.scope(init);
    }

    @Override
    public void assign(@Nonnull Variable variable, @Nonnull Expression expression) {
      delegate.assign(variable, expression);
    }

    @Override
    public void doReturn(@Nonnull Expression expression) {
      delegate.doReturn(expression);
    }

    @Override
    public void add(@Nonnull CodeBlock block) {
      delegate.add(block);
    }

    @Override
    public void addStatement(@Nonnull Convertable statement) {
      delegate.addStatement(statement);
    }

    @Nonnull
    @Override
    public List<Convertable> getStatements() {
      return delegate.getStatements();
    }

    @Nonnull
    @Override
    public String toCode(int indent) {
      return delegate.toCode(indent);
    }
  }
}
