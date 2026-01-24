// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl;

import consulo.execution.debug.stream.trace.dsl.impl.TextExpression;
import consulo.execution.debug.stream.trace.impl.handler.type.GenericType;
import consulo.execution.debug.stream.wrapper.IntermediateStreamCall;
import jakarta.annotation.Nonnull;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author Vitaliy.Bibaev
 */
public interface DslFactory {
    default Lambda lambda(@Nonnull String argName, @Nonnull Consumer<LambdaBody> init) {
        return lambda(argName, (lambdaBody, expression) -> init.accept(lambdaBody));
    }

    Lambda lambda(@Nonnull String argName, @Nonnull BiConsumer<LambdaBody, Expression> init);

    Variable variable(@Nonnull GenericType type, @Nonnull String name);

    ArrayVariable array(@Nonnull GenericType elementType, @Nonnull String name);

    ListVariable list(@Nonnull GenericType elementType, @Nonnull String name);

    Expression newList(@Nonnull GenericType elementType, @Nonnull Expression... args);

    Expression newArray(@Nonnull GenericType elementType, @Nonnull Expression... args);

    Expression newSizedArray(@Nonnull GenericType elementType, @Nonnull Expression size);

    default Expression newSizedArray(@Nonnull GenericType elementType, int size) {
        return newSizedArray(elementType, expr(String.valueOf(size)));
    }

    MapVariable map(@Nonnull GenericType keyType, @Nonnull GenericType valueType, @Nonnull String name, @Nonnull Expression... args);

    MapVariable linkedMap(@Nonnull GenericType keyType, @Nonnull GenericType valueType, @Nonnull String name, @Nonnull Expression... args);

    VariableDeclaration declaration(@Nonnull Variable variable, @Nonnull Expression init, boolean isMutable);

    default Expression expr(@Nonnull String text) {
        return new TextExpression(text);
    }

    Expression and(@Nonnull Expression left, @Nonnull Expression right);

    Expression equals(@Nonnull Expression left, @Nonnull Expression right);

    Expression same(@Nonnull Expression left, @Nonnull Expression right);

    Expression not(@Nonnull Expression expression);

    VariableDeclaration timeDeclaration();

    Expression currentTime();

    Expression updateTime();

    Expression currentNanoseconds();

    IntermediateStreamCall createPeekCall(@Nonnull GenericType elementType, @Nonnull Lambda lambda);
}
