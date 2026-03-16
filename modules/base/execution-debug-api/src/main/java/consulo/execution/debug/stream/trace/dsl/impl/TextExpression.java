// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.trace.dsl.impl;

import consulo.execution.debug.stream.trace.dsl.Expression;

/**
 * @author Vitaliy.Bibaev
 */
public class TextExpression implements Expression {
  private final String myText;

  public TextExpression(String text) {
    myText = text;
  }

  
  @Override
  public Expression call(String callName, Expression... args) {
    StringBuilder sb = new StringBuilder();
    sb.append(myText).append(".").append(callName).append("(");
    for (int i = 0; i < args.length; i++) {
      if (i > 0) {
        sb.append(", ");
      }
      sb.append(args[i].toCode());
    }
    sb.append(")");
    return new TextExpression(sb.toString());
  }

  @Override
  public String toString() {
    return toCode(0);
  }

  
  @Override
  public String toCode(int indent) {
    return withIndent(myText, indent);
  }
}
