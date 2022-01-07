/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package com.intellij.openapi.actionSystem;

import consulo.annotation.DeprecationInfo;
import consulo.awt.TargetAWT;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.image.Image;
import consulo.ui.Size;
import org.intellij.lang.annotations.MagicConstant;

import javax.annotation.Nonnull;
import java.util.List;

/**
 * Represents a toolbar with a visual presentation.
 *
 * @see ActionManager#createActionToolbar(String, ActionGroup, boolean)
 */
public interface ActionToolbar {
  String ACTION_TOOLBAR_PROPERTY_KEY = "ACTION_TOOLBAR";

  /**
   * This is default layout policy for the toolbar. It defines that
   * all toolbar component are in one row / column and they are not wrapped
   * when toolbar is small
   */
  int NOWRAP_LAYOUT_POLICY = 0;
  /**
   * This is experimental layout policy which allow toolbar to
   * wrap components in multiple rows.
   */
  int WRAP_LAYOUT_POLICY = 1;
  /**
   * This is experimental layout policy which allow toolbar auto-hide and show buttons that don't fit into actual side
   */
  int AUTO_LAYOUT_POLICY = 2;

  /**
   * Horizontal orientation. Used for scrollbars and sliders.
   */
  int HORIZONTAL_ORIENTATION = 0;
  /**
   * Vertical orientation. Used for scrollbars and sliders.
   */
  int VERTICAL_ORIENTATION = 1;

  /**
   * This is default minimum size of the toolbar button, without scaling
   */
  Size DEFAULT_MINIMUM_BUTTON_SIZE = new Size(25, 25);

  Size NAVBAR_MINIMUM_BUTTON_SIZE = new Size(20, 20);

  /**
   * Constraint that's passed to <code>Container.add</code> when ActionButton is added to the toolbar.
   */
  String ACTION_BUTTON_CONSTRAINT = "Constraint.ActionButton";

  /**
   * Constraint that's passed to <code>Container.add</code> when a custom component is added to the toolbar.
   */
  String CUSTOM_COMPONENT_CONSTRAINT = "Constraint.CustomComponent";

  /**
   * Constraint that's passed to <code>Container.add</code> when a Separator is added to the toolbar.
   */
  String SEPARATOR_CONSTRAINT = "Constraint.Separator";

  /**
   * Constraint that's passed to <code>Container.add</code> when a secondary action is added to the toolbar.
   */
  String SECONDARY_ACTION_CONSTRAINT = "Constraint.SecondaryAction";

  /**
   * @return component which represents the tool bar on UI
   */
  @Nonnull
  default javax.swing.JComponent getComponent() {
    return (javax.swing.JComponent)TargetAWT.to(getUIComponent());
  }

  /**
   * @return component which represents the tool bar on UI
   */
  @Nonnull
  default Component getUIComponent() {
    throw new AbstractMethodError();
  }

  /**
   * @return current layout policy
   * @see #NOWRAP_LAYOUT_POLICY
   * @see #WRAP_LAYOUT_POLICY
   */
  int getLayoutPolicy();

  /**
   * Sets new component layout policy. Method accepts {@link #WRAP_LAYOUT_POLICY} and
   * {@link #NOWRAP_LAYOUT_POLICY} values.
   */
  void setLayoutPolicy(int layoutPolicy);

  /**
   * If the valus is <code>true</code> then the all button on toolbar are
   * the same size. It very useful when you create "Outlook" like toolbar.
   * Currently this method can be considered as hot fix.
   */
  void adjustTheSameSize(boolean value);

  /**
   * Sets minimum size of toolbar button. By default all buttons
   * at toolbar has 25x25 pixels size.
   *
   * @throws IllegalArgumentException if <code>size</code>
   *                                  is <code>null</code>
   */
  @Deprecated
  @DeprecationInfo("Use with Size parameter")
  default void setMinimumButtonSize(@Nonnull java.awt.Dimension size) {
    setMinimumButtonSize(new Size(size.width, size.height));
  }

  /**
   * Sets minimum size of toolbar button. By default all buttons
   * at toolbar has 25x25 pixels size.
   *
   * @throws IllegalArgumentException if <code>size</code>
   *                                  is <code>null</code>
   */
  void setMinimumButtonSize(@Nonnull Size size);

  /**
   * Sets toolbar orientation
   *
   * @see #HORIZONTAL_ORIENTATION
   * @see #VERTICAL_ORIENTATION
   */
  void setOrientation(@MagicConstant(intValues = {HORIZONTAL_ORIENTATION, VERTICAL_ORIENTATION}) int orientation);

  /**
   * @return maximum button height
   */
  int getMaxButtonHeight();

  /**
   * Forces update of the all actions in the toolbars. Actions, however, normally updated automatially every 500msec.
   */
  @RequiredUIAccess
  void updateActionsImmediately();

  boolean hasVisibleActions();

  /**
   * @param component will be used for datacontext computations
   */
  default void setTargetComponent(final javax.swing.JComponent component) {
    throw new AbstractMethodError();
  }

  default void setTargetUIComponent(@Nonnull Component component) {
    setTargetComponent((javax.swing.JComponent)TargetAWT.to(component));
  }

  void setReservePlaceAutoPopupIcon(final boolean reserve);

  void setSecondaryActionsTooltip(String secondaryActionsTooltip);

  void setMiniMode(boolean minimalMode);

  DataContext getToolbarDataContext();

  @Nonnull
  List<AnAction> getActions();

  default void setSecondaryActionsIcon(Image icon) {
  }

  default void setSecondaryActionsIcon(Image icon, boolean hideDropdownIcon) {
  }

  /**
   * Enables showing titles of separators as labels in the toolbar (off by default).
   */
  default void setShowSeparatorTitles(boolean showSeparatorTitles) {
  }
}
