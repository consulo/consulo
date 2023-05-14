/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.execution.debug.internal.breakpoint;

import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.evaluation.EvaluationMode;
import consulo.language.Language;

import jakarta.annotation.Nonnull;

/**
 * @author egor
 */
public class XExpressionImpl implements XExpression {
  @Nonnull
  private final String myExpression;
  private final Language myLanguage;
  private final String myCustomInfo;
  private final EvaluationMode myMode;

  public XExpressionImpl(@Nonnull String expression, Language language, String customInfo) {
    this(expression, language, customInfo, EvaluationMode.EXPRESSION);
  }

  public XExpressionImpl(@Nonnull String expression, Language language, String customInfo, EvaluationMode mode) {
    myExpression = expression;
    myLanguage = language;
    myCustomInfo = customInfo;
    myMode = mode;
  }

  @Nonnull
  @Override
  public String getExpression() {
    return myExpression;
  }

  @Override
  public Language getLanguage() {
    return myLanguage;
  }

  @Override
  public String getCustomInfo() {
    return myCustomInfo;
  }

  @Override
  public EvaluationMode getMode() {
    return myMode;
  }

  @Override
  public String toString() {
    return myExpression;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    XExpressionImpl that = (XExpressionImpl)o;

    if (myCustomInfo != null ? !myCustomInfo.equals(that.myCustomInfo) : that.myCustomInfo != null) return false;
    if (!myExpression.equals(that.myExpression)) return false;
    if (myLanguage != null ? !myLanguage.equals(that.myLanguage) : that.myLanguage != null) return false;
    if (myMode != that.myMode) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myExpression.hashCode();
    result = 31 * result + (myLanguage != null ? myLanguage.hashCode() : 0);
    result = 31 * result + (myCustomInfo != null ? myCustomInfo.hashCode() : 0);
    result = 31 * result + (myMode != null ? myMode.hashCode() : 0);
    return result;
  }
}
