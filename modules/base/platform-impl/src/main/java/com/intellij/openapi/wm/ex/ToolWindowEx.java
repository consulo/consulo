/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package com.intellij.openapi.wm.ex;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.ToolWindowInternalDecorator;
import kava.beans.PropertyChangeListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface ToolWindowEx extends ToolWindow {
  String PROP_AVAILABLE = "available";
  String PROP_ICON = "icon";
  @Deprecated
  String PROP_TITLE = "title";
  @Deprecated
  String PROP_STRIPE_TITLE = "stripe-title";

  void addPropertyChangeListener(final PropertyChangeListener l);

  /**
   * Removes specified property change listener.
   *
   * @param l listener to be removed.
   */
  void removePropertyChangeListener(PropertyChangeListener l);

  /**
   * @return type of internal decoration of tool window.
   * @throws IllegalStateException if tool window isn't installed.
   */
  @RequiredUIAccess
  ToolWindowType getInternalType();

  void stretchWidth(int value);

  void stretchHeight(int value);

  ToolWindowInternalDecorator getDecorator();

  void setAdditionalGearActions(@Nullable ActionGroup additionalGearActions);

  void setTitleActions(@Nonnull AnAction... actions);

  void setTabActions(@Nonnull AnAction... actions);

  void setTabDoubleClickActions(@Nonnull AnAction... actions);

  void setUseLastFocusedOnActivation(boolean focus);

  boolean isUseLastFocusedOnActivation();
}
