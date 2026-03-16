// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl.impl;

import consulo.execution.debug.stream.trace.dsl.*;
import consulo.execution.debug.stream.trace.impl.handler.type.GenericType;
import consulo.execution.debug.stream.wrapper.IntermediateStreamCall;

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

  public DslImpl(StatementFactory statementFactory) {
    this.statementFactory = statementFactory;
    this.nullExpression = new TextExpression("null");
    this.thisExpression = new TextExpression("this");
  }

  
  @Override
  public Expression getNullExpression() {
    return nullExpression;
  }

  
  @Override
  public Expression getThisExpression() {
    return thisExpression;
  }

  
  @Override
  public Types getTypes() {
    return statementFactory.getTypes();
  }

  
  @Override
  public Variable variable(GenericType type, String name) {
    return statementFactory.createVariable(type, name);
  }

  
  @Override
  public String code(Consumer<CodeContext> init) {
    MyContext fragment = new MyContext();
    init.accept(fragment);
    return fragment.toCode(0);
  }

  
  @Override
  public CodeBlock block(Consumer<CodeContext> init) {
    MyContext fragment = new MyContext();
    init.accept(fragment);
    return fragment;
  }

  
  @Override
  public ArrayVariable array(GenericType elementType, String name) {
    return statementFactory.createArrayVariable(elementType, name);
  }

  
  @Override
  public Expression newArray(GenericType elementType, Expression... args) {
    return statementFactory.createNewArrayExpression(elementType, args);
  }

  
  @Override
  public Expression newSizedArray(GenericType elementType, Expression size) {
    return statementFactory.createNewSizedArray(elementType, size);
  }

  
  @Override
  public MapVariable map(GenericType keyType,
                         GenericType valueType,
                         String name,
                         Expression... args) {
    return statementFactory.createMapVariable(keyType, valueType, name, false, args);
  }

  
  @Override
  public ListVariable list(GenericType elementType, String name) {
    return statementFactory.createListVariable(elementType, name);
  }

  
  @Override
  public Expression newList(GenericType elementType, Expression... args) {
    return statementFactory.createNewListExpression(elementType, args);
  }

  
  @Override
  public MapVariable linkedMap(GenericType keyType,
                                GenericType valueType,
                                String name,
                                Expression... args) {
    return statementFactory.createMapVariable(keyType, valueType, name, true, args);
  }

  
  @Override
  public Lambda lambda(String argName, BiConsumer<LambdaBody, Expression> init) {
    LambdaBody lambdaBody = statementFactory.createEmptyLambdaBody(argName);
    init.accept(lambdaBody, expr(argName));
    return statementFactory.createLambda(argName, lambdaBody);
  }

  
  @Override
  public VariableDeclaration declaration(Variable variable, Expression init, boolean isMutable) {
    return statementFactory.createVariableDeclaration(variable, init, isMutable);
  }

  
  @Override
  public VariableDeclaration timeDeclaration() {
    return statementFactory.createTimeVariableDeclaration();
  }

  
  @Override
  public Expression currentTime() {
    return statementFactory.currentTimeExpression();
  }

  
  @Override
  public Expression updateTime() {
    return statementFactory.updateCurrentTimeExpression();
  }

  
  @Override
  public Expression currentNanoseconds() {
    return statementFactory.currentNanosecondsExpression();
  }

  
  @Override
  public IntermediateStreamCall createPeekCall(GenericType elementType, Lambda lambda) {
    return statementFactory.createPeekCall(elementType, lambda);
  }

  
  @Override
  public Expression and(Expression left, Expression right) {
    return statementFactory.and(left, right);
  }

  
  @Override
  public Expression equals(Expression left, Expression right) {
    return statementFactory.equals(left, right);
  }

  
  @Override
  public Expression same(Expression left, Expression right) {
    return statementFactory.same(left, right);
  }

  
  @Override
  public Expression not(Expression expression) {
    return statementFactory.not(expression);
  }

  private class MyContext implements CodeContext, CodeBlock {
    private final CompositeCodeBlock delegate;

    private MyContext() {
      this.delegate = statementFactory.createEmptyCompositeCodeBlock();
    }

    // Dsl methods delegation
    
    @Override
    public Expression getNullExpression() {
      return DslImpl.this.getNullExpression();
    }

    
    @Override
    public Expression getThisExpression() {
      return DslImpl.this.getThisExpression();
    }

    
    @Override
    public Types getTypes() {
      return DslImpl.this.getTypes();
    }

    
    @Override
    public Variable variable(GenericType type, String name) {
      return DslImpl.this.variable(type, name);
    }

    
    @Override
    public CodeBlock block(Consumer<CodeContext> init) {
      return DslImpl.this.block(init);
    }

    
    @Override
    public String code(Consumer<CodeContext> init) {
      return DslImpl.this.code(init);
    }

    
    @Override
    public ArrayVariable array(GenericType elementType, String name) {
      return DslImpl.this.array(elementType, name);
    }

    
    @Override
    public Expression newArray(GenericType elementType, Expression... args) {
      return DslImpl.this.newArray(elementType, args);
    }

    
    @Override
    public Expression newSizedArray(GenericType elementType, Expression size) {
      return DslImpl.this.newSizedArray(elementType, size);
    }

    
    @Override
    public MapVariable map(GenericType keyType,
                           GenericType valueType,
                           String name,
                           Expression... args) {
      return DslImpl.this.map(keyType, valueType, name, args);
    }

    
    @Override
    public ListVariable list(GenericType elementType, String name) {
      return DslImpl.this.list(elementType, name);
    }

    
    @Override
    public Expression newList(GenericType elementType, Expression... args) {
      return DslImpl.this.newList(elementType, args);
    }

    
    @Override
    public MapVariable linkedMap(GenericType keyType,
                                  GenericType valueType,
                                  String name,
                                  Expression... args) {
      return DslImpl.this.linkedMap(keyType, valueType, name, args);
    }

    
    @Override
    public Lambda lambda(String argName, BiConsumer<LambdaBody, Expression> init) {
      return DslImpl.this.lambda(argName, init);
    }

    
    @Override
    public VariableDeclaration declaration(Variable variable, Expression init, boolean isMutable) {
      return DslImpl.this.declaration(variable, init, isMutable);
    }

    
    @Override
    public VariableDeclaration timeDeclaration() {
      return DslImpl.this.timeDeclaration();
    }

    
    @Override
    public Expression currentTime() {
      return DslImpl.this.currentTime();
    }

    
    @Override
    public Expression updateTime() {
      return DslImpl.this.updateTime();
    }

    
    @Override
    public Expression currentNanoseconds() {
      return DslImpl.this.currentNanoseconds();
    }

    
    @Override
    public IntermediateStreamCall createPeekCall(GenericType elementType, Lambda lambda) {
      return DslImpl.this.createPeekCall(elementType, lambda);
    }

    
    @Override
    public Expression and(Expression left, Expression right) {
      return DslImpl.this.and(left, right);
    }

    
    @Override
    public Expression equals(Expression left, Expression right) {
      return DslImpl.this.equals(left, right);
    }

    
    @Override
    public Expression same(Expression left, Expression right) {
      return DslImpl.this.same(left, right);
    }

    
    @Override
    public Expression not(Expression expression) {
      return DslImpl.this.not(expression);
    }

    // CodeBlock methods delegation
    @Override
    public int getSize() {
      return delegate.getSize();
    }

    @Override
    public void addStatement(Statement statement) {
      delegate.addStatement(statement);
    }

    @Override
    public void statement(Supplier<Statement> statement) {
      delegate.statement(statement);
    }

    
    @Override
    public Variable declare(Variable variable, boolean isMutable) {
      return delegate.declare(variable, isMutable);
    }

    
    @Override
    public Variable declare(Variable variable, Expression init, boolean isMutable) {
      return delegate.declare(variable, init, isMutable);
    }

    
    @Override
    public Variable declare(VariableDeclaration declaration) {
      return delegate.declare(declaration);
    }

    @Override
    public void forLoop(VariableDeclaration initialization,
                        Expression condition,
                        Expression afterThought,
                        Consumer<ForLoopBody> init) {
      delegate.forLoop(initialization, condition, afterThought, init);
    }

    
    @Override
    public TryBlock tryBlock(Consumer<CodeBlock> init) {
      return delegate.tryBlock(init);
    }

    
    @Override
    public IfBranch ifBranch(Expression condition, Consumer<CodeBlock> init) {
      return delegate.ifBranch(condition, init);
    }

    
    @Override
    public Expression call(Expression receiver, String methodName, Expression... args) {
      return delegate.call(receiver, methodName, args);
    }

    @Override
    public void forEachLoop(Variable iterateVariable,
                            Expression collection,
                            Consumer<ForLoopBody> init) {
      delegate.forEachLoop(iterateVariable, collection, init);
    }

    @Override
    public void scope(Consumer<CodeBlock> init) {
      delegate.scope(init);
    }

    @Override
    public void assign(Variable variable, Expression expression) {
      delegate.assign(variable, expression);
    }

    @Override
    public void doReturn(Expression expression) {
      delegate.doReturn(expression);
    }

    @Override
    public void add(CodeBlock block) {
      delegate.add(block);
    }

    @Override
    public void addStatement(Convertable statement) {
      delegate.addStatement(statement);
    }

    
    @Override
    public List<Convertable> getStatements() {
      return delegate.getStatements();
    }

    
    @Override
    public String toCode(int indent) {
      return delegate.toCode(indent);
    }
  }
}
