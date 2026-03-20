/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.execution.debug.frame;

import consulo.execution.debug.XDebuggerUtil;
import consulo.execution.debug.XSourcePosition;
import consulo.execution.debug.breakpoint.XExpression;
import consulo.execution.debug.evaluation.EvaluationMode;
import consulo.execution.debug.evaluation.XInstanceEvaluator;
import consulo.ui.image.Image;
import consulo.util.concurrent.AsyncResult;
import consulo.util.lang.ThreeState;

import org.jspecify.annotations.Nullable;

/**
 * Represents a value in debugger tree.
 * Override {@link XValueContainer#computeChildren} if value has a properties which should be shown as child nodes
 *
 * @author nik
 */
public abstract class XValue extends XValueContainer {
  /**
   * Start computing presentation of the value in the debugger tree and call {@link XValueNode#setPresentation(Image, String, String, boolean)}
   * when computation is finished.
   * Note that this method is called from the Event Dispatch thread so it should return quickly.
   * @param node node
   * @param place where the node will be shown.
   */
  public abstract void computePresentation(XValueNode node, XValuePlace place);

  /**
   * @return expression which evaluates to the current value
   */
  public @Nullable String getEvaluationExpression() {
    return null;
  }

  /**
   * Asynchronously calculates expression which evaluates to the current value
   */
  public AsyncResult<XExpression> calculateEvaluationExpression() {
    String expression = getEvaluationExpression();
    XExpression res =
            expression != null ? XDebuggerUtil.getInstance().createExpression(expression, null, null, EvaluationMode.EXPRESSION) : null;
    return AsyncResult.done(res);
  }

  /**
   * @return evaluator to calculate value of the current object instance
   */
  public @Nullable XInstanceEvaluator getInstanceEvaluator() {
    return null;
  }

  /**
   * @return {@link XValueModifier} instance which can be used to modify the value
   */
  public @Nullable XValueModifier getModifier() {
    return null;
  }

  /**
   * Start computing source position of the value and call {@link XNavigatable#setSourcePosition(XSourcePosition)}
   * when computation is finished.
   * Note that this method is called from the Event Dispatch thread so it should return quickly.
   * @param navigatable navigatable
   */
  public void computeSourcePosition(XNavigatable navigatable) {
    navigatable.setSourcePosition(null);
  }

  /**
   * Provide inline debugger data, return ability to provide, use
   * {@link ThreeState#UNSURE} if unsupported (default platform implementation will be used),
   * {@link ThreeState#YES} if applicable
   * {@link ThreeState#NO} if not applicable
   */
  public ThreeState computeInlineDebuggerData(XInlineDebuggerDataCallback callback) {
    return ThreeState.UNSURE;
  }

  /**
   * Return {@code true} from this method and override {@link #computeSourcePosition(XNavigatable)} if navigation to the source
   * is supported for the value
   * @return {@code true} if navigation to the value's source is supported
   */
  public boolean canNavigateToSource() {
    // should be false, but cannot be due to compatibility reasons
    return true;
  }

  /**
   * Return {@code true} from this method and override {@link #computeTypeSourcePosition(XNavigatable)} if navigation to the value's type
   * is supported for the value
   * @return {@code true} if navigation to the value's type is supported
   */
  public boolean canNavigateToTypeSource() {
    return false;
  }

  /**
   * Start computing source position of the value's type and call {@link XNavigatable#setSourcePosition(XSourcePosition)}
   * when computation is finished.
   * Note that this method is called from the Event Dispatch thread so it should return quickly.
   */
  public void computeTypeSourcePosition(XNavigatable navigatable) {
    navigatable.setSourcePosition(null);
  }

  /**
   * This enables showing referrers for the value
   *
   * @return provider that creates an XValue returning objects that refer to the current value
   * or null if showing referrers for the value is disabled
   */
  public @Nullable XReferrersProvider getReferrersProvider() {
    return null;
  }
}