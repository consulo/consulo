// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl;

import consulo.execution.debug.stream.trace.dsl.impl.AssignmentStatement;
import consulo.execution.debug.stream.trace.impl.handler.type.GenericType;
import consulo.execution.debug.stream.wrapper.IntermediateStreamCall;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Contains language-dependent logic
 *
 * @author Vitaliy.Bibaev
 */
public interface StatementFactory {
  static String commaSeparate(Expression... args) {
    return Arrays.stream(args)
      .map(Expression::toCode)
      .collect(Collectors.joining(", "));
  }

  Types getTypes();

  CompositeCodeBlock createEmptyCompositeCodeBlock();

  CodeBlock createEmptyCodeBlock();

  VariableDeclaration createVariableDeclaration(Variable variable, boolean isMutable);

  VariableDeclaration createVariableDeclaration(Variable variable, Expression init, boolean isMutable);

  ForLoopBody createEmptyForLoopBody(Variable iterateVariable);

  Convertable createForEachLoop(Variable iterateVariable, Expression collection, ForLoopBody loopBody);

  Convertable createForLoop(VariableDeclaration initialization,
                            Expression condition,
                            Expression afterThought,
                            ForLoopBody loopBody);

  LambdaBody createEmptyLambdaBody(String argName);

  Lambda createLambda(String argName, LambdaBody lambdaBody);

  Variable createVariable(GenericType type, String name);

  Expression and(Expression left, Expression right);

  Expression equals(Expression left, Expression right);

  Expression same(Expression left, Expression right);

  IfBranch createIfBranch(Expression condition, CodeBlock thenBlock);

  AssignmentStatement createAssignmentStatement(Variable variable, Expression expression);

  MapVariable createMapVariable(GenericType keyType, GenericType valueType, String name, boolean linked, Expression... args);

  ArrayVariable createArrayVariable(GenericType elementType, String name);

  Convertable createScope(CodeBlock codeBlock);

  TryBlock createTryBlock(CodeBlock block);

  VariableDeclaration createTimeVariableDeclaration();

  Expression currentTimeExpression();

  Expression updateCurrentTimeExpression();

  Expression currentNanosecondsExpression();

  Expression createNewArrayExpression(GenericType elementType, Expression... args);

  Expression createNewSizedArray(GenericType elementType, Expression size);

  Expression createNewListExpression(GenericType elementType, Expression... args);

  IntermediateStreamCall createPeekCall(GenericType elementsType, String lambda);

  default IntermediateStreamCall createPeekCall(GenericType elementsType, Lambda lambda) {
    return createPeekCall(elementsType, lambda.toCode());
  }

  ListVariable createListVariable(GenericType elementType, String name);

  Expression not(Expression expression);
}
