/*
 * Copyright 2013-2021 consulo.io
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
package consulo.desktop.awt.ui.impl;

import consulo.desktop.awt.facade.FromSwingComponentWrapper;
import consulo.desktop.awt.ui.impl.base.SwingComponentDelegate;
import consulo.localize.LocalizeValue;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.ex.awt.LocalizeAction;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jdesktop.swingx.JXHyperlink;

import java.awt.event.ActionEvent;

/**
 * @author VISTALL
 * @since 16/07/2021
 */
public class DesktopHyperlinkImpl extends SwingComponentDelegate<DesktopHyperlinkImpl.MyLinkLabel> implements Hyperlink {
  public class MyLinkLabel extends JXHyperlink implements FromSwingComponentWrapper {
    public MyLinkLabel(String text, @Nullable Image icon) {
      super(new LocalizeAction(LocalizeValue.of(text)) {
          @Override
          public void actionPerformed(ActionEvent e) {
              getListenerDispatcher(HyperlinkEvent.class).onEvent(new HyperlinkEvent(DesktopHyperlinkImpl.this, ""));
          }
      });

      setFocusPainted(false);
      setIcon(TargetAWT.to(icon));
    }

    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopHyperlinkImpl.this;
    }
  }

  public DesktopHyperlinkImpl(String text) {
    MyLinkLabel label = new MyLinkLabel(text, null);
    initialize(label);
  }

  @Nonnull
  @Override
  public String getText() {
    return toAWTComponent().getText();
  }

  @RequiredUIAccess
  @Override
  public void setText(@Nonnull String text) {
    toAWTComponent().setText(text);
  }

  @Override
  public void setIcon(@Nullable Image icon) {
    toAWTComponent().setIcon(TargetAWT.to(icon));
  }

  @Nullable
  @Override
  public Image getIcon() {
    return TargetAWT.from(toAWTComponent().getIcon());
  }
}
