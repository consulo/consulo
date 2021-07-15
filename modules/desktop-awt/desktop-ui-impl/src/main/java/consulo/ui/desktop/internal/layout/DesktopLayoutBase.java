/*
 * Copyright 2013-2019 consulo.io
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
package consulo.ui.desktop.internal.layout;

import consulo.awt.TargetAWT;
import consulo.awt.impl.FromSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 2019-02-16
 */
abstract class DesktopLayoutBase<T extends JPanel> extends SwingComponentDelegate<T> {
  class MyJPanel extends JPanel implements FromSwingComponentWrapper {
    MyJPanel(LayoutManager layout) {
      super(layout);
    }

    @Override
    public void updateUI() {
      super.updateUI();
    }

    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopLayoutBase.this;
    }
  }

  @SuppressWarnings("unchecked")
  protected void initDefaultPanel(LayoutManager layoutManager) {
    initialize((T)new MyJPanel(layoutManager));
  }

  protected void add(Component component, Object constraints) {
    T panel = toAWTComponent();
    panel.add(TargetAWT.to(component), constraints);
    panel.validate();
    panel.repaint();
  }
}
