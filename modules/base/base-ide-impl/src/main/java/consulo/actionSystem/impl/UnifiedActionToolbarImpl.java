/*
 * Copyright 2013-2020 consulo.io
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
package consulo.actionSystem.impl;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DataContext;
import consulo.ui.Component;
import consulo.ui.Size;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.Layout;
import consulo.ui.layout.VerticalLayout;
import org.intellij.lang.annotations.MagicConstant;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.Collections;
import java.util.List;

/**
 * @author VISTALL
 * @since 2020-05-11
 */
public class UnifiedActionToolbarImpl implements ActionToolbar {
  private final String myPlace;

  private final ActionGroup myGroup;

  private int myLayoutPolicy;

  private Layout myComponent;

  private int myOrientation;

  public UnifiedActionToolbarImpl(String place, ActionGroup group, boolean horizontal, boolean decorateButtons) {
    myPlace = place;
    myGroup = group;
    myOrientation = horizontal ? HORIZONTAL_ORIENTATION : VERTICAL_ORIENTATION;

    rebuildUI();
  }

  private void rebuildUI() {
    myComponent = myOrientation == HORIZONTAL_ORIENTATION ? HorizontalLayout.create() : VerticalLayout.create();
  }

  public void setTargetComponent(final javax.swing.JComponent component) {
  }

  @Nonnull
  @Override
  public javax.swing.JComponent getComponent() {
    // FIXME [VISTALL] just stub - not throw on old ui
    return new JPanel();
  }

  @Nonnull
  @Override
  public Component getUIComponent() {
    return myComponent;
  }

  @Override
  public int getLayoutPolicy() {
    return myLayoutPolicy;
  }

  @Override
  public void setLayoutPolicy(int layoutPolicy) {
    myLayoutPolicy = layoutPolicy;
  }

  @Override
  public void adjustTheSameSize(boolean value) {

  }

  @Override
  public void setMinimumButtonSize(@Nonnull Size size) {

  }

  @Override
  public void setOrientation(@MagicConstant(intValues = {HORIZONTAL_ORIENTATION, VERTICAL_ORIENTATION}) int orientation) {
    myOrientation = orientation;

    rebuildUI();
  }

  @Override
  public int getMaxButtonHeight() {
    return 0;
  }

  @RequiredUIAccess
  @Override
  public void updateActionsImmediately() {

  }

  @Override
  public boolean hasVisibleActions() {
    return false;
  }

  @Override
  public void setReservePlaceAutoPopupIcon(boolean reserve) {

  }

  @Override
  public void setSecondaryActionsTooltip(String secondaryActionsTooltip) {

  }

  @Override
  public void setMiniMode(boolean minimalMode) {

  }

  @Override
  public DataContext getToolbarDataContext() {
    return null;
  }

  @Nonnull
  @Override
  public List<AnAction> getActions() {
    return Collections.emptyList();
  }
}
