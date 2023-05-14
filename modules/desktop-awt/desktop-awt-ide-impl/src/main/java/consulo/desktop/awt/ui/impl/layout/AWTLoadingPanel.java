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
package consulo.desktop.awt.ui.impl.layout;

import consulo.desktop.awt.facade.FromSwingComponentWrapper;
import consulo.disposer.Disposable;
import consulo.ui.Component;
import consulo.ui.ex.awt.AsyncProcessIcon;
import consulo.ui.ex.awt.LoadingDecorator;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ColorUtil;

import jakarta.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @see consulo.ide.impl.idea.ui.components.JBLoadingPanel
 * @since 23/04/2023
 */
public class AWTLoadingPanel extends JPanel implements FromSwingComponentWrapper {
  private final Component myUIComponent;
  private final JComponent myInnerComponent;
  private final LoadingDecorator myDecorator;

  public AWTLoadingPanel(@Nonnull Component uiComponent, @Nonnull JComponent inner, @Nonnull Disposable parent) {
    super(new BorderLayout());
    myInnerComponent = inner;
    myUIComponent = uiComponent;
    myInnerComponent.setOpaque(false);
    myInnerComponent.setFocusable(false);
    myDecorator = new LoadingDecorator(inner, parent, -1) {
      @Override
      protected NonOpaquePanel customizeLoadingLayer(JPanel parent, JLabel text, AsyncProcessIcon icon) {
        final NonOpaquePanel panel = super.customizeLoadingLayer(parent, text, icon);
        customizeStatusText(text);
        return panel;
      }
    };
    add(myDecorator.getComponent(), BorderLayout.CENTER);
  }

  public static void customizeStatusText(JLabel text) {
    Font font = text.getFont();
    text.setFont(font.deriveFont(font.getStyle(), font.getSize() + 6));
    text.setForeground(ColorUtil.toAlpha(UIUtil.getLabelForeground(), 150));
  }

  public void setLoadingText(String text) {
    myDecorator.setLoadingText(text);
  }

  public void stopLoading() {
    myDecorator.stopLoading();
  }

  public boolean isLoading() {
    return myDecorator.isLoading();
  }

  public void startLoading() {
    myDecorator.startLoading(false);
  }

  public JComponent getContentPanel() {
    return myInnerComponent;
  }

  @Override
  public Dimension getPreferredSize() {
    return getContentPanel().getPreferredSize();
  }

  @Nonnull
  @Override
  public Component toUIComponent() {
    return myUIComponent;
  }
}
