/*
 * Copyright 2013-2022 consulo.io
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
package consulo.ide.impl.fileEditor;

import consulo.fileEditor.EditorNotificationBuilder;
import consulo.fileEditor.internal.EditorNotificationBuilderEx;
import consulo.ide.impl.idea.ui.EditorNotificationPanel;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.color.ColorValue;
import consulo.ui.ex.awt.ClickListener;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import consulo.util.lang.StringUtil;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.event.MouseEvent;

/**
 * @author VISTALL
 * @since 18-Jul-22
 */
public class DesktopAWTNotificationPanel extends EditorNotificationPanel implements EditorNotificationBuilderEx {
  @Nonnull
  @Override
  public EditorNotificationBuilder withText(@Nonnull LocalizeValue text) {
    setText(text.get());
    return this;
  }

  @Nonnull
  @Override
  public EditorNotificationBuilder withIcon(@Nonnull Image image) {
    icon(TargetAWT.to(image));
    return this;
  }

  @Nonnull
  @Override
  public EditorNotificationBuilder withBackgroundColor(@Nonnull ColorValue color) {
    myBackgroundColor = TargetAWT.to(color);
    return this;
  }

  @Nonnull
  @Override
  public EditorNotificationBuilder withAction(@Nonnull LocalizeValue actionText, @Nonnull String actionRefId) {
    createActionLabel(actionText.get(), actionRefId);
    return this;
  }

  @Nonnull
  @Override
  public EditorNotificationBuilder withAction(@Nonnull LocalizeValue actionText, @Nonnull LocalizeValue actionTooltipText, @Nonnull Runnable actionHandler) {
    createActionLabel(actionText.get(), actionHandler).setToolTipText(StringUtil.nullize(actionTooltipText.get()));
    return this;
  }

  @Nonnull
  @Override
  public EditorNotificationBuilder withGearAction(@Nonnull LocalizeValue tooltipText, @Nonnull Image image, @Nonnull Runnable action) {
    myGearLabel.setIcon(TargetAWT.to(image));
    myGearLabel.setToolTipText(StringUtil.nullize(tooltipText.getValue()));
    new ClickListener() {
      @Override
      public boolean onClick(@Nonnull MouseEvent event, int clickCount) {
        action.run();
        return true;
      }
    }.installOn(myGearLabel);
    return this;
  }

  @Nonnull
  @Override
  public Component getUIComponent() {
    throw new UnsupportedOperationException("unsupported platform");
  }

  @Nonnull
  @Override
  public JComponent getComponent() {
    return this;
  }
}
