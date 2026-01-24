// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.impl.handler.unified;

import consulo.execution.debug.stream.trace.dsl.*;
import consulo.execution.debug.stream.trace.dsl.impl.TextExpression;
import consulo.execution.debug.stream.trace.impl.handler.type.GenericType;
import consulo.execution.debug.stream.wrapper.IntermediateStreamCall;
import jakarta.annotation.Nonnull;

import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public class PeekTraceHandler extends HandlerBase.Intermediate {
    private final GenericType myTypeBefore;
    private final MapVariable myBeforeMap;
    private final MapVariable myAfterMap;

    public PeekTraceHandler(int num, String callName, GenericType typeBefore, GenericType typeAfter, Dsl dsl) {
        super(dsl);
        myTypeBefore = typeBefore;
        myBeforeMap = dsl.linkedMap(dsl.getTypes().INT(), typeBefore, callName + "Peek" + num + "Before");
        myAfterMap = dsl.linkedMap(dsl.getTypes().INT(), typeAfter, callName + "Peek" + num + "After");
    }

    public MapVariable getBeforeMap() {
        return myBeforeMap;
    }

    public MapVariable getAfterMap() {
        return myAfterMap;
    }

    @Nonnull
    @Override
    public List<VariableDeclaration> additionalVariablesDeclaration() {
        return List.of(myBeforeMap.defaultDeclaration(), myAfterMap.defaultDeclaration());
    }

    @Nonnull
    @Override
    public CodeBlock prepareResult() {
        return dsl.block(block -> {
            block.add(myBeforeMap.convertToArray(block, "beforeArray"));
            block.add(myAfterMap.convertToArray(block, "afterArray"));
        });
    }

    @Nonnull
    @Override
    public Expression getResultExpression() {
        return dsl.newArray(dsl.getTypes().ANY(), new TextExpression("beforeArray"), new TextExpression("afterArray"));
    }

    @Nonnull
    @Override
    public List<IntermediateStreamCall> additionalCallsBefore() {
        Lambda lambda = dsl.lambda("x", context -> {
            context.doReturn(myBeforeMap.set(dsl.currentTime(), context.getLambdaArg()));
        });

        return List.of(dsl.createPeekCall(myTypeBefore, lambda));
    }

    @Nonnull
    @Override
    public List<IntermediateStreamCall> additionalCallsAfter() {
        Lambda lambda = dsl.lambda("x", context -> {
            context.doReturn(myAfterMap.set(dsl.currentTime(), context.getLambdaArg()));
        });

        return List.of(dsl.createPeekCall(myTypeBefore, lambda));
    }
}
