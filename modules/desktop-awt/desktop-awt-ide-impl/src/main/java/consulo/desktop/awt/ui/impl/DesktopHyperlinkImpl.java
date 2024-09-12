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
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.ex.awt.LinkLabel;
import consulo.ui.ex.awt.LinkListener;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.image.Image;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 16/07/2021
 */
public class DesktopHyperlinkImpl extends SwingComponentDelegate<DesktopHyperlinkImpl.MyLinkLabel> implements Hyperlink {
  public class MyLinkLabel extends LinkLabel<Object> implements FromSwingComponentWrapper {
    public MyLinkLabel(String text, @Nullable Image icon, @Nullable LinkListener<Object> aListener) {
      super(text, icon, aListener);
    }

    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopHyperlinkImpl.this;
    }
  }

  public DesktopHyperlinkImpl(String text) {
    MyLinkLabel label = new MyLinkLabel(text, null, (aSource, aLinkData) -> {
      getListenerDispatcher(HyperlinkEvent.class).onEvent(new HyperlinkEvent(this, ""));
    });
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
  public void setImage(@Nullable Image icon) {
    toAWTComponent().setIcon(TargetAWT.to(icon));
  }

  @Nullable
  @Override
  public Image getImage() {
    return TargetAWT.from(toAWTComponent().getIcon());
  }
}
