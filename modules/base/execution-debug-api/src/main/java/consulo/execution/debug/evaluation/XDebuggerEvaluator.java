/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.execution.debug.evaluation;

import consulo.document.Document;
import consulo.project.Project;
import consulo.document.util.TextRange;
import consulo.language.psi.PsiFile;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.frame.XValue;
import consulo.execution.debug.frame.XValueCallback;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author nik
 */
public abstract class XDebuggerEvaluator {
  /**
   * Start evaluating expression.
   *
   * @param expression expression to evaluate
   * @param callback   used to notify that the expression has been evaluated or an error occurs
   */
  public abstract void evaluate(@Nonnull String expression, @Nonnull XEvaluationCallback callback, @Nullable XSourcePosition expressionPosition);

  /**
   * Start evaluating expression.
   *
   * @param expression         expression to evaluate
   * @param callback           used to notify that the expression has been evaluated or an error occurs
   * @param expressionPosition position where this expression should be evaluated
   */
  public void evaluate(@Nonnull XExpression expression, @Nonnull XEvaluationCallback callback, @Nullable XSourcePosition expressionPosition) {
    evaluate(expression.getExpression(), callback, expressionPosition);
  }

  /**
   * If this method returns {@code true} 'Code Fragment Mode' button will be shown in 'Evaluate' dialog allowing user to execute a set of
   * statements
   *
   * @return {@code true} if debugger supports evaluation of code fragments (statements)
   */
  public boolean isCodeFragmentEvaluationSupported() {
    return true;
  }

  /**
   * Return text range of expression which can be evaluated.
   *
   * @param project            project
   * @param document           document
   * @param offset             offset
   * @param sideEffectsAllowed if this parameter is false, the expression should not have any side effects when evaluated
   *                           (such expressions are evaluated in quick popups)
   * @return text range of expression
   */
  @Nullable
  public TextRange getExpressionRangeAtOffset(Project project, Document document, int offset, boolean sideEffectsAllowed) {
    return null;
  }

  /**
   * @param project            project
   * @param document           document
   * @param offset             offset
   * @param sideEffectsAllowed if this parameter is false, the expression should not have any side effects when evaluated
   *                           (such expressions are evaluated in quick popups)
   * @return {@link ExpressionInfo} of expression which can be evaluated
   */
  @Nullable
  public ExpressionInfo getExpressionInfoAtOffset(@Nonnull Project project, @Nonnull Document document, int offset, boolean sideEffectsAllowed) {
    TextRange range = getExpressionRangeAtOffset(project, document, offset, sideEffectsAllowed);
    return range == null ? null : new ExpressionInfo(range);
  }

  /**
   * Override this method to format selected text before it is shown in 'Evaluate' dialog
   */
  @Nonnull
  public String formatTextForEvaluation(@Nonnull String text) {
    return text;
  }

  /**
   * Returns mode which should be used to evaluate the text
   */
  public EvaluationMode getEvaluationMode(@Nonnull String text, int startOffset, int endOffset, @Nullable PsiFile psiFile) {
    return text.contains("\n") ? EvaluationMode.CODE_FRAGMENT : EvaluationMode.EXPRESSION;
  }

  public interface XEvaluationCallback extends XValueCallback {
    void evaluated(@Nonnull XValue result);
  }
}