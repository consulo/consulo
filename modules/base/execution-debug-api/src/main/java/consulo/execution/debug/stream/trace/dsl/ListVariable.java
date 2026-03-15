// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl;

import consulo.execution.debug.stream.trace.dsl.impl.TextExpression;
import consulo.execution.debug.stream.trace.impl.handler.type.ListType;

/**
 * @author Vitaliy.Bibaev
 */
public interface ListVariable extends Variable {
  @Override
  
  ListType getType();

  
  Expression get(Expression index);

  
  default Expression get(int index) {
    return get(new TextExpression(Integer.toString(index)));
  }

  
  Expression set(Expression index, Expression newValue);

  
  default Expression set(int index, Expression newValue) {
    return set(new TextExpression(Integer.toString(index)), newValue);
  }

  
  Expression contains(Expression element);

  
  Expression size();

  
  Expression add(Expression element);

  
  VariableDeclaration defaultDeclaration();
}
