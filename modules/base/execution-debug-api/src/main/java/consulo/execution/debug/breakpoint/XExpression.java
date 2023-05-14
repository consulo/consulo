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
package consulo.execution.debug.breakpoint;

import consulo.execution.debug.evaluation.EvaluationMode;
import consulo.execution.debug.internal.breakpoint.XExpressionImpl;
import consulo.language.Language;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author egor
 */
public interface XExpression {
  XExpression EMPTY_EXPRESSION = fromText("", EvaluationMode.EXPRESSION);
  XExpression EMPTY_CODE_FRAGMENT = fromText("", EvaluationMode.CODE_FRAGMENT);

  @Nullable
  public static XExpression fromText(@Nullable String text) {
    return text != null ? new XExpressionImpl(text, null, null, EvaluationMode.EXPRESSION) : null;
  }

  @Nullable
  public static XExpression fromText(@Nullable String text, EvaluationMode mode) {
    return text != null ? new XExpressionImpl(text, null, null, mode) : null;
  }

  @Nonnull
  public static XExpression changeMode(XExpression expression, EvaluationMode mode) {
    return new XExpressionImpl(expression.getExpression(), expression.getLanguage(), expression.getCustomInfo(), mode);
  }

  @Nonnull
  String getExpression();

  @Nullable
  Language getLanguage();

  @Nullable
  String getCustomInfo();

  EvaluationMode getMode();
}
