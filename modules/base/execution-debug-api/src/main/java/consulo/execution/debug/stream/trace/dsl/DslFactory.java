// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl;

import consulo.execution.debug.stream.trace.dsl.impl.TextExpression;
import consulo.execution.debug.stream.trace.impl.handler.type.GenericType;
import consulo.execution.debug.stream.wrapper.IntermediateStreamCall;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author Vitaliy.Bibaev
 */
public interface DslFactory {
    default Lambda lambda(String argName, Consumer<LambdaBody> init) {
        return lambda(argName, (lambdaBody, expression) -> init.accept(lambdaBody));
    }

    Lambda lambda(String argName, BiConsumer<LambdaBody, Expression> init);

    Variable variable(GenericType type, String name);

    ArrayVariable array(GenericType elementType, String name);

    ListVariable list(GenericType elementType, String name);

    Expression newList(GenericType elementType, Expression... args);

    Expression newArray(GenericType elementType, Expression... args);

    Expression newSizedArray(GenericType elementType, Expression size);

    default Expression newSizedArray(GenericType elementType, int size) {
        return newSizedArray(elementType, expr(String.valueOf(size)));
    }

    MapVariable map(GenericType keyType, GenericType valueType, String name, Expression... args);

    MapVariable linkedMap(GenericType keyType, GenericType valueType, String name, Expression... args);

    VariableDeclaration declaration(Variable variable, Expression init, boolean isMutable);

    default Expression expr(String text) {
        return new TextExpression(text);
    }

    Expression and(Expression left, Expression right);

    Expression equals(Expression left, Expression right);

    Expression same(Expression left, Expression right);

    Expression not(Expression expression);

    VariableDeclaration timeDeclaration();

    Expression currentTime();

    Expression updateTime();

    Expression currentNanoseconds();

    IntermediateStreamCall createPeekCall(GenericType elementType, Lambda lambda);
}
