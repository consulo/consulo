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

import com.intellij.ui.HyperlinkAdapter;
import com.intellij.util.ui.JBHtmlEditorKit;
import consulo.awt.impl.FromSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.HtmlView;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;
import consulo.ui.event.HyperlinkListener;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;

/**
 * @author VISTALL
 * @since 24/11/2021
 */
public class DesktopHtmlViewImpl extends SwingComponentDelegate<DesktopHtmlViewImpl.MyEditorPanel> implements HtmlView {
  public class MyEditorPanel extends JEditorPane implements FromSwingComponentWrapper {
    public MyEditorPanel() {
      super("text/html", "");

      setEditorKit(JBHtmlEditorKit.create());
      setEditable(false);

      addHyperlinkListener(new HyperlinkAdapter() {
        @Override
        protected void hyperlinkActivated(HyperlinkEvent e) {
          getListenerDispatcher(HyperlinkListener.class).navigate(new consulo.ui.event.HyperlinkEvent(toUIComponent(), e.getDescription()));
        }
      });
    }

    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopHtmlViewImpl.this;
    }
  }

  public DesktopHtmlViewImpl() {
    initialize(new MyEditorPanel());
  }

  @Override
  public void setEditable(boolean editable) {
    toAWTComponent().setEditable(editable);
  }

  @Override
  public boolean isEditable() {
    return toAWTComponent().isEditable();
  }

  @Nullable
  @Override
  public String getValue() {
    return toAWTComponent().getText();
  }

  @RequiredUIAccess
  @Override
  public void setValue(String value, boolean fireListeners) {
    toAWTComponent().setText(value);
    toAWTComponent().setCaretPosition(0);

    if (fireListeners) {
      fireListeners();
    }
  }

  @SuppressWarnings("unchecked")
  @RequiredUIAccess
  private void fireListeners() {
    getListenerDispatcher(ValueListener.class).valueChanged(new ValueEvent(this, toAWTComponent().getText()));
  }
}
