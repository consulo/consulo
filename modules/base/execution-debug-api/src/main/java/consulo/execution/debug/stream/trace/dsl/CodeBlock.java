// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl;

import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * @author Vitaliy.Bibaev
 */
public interface CodeBlock extends Statement {
    int getSize();

    Variable declare(Variable variable, boolean isMutable);

    Variable declare(Variable variable, Expression init, boolean isMutable);

    Variable declare(VariableDeclaration declaration);

    void forEachLoop(Variable iterateVariable, Expression collection, Consumer<ForLoopBody> init);

    void forLoop(VariableDeclaration initialization, Expression condition, Expression afterThought, Consumer<ForLoopBody> init);

    TryBlock tryBlock(Consumer<CodeBlock> init);

    void scope(Consumer<CodeBlock> init);

    IfBranch ifBranch(Expression condition, Consumer<CodeBlock> init);

    Expression call(Expression receiver, String methodName, Expression... args);

    void doReturn(Expression expression);

    void statement(Supplier<Statement> statement);

    void addStatement(Statement statement);

    void assign(Variable variable, Expression expression);

    void add(CodeBlock block);

    void addStatement(@Nonnull Convertable statement);

    List<Convertable> getStatements();
}
