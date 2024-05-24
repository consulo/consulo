// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.editor.template;

import consulo.language.editor.completion.lookup.LookupElement;

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
