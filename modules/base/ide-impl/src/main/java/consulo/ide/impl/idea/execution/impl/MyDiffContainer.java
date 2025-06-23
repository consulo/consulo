/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.execution.impl;

import consulo.disposer.Disposable;
import consulo.ui.ex.awt.JBLayeredPane;
import consulo.ui.ex.awt.AbstractLayoutManager;
import consulo.ui.ex.awt.AnimatedIconComponent;
import consulo.ui.ex.awt.AsyncProcessIcon;
import consulo.ui.ex.awt.UIUtil;
import consulo.disposer.Disposer;

import javax.swing.*;
import java.awt.*;

/**
 * @author amakeev
 * @author Irina.Chernushina
 * @since 2011-08-08
 */
public class MyDiffContainer extends JBLayeredPane implements Disposable {
  private final AnimatedIconComponent myIcon = new AsyncProcessIcon(getClass().getName());

  private final JComponent myContent;
  private final JComponent myLoadingPanel;
  private final JLabel myJLabel;

  public MyDiffContainer(JComponent content, final String text) {
    setLayout(new MyOverlayLayout());
    myContent = content;
    myLoadingPanel = new JPanel(new MyPanelLayout());
    myLoadingPanel.setOpaque(false);
    myLoadingPanel.add(myIcon);
    Disposer.register(this, myIcon);
    myJLabel = new JLabel(text);
    myJLabel.setForeground(UIUtil.getInactiveTextColor());
    myLoadingPanel.add(myJLabel);

    add(myContent);
    add(myLoadingPanel, JLayeredPane.POPUP_LAYER);

    finishUpdating();
  }

  @Override
  public void dispose() {
  }

  public void startUpdating() {
    myLoadingPanel.setVisible(true);
    myIcon.resume();
  }

  public void finishUpdating() {
    myIcon.suspend();
    myLoadingPanel.setVisible(false);
  }

  private class MyOverlayLayout extends AbstractLayoutManager {
    @Override
    public void layoutContainer(Container parent) {
      myContent.setBounds(0, 0, getWidth(), getHeight());
      myLoadingPanel.setBounds(0, 0, getWidth(), getHeight());
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
      return myContent.getPreferredSize();
    }
  }

  private class MyPanelLayout extends AbstractLayoutManager {
    @Override
    public void layoutContainer(Container parent) {
      Dimension size = myIcon.getPreferredSize();
      Dimension preferredSize = myJLabel.getPreferredSize();
      int width = getWidth();
      int offset = width - size.width - 15 - preferredSize.width;
      myIcon.setBounds(offset, 0, size.width, size.height);
      myJLabel.setBounds(offset + size.width + 3, 0, preferredSize.width, size.height);
    }

    @Override
    public Dimension preferredLayoutSize(Container parent) {
      return myContent.getPreferredSize();
    }
  }
}
