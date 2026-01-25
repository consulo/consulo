// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl.impl;

import consulo.execution.debug.stream.trace.dsl.*;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Vitaliy.Bibaev
 */
public abstract class CodeBlockBase implements CompositeCodeBlock {
  private final StatementFactory myFactory;
  private final List<Convertable> myStatements = new ArrayList<>();

  protected CodeBlockBase(@Nonnull StatementFactory factory) {
    myFactory = factory;
  }

  @Override
  public int getSize() {
    return myStatements.size();
  }

  @Override
  public void addStatement(@Nonnull Statement statement) {
    myStatements.add(statement);
  }

  @Override
  public void statement(@Nonnull Supplier<Statement> statement) {
    myStatements.add(Objects.requireNonNull(statement.get()));
  }

  @Nonnull
  @Override
  public Variable declare(@Nonnull Variable variable, boolean isMutable) {
    return declare(myFactory.createVariableDeclaration(variable, isMutable));
  }

  @Nonnull
  @Override
  public Variable declare(@Nonnull Variable variable, @Nonnull Expression init, boolean isMutable) {
    return declare(myFactory.createVariableDeclaration(variable, init, isMutable));
  }

  @Nonnull
  @Override
  public Variable declare(@Nonnull VariableDeclaration declaration) {
    addStatement(declaration);
    return declaration.getVariable();
  }

  @Override
  public void forLoop(@Nonnull VariableDeclaration initialization,
                      @Nonnull Expression condition,
                      @Nonnull Expression afterThought,
                      @Nonnull Consumer<ForLoopBody> init) {
    ForLoopBody loopBody = myFactory.createEmptyForLoopBody(initialization.getVariable());
    init.accept(loopBody);
    addStatement(myFactory.createForLoop(initialization, condition, afterThought, loopBody));
  }

  @Nonnull
  @Override
  public TryBlock tryBlock(@Nonnull Consumer<CodeBlock> init) {
    CodeBlock codeBlock = myFactory.createEmptyCodeBlock();
    init.accept(codeBlock);
    TryBlock tryBlock = myFactory.createTryBlock(codeBlock);
    myStatements.add(Objects.requireNonNull(tryBlock));
    return tryBlock;
  }

  @Nonnull
  @Override
  public IfBranch ifBranch(@Nonnull Expression condition, @Nonnull Consumer<CodeBlock> init) {
    CodeBlock ifBody = myFactory.createEmptyCodeBlock();
    init.accept(ifBody);
    IfBranch branch = myFactory.createIfBranch(condition, ifBody);
    addStatement(branch);
    return branch;
  }

  @Nonnull
  @Override
  public Expression call(@Nonnull Expression receiver, @Nonnull String methodName, @Nonnull Expression... args) {
    Expression call = receiver.call(methodName, args);
    addStatement(call);
    return call;
  }

  @Override
  public void forEachLoop(@Nonnull Variable iterateVariable,
                          @Nonnull Expression collection,
                          @Nonnull Consumer<ForLoopBody> init) {
    ForLoopBody loopBody = myFactory.createEmptyForLoopBody(iterateVariable);
    init.accept(loopBody);
    addStatement(myFactory.createForEachLoop(iterateVariable, collection, loopBody));
  }

  @Override
  public void scope(@Nonnull Consumer<CodeBlock> init) {
    CodeBlock codeBlock = myFactory.createEmptyCodeBlock();
    init.accept(codeBlock);
    addStatement(myFactory.createScope(codeBlock));
  }

  @Override
  public void assign(@Nonnull Variable variable, @Nonnull Expression expression) {
    AssignmentStatement assignmentStatement = myFactory.createAssignmentStatement(variable, expression);
    addStatement(assignmentStatement);
  }

  @Override
  public void doReturn(@Nonnull Expression expression) {
    addStatement(new TextExpression("return " + expression.toCode()));
  }

  @Override
  public void add(@Nonnull CodeBlock block) {
    for (Convertable statement : block.getStatements()) {
      addStatement(statement);
    }
  }

  @Override
  public void addStatement(@Nonnull Convertable statement) {
    myStatements.add(statement);
  }

  @Nonnull
  @Override
  public List<Convertable> getStatements() {
    return new ArrayList<>(myStatements);
  }
}
