// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.impl;

import consulo.execution.debug.stream.lib.HandlerFactory;
import consulo.execution.debug.stream.trace.IntermediateCallHandler;
import consulo.execution.debug.stream.trace.TerminatorCallHandler;
import consulo.execution.debug.stream.trace.TraceExpressionBuilder;
import consulo.execution.debug.stream.trace.TraceHandler;
import consulo.execution.debug.stream.trace.dsl.*;
import consulo.execution.debug.stream.trace.dsl.impl.TextExpression;
import consulo.execution.debug.stream.trace.impl.handler.type.GenericType;
import consulo.execution.debug.stream.wrapper.IntermediateStreamCall;
import consulo.execution.debug.stream.wrapper.StreamChain;
import consulo.execution.debug.stream.wrapper.TerminatorStreamCall;
import consulo.execution.debug.stream.wrapper.impl.StreamChainImpl;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public abstract class TraceExpressionBuilderBase implements TraceExpressionBuilder {
    protected final Dsl dsl;
    private final HandlerFactory handlerFactory;
    protected final String resultVariableName = "myRes";

    protected TraceExpressionBuilderBase(@Nonnull Dsl dsl, @Nonnull HandlerFactory handlerFactory) {
        this.dsl = dsl;
        this.handlerFactory = handlerFactory;
    }

    @Nonnull
    @Override
    public String createTraceExpression(@Nonnull StreamChain chain) {
        List<IntermediateCallHandler> intermediateHandlers = new ArrayList<>();
        List<IntermediateStreamCall> intermediateCalls = chain.getIntermediateCalls();
        for (int i = 0; i < intermediateCalls.size(); i++) {
            intermediateHandlers.add(handlerFactory.getForIntermediate(i, intermediateCalls.get(i)));
        }

        TerminatorStreamCall terminatorCall = chain.getTerminationCall();
        TerminatorCallHandler terminatorHandler = handlerFactory.getForTermination(terminatorCall, "evaluationResult[0]");

        StreamChain traceChain = buildTraceChain(chain, intermediateHandlers, terminatorHandler);

        int infoArraySize = 2 + intermediateHandlers.size();
        ArrayVariable info = dsl.array(dsl.getTypes().ANY(), "info");
        Variable streamResult = dsl.variable(dsl.getTypes().nullable(types -> types.ANY()), "streamResult");
        CodeBlock declarations = buildDeclarations(intermediateHandlers, terminatorHandler);

        CodeBlock tracingCall = buildStreamExpression(traceChain, streamResult);
        CodeBlock fillingInfoArray = buildFillInfo(intermediateHandlers, terminatorHandler, info);

        Variable result = dsl.variable(dsl.getTypes().ANY(), resultVariableName);

        return dsl.code(code -> {
            code.scope(scope -> {
                // TODO: avoid language dependent code
                Variable startTime = scope.declare(dsl.variable(dsl.getTypes().LONG(), "startTime"), dsl.currentNanoseconds(), false);
                scope.declare(info, dsl.newSizedArray(dsl.getTypes().ANY(), infoArraySize), false);
                scope.declare(dsl.timeDeclaration());
                scope.add(declarations);
                scope.add(tracingCall);
                scope.add(fillingInfoArray);

                ArrayVariable elapsedTime = (ArrayVariable) scope.declare(
                    dsl.array(dsl.getTypes().LONG(), "elapsedTime"),
                    dsl.newArray(dsl.getTypes().LONG(), new TextExpression(dsl.currentNanoseconds() + " - " + startTime.toCode())),
                    false
                );
                scope.statement(() -> {
                    code.assign(result, dsl.newArray(dsl.getTypes().ANY(), info, streamResult, elapsedTime));
                    return null;
                });
            });
        });
    }

    @Nonnull
    private StreamChain buildTraceChain(
        @Nonnull StreamChain chain,
        @Nonnull List<IntermediateCallHandler> intermediateCallHandlers,
        @Nonnull TerminatorCallHandler terminatorHandler
    ) {
        List<IntermediateStreamCall> newIntermediateCalls = new ArrayList<>();

        newIntermediateCalls.add(createTimePeekCall(chain.getQualifierExpression().getTypeAfter()));

        List<IntermediateStreamCall> intermediateCalls = chain.getIntermediateCalls();

        assert intermediateCalls.size() == intermediateCallHandlers.size();

        for (int i = 0; i < intermediateCalls.size(); i++) {
            IntermediateStreamCall call = intermediateCalls.get(i);
            IntermediateCallHandler handler = intermediateCallHandlers.get(i);

            newIntermediateCalls.addAll(handler.additionalCallsBefore());
            newIntermediateCalls.add(handler.transformCall(call));
            newIntermediateCalls.addAll(handler.additionalInseparableCalls());
            newIntermediateCalls.add(createTimePeekCall(call.getTypeAfter()));
            newIntermediateCalls.addAll(handler.additionalCallsAfter());
        }

        newIntermediateCalls.addAll(terminatorHandler.additionalCallsBefore());
        TerminatorStreamCall terminatorCall = terminatorHandler.transformCall(chain.getTerminationCall());

        return new StreamChainImpl(chain.getQualifierExpression(), newIntermediateCalls, terminatorCall, chain.getContext());
    }

    @Nonnull
    private IntermediateStreamCall createTimePeekCall(@Nonnull GenericType elementType) {
        return dsl.createPeekCall(elementType, dsl.lambda("x", lambda -> {
            lambda.doReturn(dsl.updateTime());
        }));
    }

    @Nonnull
    private CodeBlock buildDeclarations(
        @Nonnull List<IntermediateCallHandler> intermediateCallsHandlers,
        @Nonnull TerminatorCallHandler terminatorHandler
    ) {
        return dsl.block(block -> {
            for (IntermediateCallHandler handler : intermediateCallsHandlers) {
                for (VariableDeclaration variable : handler.additionalVariablesDeclaration()) {
                    block.declare(variable);
                }
            }
            for (VariableDeclaration variable : terminatorHandler.additionalVariablesDeclaration()) {
                block.declare(variable);
            }
        });
    }

    @Nonnull
    private CodeBlock buildStreamExpression(@Nonnull StreamChain chain, @Nonnull Variable streamResult) {
        GenericType resultType = chain.getTerminationCall().getResultType();
        return dsl.block(block -> {
            block.declare(streamResult, dsl.getNullExpression(), true);
            GenericType elementType = evaluationResultArrayElementType(resultType);
            ArrayVariable evaluationResult = dsl.array(elementType, "evaluationResult");

            if (!resultType.equals(dsl.getTypes().VOID())) {
                block.declare(evaluationResult, dsl.newArray(elementType, new TextExpression(elementType.getDefaultValue())), true);
            }

            block.tryBlock(tryBlock -> {
                if (resultType.equals(dsl.getTypes().VOID())) {
                    tryBlock.statement(() -> {
                        block.assign(streamResult, dsl.newSizedArray(dsl.getTypes().ANY(), 1));
                        return null;
                    });
                    tryBlock.statement(() -> new TextExpression(chain.getText()));
                }
                else {
                    tryBlock.statement(() -> evaluationResult.set(0, new TextExpression(chain.getText())));
                    tryBlock.statement(() -> {
                        block.assign(streamResult, evaluationResult);
                        return null;
                    });
                }
            }).doCatch(dsl.variable(dsl.getTypes().EXCEPTION(), "t"), catchBlock -> {
                // TODO: add exception variable as a property of catch code block
                catchBlock.statement(() -> {
                    block.assign(streamResult, dsl.newArray(dsl.getTypes().EXCEPTION(), new TextExpression("t")));
                    return null;
                });
            });
        });
    }

    @Nonnull
    protected GenericType evaluationResultArrayElementType(@Nonnull GenericType resultType) {
        return resultType;
    }

    @Nonnull
    private CodeBlock buildFillInfo(
        @Nonnull List<IntermediateCallHandler> intermediateCallsHandlers,
        @Nonnull TerminatorCallHandler terminatorHandler,
        @Nonnull ArrayVariable info
    ) {
        List<TraceHandler> handlers = new ArrayList<>(intermediateCallsHandlers);
        handlers.add(terminatorHandler);

        return dsl.block(block -> {
            for (int i = 0; i < handlers.size(); i++) {
                final int index = i;
                TraceHandler handler = handlers.get(i);
                block.scope(scope -> {
                    scope.add(handler.prepareResult());
                    scope.statement(() -> info.set(index, handler.getResultExpression()));
                });
            }
        });
    }
}
