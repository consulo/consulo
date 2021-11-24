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
package consulo.ui.desktop.internal;

import com.intellij.ui.components.labels.LinkLabel;
import com.intellij.ui.components.labels.LinkListener;
import consulo.awt.TargetAWT;
import consulo.awt.impl.FromSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.Hyperlink;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;
import consulo.ui.event.HyperlinkEvent;
import consulo.ui.event.HyperlinkListener;
import consulo.ui.image.Image;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

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
      getListenerDispatcher(HyperlinkListener.class).navigate(new HyperlinkEvent(this, ""));
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
