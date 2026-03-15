// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl.impl;

import consulo.execution.debug.stream.trace.dsl.*;

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

  protected CodeBlockBase(StatementFactory factory) {
    myFactory = factory;
  }

  @Override
  public int getSize() {
    return myStatements.size();
  }

  @Override
  public void addStatement(Statement statement) {
    myStatements.add(statement);
  }

  @Override
  public void statement(Supplier<Statement> statement) {
    myStatements.add(Objects.requireNonNull(statement.get()));
  }

  
  @Override
  public Variable declare(Variable variable, boolean isMutable) {
    return declare(myFactory.createVariableDeclaration(variable, isMutable));
  }

  
  @Override
  public Variable declare(Variable variable, Expression init, boolean isMutable) {
    return declare(myFactory.createVariableDeclaration(variable, init, isMutable));
  }

  
  @Override
  public Variable declare(VariableDeclaration declaration) {
    addStatement(declaration);
    return declaration.getVariable();
  }

  @Override
  public void forLoop(VariableDeclaration initialization,
                      Expression condition,
                      Expression afterThought,
                      Consumer<ForLoopBody> init) {
    ForLoopBody loopBody = myFactory.createEmptyForLoopBody(initialization.getVariable());
    init.accept(loopBody);
    addStatement(myFactory.createForLoop(initialization, condition, afterThought, loopBody));
  }

  
  @Override
  public TryBlock tryBlock(Consumer<CodeBlock> init) {
    CodeBlock codeBlock = myFactory.createEmptyCodeBlock();
    init.accept(codeBlock);
    TryBlock tryBlock = myFactory.createTryBlock(codeBlock);
    myStatements.add(Objects.requireNonNull(tryBlock));
    return tryBlock;
  }

  
  @Override
  public IfBranch ifBranch(Expression condition, Consumer<CodeBlock> init) {
    CodeBlock ifBody = myFactory.createEmptyCodeBlock();
    init.accept(ifBody);
    IfBranch branch = myFactory.createIfBranch(condition, ifBody);
    addStatement(branch);
    return branch;
  }

  
  @Override
  public Expression call(Expression receiver, String methodName, Expression... args) {
    Expression call = receiver.call(methodName, args);
    addStatement(call);
    return call;
  }

  @Override
  public void forEachLoop(Variable iterateVariable,
                          Expression collection,
                          Consumer<ForLoopBody> init) {
    ForLoopBody loopBody = myFactory.createEmptyForLoopBody(iterateVariable);
    init.accept(loopBody);
    addStatement(myFactory.createForEachLoop(iterateVariable, collection, loopBody));
  }

  @Override
  public void scope(Consumer<CodeBlock> init) {
    CodeBlock codeBlock = myFactory.createEmptyCodeBlock();
    init.accept(codeBlock);
    addStatement(myFactory.createScope(codeBlock));
  }

  @Override
  public void assign(Variable variable, Expression expression) {
    AssignmentStatement assignmentStatement = myFactory.createAssignmentStatement(variable, expression);
    addStatement(assignmentStatement);
  }

  @Override
  public void doReturn(Expression expression) {
    addStatement(new TextExpression("return " + expression.toCode()));
  }

  @Override
  public void add(CodeBlock block) {
    for (Convertable statement : block.getStatements()) {
      addStatement(statement);
    }
  }

  @Override
  public void addStatement(Convertable statement) {
    myStatements.add(statement);
  }

  
  @Override
  public List<Convertable> getStatements() {
    return new ArrayList<>(myStatements);
  }
}
