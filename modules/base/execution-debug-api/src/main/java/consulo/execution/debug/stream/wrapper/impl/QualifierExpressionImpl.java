// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.execution.debug.stream.wrapper.impl;

import consulo.execution.debug.stream.trace.impl.handler.type.GenericType;
import consulo.execution.debug.stream.wrapper.QualifierExpression;

import consulo.document.util.TextRange;

/**
 * @author Vitaliy.Bibaev
 */
public class QualifierExpressionImpl implements QualifierExpression {
  private final String text;
  private final TextRange textRange;
  private final GenericType typeAfter;

  public QualifierExpressionImpl(String text, TextRange textRange, GenericType typeAfter) {
    this.text = text;
    this.textRange = textRange;
    this.typeAfter = typeAfter;
  }

  @Override
 
  public String getText() {
    return text;
  }

  @Override
  public TextRange getTextRange() {
    return textRange;
  }

  @Override
  public GenericType getTypeAfter() {
    return typeAfter;
  }
}
