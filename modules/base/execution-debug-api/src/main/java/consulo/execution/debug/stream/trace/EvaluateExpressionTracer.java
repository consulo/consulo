// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.execution.debug.stream.trace;

import consulo.execution.debug.XDebugSession;
import consulo.execution.debug.evaluation.XDebuggerEvaluator;
import consulo.execution.debug.frame.XStackFrame;
import consulo.execution.debug.frame.XValue;
import consulo.execution.debug.internal.XEvaluationCallbackBase;
import consulo.execution.debug.localize.XDebuggerLocalize;
import consulo.execution.debug.stream.wrapper.StreamChain;
import consulo.localize.LocalizeValue;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author Vitaliy.Bibaev
 */
public class EvaluateExpressionTracer implements StreamTracer {
    private final XDebugSession mySession;
    private final TraceExpressionBuilder myExpressionBuilder;
    private final TraceResultInterpreter myResultInterpreter;
    private final XValueInterpreter myXValueInterpreter;

    public EvaluateExpressionTracer(
        @Nonnull XDebugSession mySession,
        @Nonnull TraceExpressionBuilder myExpressionBuilder,
        @Nonnull TraceResultInterpreter myResultInterpreter,
        @Nonnull XValueInterpreter myXValueInterpreter
    ) {
        this.mySession = mySession;
        this.myExpressionBuilder = myExpressionBuilder;
        this.myResultInterpreter = myResultInterpreter;
        this.myXValueInterpreter = myXValueInterpreter;
    }

    @Nonnull
    @Override
    public Result trace(@Nonnull StreamChain chain) {
        String streamTraceExpression = myExpressionBuilder.createTraceExpression(chain);

        XStackFrame stackFrame = mySession.getCurrentStackFrame();
        XDebuggerEvaluator evaluator = mySession.getDebugProcess().getEvaluator();

        if (stackFrame != null && evaluator != null) {
            EvaluationResult deferredResult = evaluateStreamExpression(evaluator, chain, streamTraceExpression, stackFrame);

            if (deferredResult.error == null) {
                XValue xValue = deferredResult.xValue;
                if (xValue == null) {
                    return Result.Unknown.INSTANCE;
                }

                XValueInterpreter.Result result = null;
                try {
                    result = myXValueInterpreter.extract(mySession, xValue).get();
                }
                catch (Exception e) {
                    return new Result.EvaluationFailed(streamTraceExpression, e.getMessage());
                }
                if (result instanceof XValueInterpreter.Result.Array arrayResult) {
                    TracingResult interpretedResult;
                    try {
                        interpretedResult = myResultInterpreter.interpret(chain, arrayResult.getArrayReference(), arrayResult.getHasInnerExceptions());
                    }
                    catch (Throwable t) {
                        return new Result.EvaluationFailed(
                            streamTraceExpression,
                            XDebuggerLocalize.streamDebuggerEvaluationFailedCannotInterpretResult(t.getMessage()).get()
                        );
                    }
                    return new Result.Evaluated(interpretedResult, arrayResult.getEvaluationContext());
                }
                else if (result instanceof XValueInterpreter.Result.Error errorResult) {
                    return new Result.EvaluationFailed(streamTraceExpression, errorResult.getMessage());
                }
                else if (result instanceof XValueInterpreter.Result.Unknown) {
                    return new Result.EvaluationFailed(
                        streamTraceExpression,
                        XDebuggerLocalize.streamDebuggerEvaluationFailed(XDebuggerLocalize.streamDebuggerEvaluationFailedUnknownType()).get()
                    );
                }
            }
            else {
                return new Result.CompilationFailed(streamTraceExpression, deferredResult.error);
            }
        }

        return Result.Unknown.INSTANCE;
    }

    public static class EvaluationResult {
        @Nullable
        public final XValue xValue;
        @Nullable

        public final String error;

        public EvaluationResult(@Nullable XValue xValue, @Nullable String error) {
            this.xValue = xValue;
            this.error = error;
        }
    }

    @Nonnull
    private EvaluationResult evaluateStreamExpression(
        @Nonnull XDebuggerEvaluator evaluator,
        @Nonnull StreamChain chain,
        @Nonnull String streamTraceExpression,
        @Nonnull XStackFrame stackFrame
    ) {
        CompletableFuture<EvaluationResult> deferred = new CompletableFuture<>();

        evaluator.evaluate(myExpressionBuilder.createXExpression(chain, streamTraceExpression), new XEvaluationCallbackBase() {
            @Override
            public void evaluated(@Nonnull XValue evaluationResult) {
                deferred.complete(new EvaluationResult(evaluationResult, null));
            }

            @Override
            public void errorOccurred(@Nonnull LocalizeValue errorMessage) {
                deferred.complete(new EvaluationResult(null, errorMessage.get()));
            }
        }, stackFrame.getSourcePosition());

        try {
            return deferred.get();
        }
        catch (Exception e) {
            return new EvaluationResult(null, e.getMessage());
        }
    }
}
