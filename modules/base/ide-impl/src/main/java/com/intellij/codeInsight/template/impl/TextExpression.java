// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.codeInsight.template.impl;

import consulo.language.editor.completion.lookup.LookupElement;
import consulo.language.editor.template.Expression;
import consulo.language.editor.template.ExpressionContext;
import consulo.language.editor.template.Result;
import consulo.language.editor.template.TextResult;


public class TextExpression extends Expression {
  private final String myString;

  public TextExpression(String string) {
    myString = string;
  }

  @Override
  public Result calculateResult(ExpressionContext expressionContext) {
    return new TextResult(myString);
  }

  @Override
  public boolean requiresCommittedPSI() {
    return false;
  }

  @Override
  public LookupElement[] calculateLookupItems(ExpressionContext expressionContext) {
    return LookupElement.EMPTY_ARRAY;
  }
}
