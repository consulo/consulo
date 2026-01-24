package consulo.execution.debug.stream.trace.impl.handler.unified;

import consulo.execution.debug.stream.trace.dsl.CodeBlock;
import consulo.execution.debug.stream.trace.dsl.Dsl;
import consulo.execution.debug.stream.trace.dsl.Expression;
import consulo.execution.debug.stream.trace.dsl.VariableDeclaration;
import consulo.execution.debug.stream.trace.dsl.impl.TextExpression;
import consulo.execution.debug.stream.wrapper.IntermediateStreamCall;

import java.util.List;

/**
 * @author Vitaliy.Bibaev
 */
public class DistinctTraceHandler extends HandlerBase.Intermediate {
  private final IntermediateStreamCall myCall;
  private final PeekTraceHandler myPeekTracer;

  public DistinctTraceHandler(int num, IntermediateStreamCall call, Dsl dsl) {
    super(dsl);
    myCall = call;
    myPeekTracer = new PeekTraceHandler(num, "distinct", call.getTypeBefore(), call.getTypeAfter(), dsl);
  }

  @Override
  public List<VariableDeclaration> additionalVariablesDeclaration() {
    return myPeekTracer.additionalVariablesDeclaration();
  }

  @Override
  public CodeBlock prepareResult() {
    return dsl.block(block -> {
      var before = myPeekTracer.getBeforeMap();
      var after = myPeekTracer.getAfterMap();
      var nestedMapType = block.getTypes().map(block.getTypes().INT(), myCall.getTypeBefore());
      var mapping = block.linkedMap(block.getTypes().INT(), block.getTypes().INT(), "mapping");
      block.declare(mapping.defaultDeclaration());
      var eqClasses = block.map(myCall.getTypeBefore(), nestedMapType, "eqClasses");
      block.declare(eqClasses, new TextExpression(eqClasses.getType().getDefaultValue()), false);
      block.forEachLoop(block.variable(block.getTypes().INT(), "beforeTime"), before.keys(), loop -> {
        var beforeValue = loop.declare(dsl.variable(myCall.getTypeBefore(), "beforeValue"), before.get(loop.getLoopVariable()), false);
        var classItems = dsl.map(dsl.getTypes().INT(), myCall.getTypeBefore(), "classItems");
        loop.declare(classItems, true);
        loop.add(eqClasses.computeIfAbsent(dsl, beforeValue, new TextExpression(nestedMapType.getDefaultValue()), classItems));
        loop.statement(() -> classItems.set(loop.getLoopVariable(), beforeValue));
      });

      block.forEachLoop(block.variable(block.getTypes().INT(), "afterTime"), after.keys(), loop -> {
        var afterTime = loop.getLoopVariable();
        var afterValue = loop.declare(dsl.variable(myCall.getTypeAfter(), "afterValue"), after.get(loop.getLoopVariable()), false);
        var classes = dsl.map(dsl.getTypes().INT(), myCall.getTypeBefore(), "classes");
        loop.declare(classes, eqClasses.get(afterValue), false);
        loop.forEachLoop(dsl.variable(dsl.getTypes().INT(), "classElementTime"), classes.keys(), innerLoop -> {
          innerLoop.statement(() -> mapping.set(innerLoop.getLoopVariable(), afterTime));
        });
      });

      block.add(mapping.convertToArray(dsl, "resolve"));
      block.add(myPeekTracer.prepareResult());

      block.declare(block.variable(block.getTypes().ANY(), "peekResult"), myPeekTracer.getResultExpression(), false);
    });
  }

  @Override
  public Expression getResultExpression() {
    return dsl.newArray(dsl.getTypes().ANY(), new TextExpression("peekResult"), new TextExpression("resolve"));
  }

  @Override
  public List<IntermediateStreamCall> additionalCallsBefore() {
    return myPeekTracer.additionalCallsBefore();
  }

  @Override
  public List<IntermediateStreamCall> additionalCallsAfter() {
    return myPeekTracer.additionalCallsAfter();
  }
}
