// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl;

import consulo.execution.debug.stream.trace.impl.handler.type.MapType;

/**
 * @author Vitaliy.Bibaev
 */
public interface MapVariable extends Variable {
  @Override
  
  MapType getType();

  
  Expression get(Expression key);

  
  Expression set(Expression key, Expression newValue);

  
  Expression contains(Expression key);

  
  Expression size();

  
  Expression keys();

  
  CodeBlock computeIfAbsent(Dsl dsl, Expression key, Expression valueIfAbsent, Variable target);

  
  default VariableDeclaration defaultDeclaration() {
    return defaultDeclaration(true);
  }

  
  VariableDeclaration defaultDeclaration(boolean isMutable);

  
  Expression entries();

  
  CodeBlock convertToArray(Dsl dsl, String arrayName);
}
