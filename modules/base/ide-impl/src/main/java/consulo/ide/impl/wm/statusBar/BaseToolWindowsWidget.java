/*
 * Copyright 2013-2023 consulo.io
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
package consulo.ide.impl.wm.statusBar;

import consulo.application.AllIcons;
import consulo.application.Application;
import consulo.application.ui.UISettings;
import consulo.application.ui.event.UISettingsListener;
import consulo.application.util.registry.Registry;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.ui.wm.CustomStatusBarWidget;
import consulo.project.ui.wm.StatusBar;
import consulo.project.ui.wm.StatusBarWidget;
import consulo.ui.Component;
import consulo.ui.FocusManager;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2023-11-13
 */
public class BaseToolWindowsWidget implements CustomStatusBarWidget, StatusBarWidget, Disposable, UISettingsListener {
  private StatusBar myStatusBar;
  protected JBPopup popup;
  protected boolean wasExited = false;
  protected Label myLabel;

  public BaseToolWindowsWidget() {
    myLabel = Label.create();

    Disposer.register(this, FocusManager.get().addListener(this::updateIcon));

    Application.get().getMessageBus().connect(this).subscribe(UISettingsListener.class, this);
  }

  @Override
  public void uiSettingsChanged(UISettings uiSettings) {
    updateIcon();
  }

  public void performAction() {
    if (isActive()) {
      UISettings.getInstance().setHideToolStripes(!UISettings.getInstance().getHideToolStripes());
      UISettings.getInstance().fireUISettingsChanged();
    }
  }

  @RequiredUIAccess
  private void updateIcon() {
    myLabel.setToolTipText(null);
    if (isActive()) {
      boolean changes = false;

      if (!myLabel.isVisible()) {
        myLabel.setVisible(true);
        changes = true;
      }

      Image icon = UISettings.getInstance().getHideToolStripes() ? AllIcons.General.TbShown : AllIcons.General.TbHidden;
      if (icon != myLabel.getImage()) {
        myLabel.setImage(icon);
        changes = true;
      }

      if (changes) {
        myLabel.forceRepaint();
      }
    }
    else {
      myLabel.setVisible(false);
      myLabel.setToolTipText(null);
    }
  }

  public boolean isActive() {
    return myStatusBar != null && myStatusBar.getProject() != null && Registry.is("ide.windowSystem.showTooWindowButtonsSwitcher");
  }

  @Nullable
  @Override
  public Component getUIComponent() {
    return myLabel;
  }

  @Override
  public boolean isUnified() {
    return true;
  }

  @Nonnull
  @Override
  public String ID() {
    return "ToolWindows Widget";
  }

  @Override
  public WidgetPresentation getPresentation() {
    return null;
  }

  @Override
  public void install(@Nonnull StatusBar statusBar) {
    myStatusBar = statusBar;
    updateIcon();
  }

  @Override
  public void dispose() {
    Disposer.dispose(this);
    myStatusBar = null;
    popup = null;
  }
}
