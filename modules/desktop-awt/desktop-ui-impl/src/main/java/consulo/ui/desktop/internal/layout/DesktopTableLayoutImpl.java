/*
 * Copyright 2013-2017 consulo.io
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

import com.intellij.util.ui.JBUI;
import consulo.awt.TargetAWT;
import consulo.awt.impl.FromSwingComponentWrapper;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.desktop.internal.base.SwingComponentDelegate;
import consulo.ui.layout.TableLayout;
import consulo.ui.StaticPosition;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.awt.*;

/**
 * @author VISTALL
 * @since 06-Nov-17
 */
public class DesktopTableLayoutImpl extends SwingComponentDelegate<JPanel> implements TableLayout {
  class MyJPanel extends JPanel implements FromSwingComponentWrapper {
    MyJPanel(LayoutManager layout) {
      super(layout);
    }

    @Nonnull
    @Override
    public Component toUIComponent() {
      return DesktopTableLayoutImpl.this;
    }
  }

  private JPanel myGridPanel;

  public DesktopTableLayoutImpl(StaticPosition position) {
    if (position == StaticPosition.CENTER) {
      myComponent = myGridPanel = new MyJPanel(new GridBagLayout());
    }
    else {
      myComponent = new MyJPanel(new BorderLayout());
      myGridPanel = new JPanel(new GridBagLayout());
      switch (position) {
        case TOP:
          myComponent.add(myGridPanel, BorderLayout.NORTH);
          break;
        case BOTTOM:
          myComponent.add(myGridPanel, BorderLayout.SOUTH);
          break;
        case LEFT:
          myComponent.add(myGridPanel, BorderLayout.WEST);
          break;
        case RIGHT:
          myComponent.add(myGridPanel, BorderLayout.EAST);
          break;
        default:
          throw new UnsupportedOperationException();
      }
    }
  }

  @RequiredUIAccess
  @Nonnull
  @Override
  public TableLayout add(@Nonnull Component component, @Nonnull TableCell tableCell) {
    GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridwidth = 1;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.fill = tableCell.isFill() ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
    constraints.gridx = tableCell.getColumn();
    constraints.gridy = tableCell.getRow();
    constraints.weightx = tableCell.isFill() ? 1 : 0;
    constraints.insets = JBUI.insets(5, 0, 5, 0);
    myGridPanel.add(TargetAWT.to(component), constraints);
    return this;
  }
}
