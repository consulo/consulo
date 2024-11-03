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

import consulo.application.Application;
import consulo.application.ui.UISettings;
import consulo.application.ui.event.UISettingsListener;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ui.wm.StatusBar;
import consulo.ui.Component;
import consulo.ui.FocusManager;
import consulo.ui.Label;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2023-11-13
 */
public class BaseToolWindowsSwitcher implements Disposable, UISettingsListener {
  private final StatusBar myStatusBar;
  protected JBPopup popup;
  protected boolean wasExited = false;
  protected Label myLabel;

  public BaseToolWindowsSwitcher(StatusBar statusBar) {
    myStatusBar = statusBar;

    myLabel = Label.create();

    Disposer.register(this, FocusManager.get().addListener(this::update));

    Application.get().getMessageBus().connect(this).subscribe(UISettingsListener.class, this);
  }

  @Override
  public void uiSettingsChanged(UISettings uiSettings) {
    update();
  }

  public void performAction() {
    if (isActive()) {
      UISettings.getInstance().setHideToolStripes(!UISettings.getInstance().getHideToolStripes());
      UISettings.getInstance().fireUISettingsChanged();
    }
  }

  @RequiredUIAccess
  public void update() {
    myLabel.setToolTipText(LocalizeValue.empty());
    if (isActive()) {
      boolean changes = false;

      if (!myLabel.isVisible()) {
        myLabel.setVisible(true);
        changes = true;
      }

      Image icon = UISettings.getInstance().getHideToolStripes() ? PlatformIconGroup.generalTbshown() : PlatformIconGroup.generalTbhidden();
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
      myLabel.setToolTipText(LocalizeValue.empty());
    }
  }

  public boolean isActive() {
    return myStatusBar != null && myStatusBar.getProject() != null;
  }

  @Nonnull
  public Component getUIComponent() {
    return myLabel;
  }

  @Override
  public void dispose() {
    Disposer.dispose(this);
    popup = null;
  }
}
