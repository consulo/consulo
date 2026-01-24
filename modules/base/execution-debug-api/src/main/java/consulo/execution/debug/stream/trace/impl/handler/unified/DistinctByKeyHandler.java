package consulo.execution.debug.stream.trace.impl.handler.unified;

import consulo.document.util.TextRange;
import consulo.execution.debug.stream.trace.dsl.*;
import consulo.execution.debug.stream.trace.dsl.impl.TextExpression;
import consulo.execution.debug.stream.trace.impl.handler.type.ClassTypeImpl;
import consulo.execution.debug.stream.trace.impl.handler.type.GenericType;
import consulo.execution.debug.stream.wrapper.CallArgument;
import consulo.execution.debug.stream.wrapper.IntermediateStreamCall;
import consulo.execution.debug.stream.wrapper.impl.CallArgumentImpl;
import consulo.execution.debug.stream.wrapper.impl.IntermediateStreamCallImpl;
import jakarta.annotation.Nonnull;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class DistinctByKeyHandler extends HandlerBase.Intermediate {
    protected static final String KEY_EXTRACTOR_VARIABLE_PREFIX = "keyExtractor";
    protected static final String TRANSITIONS_ARRAY_NAME = "transitionsArray";

    private final IntermediateStreamCall myCall;
    private final String functionApplyName;
    protected final int keyExtractorPosition;
    protected final GenericType myKeyType;
    protected final GenericType afterValueType;

    protected final PeekTraceHandler myPeekHandler;
    protected final CallArgument myKeyExtractor;
    protected final GenericType myTypeAfter;
    protected final Variable myExtractorVariable;
    protected final ListVariable myBeforeTimes;
    protected final ListVariable myBeforeValues;
    protected final ListVariable myKeys;
    protected final MapVariable myTime2ValueAfter;

    public DistinctByKeyHandler(int callNumber,
                                IntermediateStreamCall call,
                                Dsl dsl,
                                String functionApplyName,
                                int keyExtractorPosition,
                                GenericType keyType,
                                GenericType afterValueType) {
        super(dsl);
        this.myCall = call;
        this.functionApplyName = functionApplyName;
        this.keyExtractorPosition = keyExtractorPosition;
        this.myKeyType = keyType;
        this.afterValueType = afterValueType;
        this.myPeekHandler = new PeekTraceHandler(callNumber, "distinct", call.getTypeBefore(), call.getTypeAfter(), dsl);
        this.myTypeAfter = call.getTypeAfter();
        this.myBeforeTimes = dsl.list(dsl.getTypes().INT(), call.getName() + callNumber + "BeforeTimes");
        this.myBeforeValues = dsl.list(dsl.getTypes().ANY(), call.getName() + callNumber + "BeforeValues");
        this.myKeys = dsl.list(keyType, call.getName() + callNumber + "Keys");
        this.myTime2ValueAfter = dsl.linkedMap(dsl.getTypes().INT(), afterValueType, call.getName() + callNumber + "After");

        var arguments = call.getArguments();
        assert !arguments.isEmpty() : "Key extractor is not specified";
        this.myKeyExtractor = arguments.get(keyExtractorPosition);
        this.myExtractorVariable = dsl.variable(new ClassTypeImpl(myKeyExtractor.getType()), KEY_EXTRACTOR_VARIABLE_PREFIX + callNumber);
    }

    public DistinctByKeyHandler(int callNumber,
                                IntermediateStreamCall call,
                                Dsl dsl) {
        this(callNumber, call, dsl, "apply", 0, dsl.getTypes().ANY(), dsl.getTypes().ANY());
    }

    @Nonnull
    @Override
    public List<VariableDeclaration> additionalVariablesDeclaration() {
        var extractor = dsl.declaration(myExtractorVariable, new TextExpression(myKeyExtractor.getText()), false);
        var variables = new ArrayList<VariableDeclaration>();
        variables.add(extractor);
        variables.add(myBeforeTimes.defaultDeclaration());
        variables.add(myBeforeValues.defaultDeclaration());
        variables.add(myTime2ValueAfter.defaultDeclaration());
        variables.add(myKeys.defaultDeclaration());
        variables.addAll(myPeekHandler.additionalVariablesDeclaration());

        return variables;
    }

    @Nonnull
    @Override
    public IntermediateStreamCall transformCall(IntermediateStreamCall call) {
        Lambda newKeyExtractor = dsl.lambda("x", context -> {
            var key = dsl.variable(myKeyType, "key");
            context.declare(key, myExtractorVariable.call(functionApplyName, context.getLambdaArg()), false);
            context.statement(() -> myBeforeTimes.add(dsl.currentTime()));
            context.statement(() -> myBeforeValues.add(context.getLambdaArg()));
            context.statement(() -> myKeys.add(key));
            context.doReturn(key);
        });

        var newArgs = new ArrayList<>(call.getArguments());
        if (keyExtractorPosition < newArgs.size()) {
            newArgs.set(keyExtractorPosition, new CallArgumentImpl(myKeyExtractor.getType(), newKeyExtractor.toCode()));
        }
        else {
            newArgs.add(keyExtractorPosition, new CallArgumentImpl(myKeyExtractor.getType(), newKeyExtractor.toCode()));
        }

        return updateArguments(call, newArgs);
    }

    protected Expression[] getMapArguments() {
        return new Expression[0];
    }

    @Nonnull
    @Override
    public CodeBlock prepareResult() {
        var keys2TimesBefore = dsl.map(myKeyType, dsl.getTypes().list(dsl.getTypes().INT()), "keys2Times", getMapArguments());
        var transitions = dsl.map(dsl.getTypes().INT(), dsl.getTypes().INT(), "transitionsMap");
        var nullKeyList = dsl.list(dsl.getTypes().INT(), "nullKeyList");

        return dsl.block(block -> {
            block.add(myPeekHandler.prepareResult());
            block.declare(keys2TimesBefore.defaultDeclaration());
            block.declare(transitions.defaultDeclaration());
            block.declare(nullKeyList.defaultDeclaration());

            integerIteration(myKeys.size(), block, loop -> {
                var key = loop.declare(dsl.variable(myKeyType, "key"), myKeys.get(loop.getLoopVariable()), false);
                var lst = dsl.list(dsl.getTypes().INT(), "lst");
                loop.declare(lst, true);
                loop.ifBranch(dsl.same(key, dsl.getNullExpression()), ifContext -> {
                    ifContext.assign(lst, nullKeyList);
                }).elseBranch(elseContext -> {
                    elseContext.add(keys2TimesBefore.computeIfAbsent(dsl, key, dsl.newList(dsl.getTypes().INT()), lst));
                });
                loop.statement(() -> lst.add(myBeforeTimes.get(loop.getLoopVariable())));
            });

            block.forEachLoop(block.variable(block.getTypes().INT(), "afterTime"), myTime2ValueAfter.keys(), loop -> {
                var afterTime = loop.getLoopVariable();
                var valueAfter = loop.declare(dsl.variable(dsl.getTypes().ANY(), "valueAfter"), myTime2ValueAfter.get(loop.getLoopVariable()), false);
                var key = loop.declare(dsl.variable(myKeyType, "key"), new TextExpression(myKeyType.getDefaultValue()), true);
                var found = loop.declare(dsl.variable(dsl.getTypes().BOOLEAN(), "found"), new TextExpression("false"), true);
                
                integerIteration(myBeforeTimes.size(), block, innerLoop -> {
                    var equals = dsl.equals(valueAfter, myBeforeValues.get(innerLoop.getLoopVariable()));
                    innerLoop.ifBranch(dsl.and(equals, dsl.not(transitions.contains(myBeforeTimes.get(innerLoop.getLoopVariable())))), ifContext -> {
                        ifContext.assign(key, myKeys.get(innerLoop.getLoopVariable()));
                        ifContext.assign(found, new TextExpression("true"));
                        ifContext.statement(innerLoop::breakIteration);
                    });
                });

                loop.ifBranch(found, ifContext -> {
                    var key2TimesBeforeList = dsl.list(dsl.getTypes().INT(), "key2TimesBeforeList");
                    ifContext.declare(key2TimesBeforeList, true);
                    ifContext.ifBranch(dsl.same(key, dsl.getNullExpression()), innerIf -> {
                        innerIf.assign(key2TimesBeforeList, nullKeyList);
                    }).elseBranch(elseContext -> {
                        elseContext.assign(key2TimesBeforeList, keys2TimesBefore.get(key));
                    });
                    
                    ifContext.forEachLoop(dsl.variable(dsl.getTypes().INT(), "beforeTime"), key2TimesBeforeList, innerLoop -> {
                        innerLoop.statement(() -> transitions.set(innerLoop.getLoopVariable(), afterTime));
                    });
                });
            });

            block.add(transitions.convertToArray(block, "transitionsArray"));
        });
    }

    @Nonnull
    @Override
    public Expression getResultExpression() {
        return dsl.newArray(dsl.getTypes().ANY(), myPeekHandler.getResultExpression(), new TextExpression(TRANSITIONS_ARRAY_NAME));
    }

    @Nonnull
    @Override
    public List<IntermediateStreamCall> additionalCallsBefore() {
        return myPeekHandler.additionalCallsBefore();
    }

    @Nonnull
    @Override
    public List<IntermediateStreamCall> additionalCallsAfter() {
        var callsAfter = new ArrayList<>(myPeekHandler.additionalCallsAfter());
        Lambda lambda = dsl.lambda("x", context -> {
            context.doReturn(myTime2ValueAfter.set(dsl.currentTime(), context.getLambdaArg()));
        });

        callsAfter.add(dsl.createPeekCall(myTypeAfter, lambda));
        return callsAfter;
    }

    protected void integerIteration(Expression border, CodeContext context, Consumer<ForLoopBody> init) {
        context.forLoop(context.declaration(context.variable(context.getTypes().INT(), "i"), new TextExpression("0"), true),
            new TextExpression("i < " + border.toCode()),
            new TextExpression("i = i + 1"), init);
    }

    protected IntermediateStreamCall updateArguments(IntermediateStreamCall call, List<CallArgument> args) {
        return new IntermediateStreamCallImpl(call.getName(), call.getGenericArguments(), args, call.getTypeBefore(), call.getTypeAfter(), call.getTextRange());
    }

    public static class DistinctByCustomKey extends DistinctByKeyHandler {
        public DistinctByCustomKey(int callNumber,
                                   IntermediateStreamCall call,
                                   String extractorType,
                                   String extractorExpression,
                                   Dsl dsl) {
            super(callNumber, transform(call, extractorType, extractorExpression), dsl);
        }

        private static IntermediateStreamCall transform(IntermediateStreamCall call, String extractorType, String extractorExpression) {
            return new IntermediateStreamCallImpl("distinct", call.getGenericArguments(),
                List.of(new CallArgumentImpl(extractorType, extractorExpression)),
                call.getTypeBefore(), call.getTypeAfter(),
                TextRange.EMPTY_RANGE);
        }
    }
}
