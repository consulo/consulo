// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl;

import consulo.execution.debug.stream.trace.dsl.impl.TextExpression;
import consulo.execution.debug.stream.trace.impl.handler.type.ArrayType;

/**
 * @author Vitaliy.Bibaev
 */
public interface ArrayVariable extends Variable {
  @Override
  
  ArrayType getType();

  
  Expression get(Expression index);

  
  default Expression get(int index) {
    return get(new TextExpression(Integer.toString(index)));
  }

  
  Expression set(Expression index, Expression value);

  
  default Expression set(int index, Expression value) {
    return set(new TextExpression(Integer.toString(index)), value);
  }

  
  VariableDeclaration defaultDeclaration(Expression size);
}
