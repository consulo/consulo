// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl.impl.common;

import consulo.execution.debug.stream.trace.dsl.ArrayVariable;
import consulo.execution.debug.stream.trace.dsl.CodeBlock;
import consulo.execution.debug.stream.trace.dsl.Dsl;
import consulo.execution.debug.stream.trace.dsl.MapVariable;
import consulo.execution.debug.stream.trace.dsl.Variable;
import consulo.execution.debug.stream.trace.dsl.impl.TextExpression;
import consulo.execution.debug.stream.trace.dsl.impl.VariableImpl;
import consulo.execution.debug.stream.trace.impl.handler.type.MapType;

/**
 * @author Vitaliy.Bibaev
 */
public abstract class MapVariableBase extends VariableImpl implements MapVariable {
  private final MapType type;
  private final String name;

  public MapVariableBase(MapType type, String name) {
    super(type, name);
    this.type = type;
    this.name = name;
  }

  @Override
  public MapType getType() {
    return type;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public CodeBlock convertToArray(Dsl dsl, String arrayName) {
    ArrayVariable resultArray = dsl.array(dsl.getTypes().ANY(), arrayName);
    Variable size = dsl.variable(dsl.getTypes().INT(), "size");
    ArrayVariable keys = dsl.array(type.getKeyType(), "keys");
    ArrayVariable values = dsl.array(type.getValueType(), "values");
    Variable i = dsl.variable(dsl.getTypes().INT(), "i");
    Variable key = dsl.variable(type.getKeyType(), "key");

    return dsl.block(block -> {
      block.declare(resultArray, dsl.newSizedArray(dsl.getTypes().ANY(), 0), true);
      block.scope(scope -> {
        scope.declare(size, size(), false);
        scope.declare(keys.defaultDeclaration(size));
        scope.declare(values.defaultDeclaration(size));
        scope.declare(i, new TextExpression("0"), true);
        scope.forEachLoop(key, keys(), loop -> {
          loop.statement(() -> keys.set(i, loop.getLoopVariable()));
          loop.statement(() -> values.set(i, get(loop.getLoopVariable())));
          loop.statement(() -> new TextExpression(i.toCode() + "++"));
        });

        scope.assign(resultArray, dsl.newArray(dsl.getTypes().ANY(), keys, values));
      });
    });
  }
}
