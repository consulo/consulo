// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl;

import consulo.execution.debug.stream.trace.dsl.impl.TextExpression;

/**
 * @author Vitaliy.Bibaev
 */
public interface Expression extends Statement {
  
  Expression call(String callName, Expression... args);

  
  default Expression property(String propertyName) {
    return new TextExpression(toCode() + "." + propertyName);
  }
}
