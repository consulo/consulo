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
package com.intellij.xdebugger.frame;

import com.intellij.openapi.util.AsyncResult;
import com.intellij.util.ThreeState;
import com.intellij.xdebugger.XDebuggerUtil;
import com.intellij.xdebugger.XExpression;
import com.intellij.xdebugger.evaluation.EvaluationMode;
import com.intellij.xdebugger.evaluation.XInstanceEvaluator;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
  public abstract void computePresentation(@Nonnull XValueNode node, @Nonnull XValuePlace place);

  /**
   * @return expression which evaluates to the current value
   */
  @Nullable
  public String getEvaluationExpression() {
    return null;
  }

  /**
   * Asynchronously calculates expression which evaluates to the current value
   */
  @Nonnull
  public AsyncResult<XExpression> calculateEvaluationExpression() {
    String expression = getEvaluationExpression();
    XExpression res =
            expression != null ? XDebuggerUtil.getInstance().createExpression(expression, null, null, EvaluationMode.EXPRESSION) : null;
    return AsyncResult.done(res);
  }

  /**
   * @return evaluator to calculate value of the current object instance
   */
  @Nullable
  public XInstanceEvaluator getInstanceEvaluator() {
    return null;
  }

  /**
   * @return {@link com.intellij.xdebugger.frame.XValueModifier} instance which can be used to modify the value
   */
  @Nullable
  public XValueModifier getModifier() {
    return null;
  }

  /**
   * Start computing source position of the value and call {@link XNavigatable#setSourcePosition(com.intellij.xdebugger.XSourcePosition)}
   * when computation is finished.
   * Note that this method is called from the Event Dispatch thread so it should return quickly.
   * @param navigatable navigatable
   */
  public void computeSourcePosition(@Nonnull XNavigatable navigatable) {
    navigatable.setSourcePosition(null);
  }

  /**
   * Provide inline debugger data, return ability to provide, use
   * {@link ThreeState#UNSURE} if unsupported (default platform implementation will be used),
   * {@link ThreeState#YES} if applicable
   * {@link ThreeState#NO} if not applicable
   */
  @Nonnull
  public ThreeState computeInlineDebuggerData(@Nonnull XInlineDebuggerDataCallback callback) {
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
   * Start computing source position of the value's type and call {@link XNavigatable#setSourcePosition(com.intellij.xdebugger.XSourcePosition)}
   * when computation is finished.
   * Note that this method is called from the Event Dispatch thread so it should return quickly.
   */
  public void computeTypeSourcePosition(@Nonnull XNavigatable navigatable) {
    navigatable.setSourcePosition(null);
  }

  /**
   * This enables showing referrers for the value
   *
   * @return provider that creates an XValue returning objects that refer to the current value
   * or null if showing referrers for the value is disabled
   */
  @Nullable
  public XReferrersProvider getReferrersProvider() {
    return null;
  }
}