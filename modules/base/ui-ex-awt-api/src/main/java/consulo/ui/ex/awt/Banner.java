/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ui.ex.awt;

import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.ex.JBColor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

class Banner extends NonOpaquePanel implements PropertyChangeListener {
  private int myBannerMinHeight;
  private final JComponent myText = new MyText();
  private final JLabel myProjectIcon = new JBLabel(PlatformIconGroup.generalProjectconfigurable(), SwingConstants.LEFT);
  private final NonOpaquePanel myActionsPanel = new NonOpaquePanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));

  private final Map<Action, LinkLabel> myActions = new HashMap<>();

  public Banner() {
    setLayout(new BorderLayout());

    setBorder(JBUI.Borders.empty(10, 10, 4, 0));

    myProjectIcon.setVisible(false);
    myProjectIcon.setForeground(JBColor.GRAY);
    myProjectIcon.setBorder(new EmptyBorder(0, 10, 0, 4));
    add(myText, BorderLayout.WEST);
    add(myProjectIcon, BorderLayout.CENTER);
    add(myActionsPanel, BorderLayout.EAST);
  }

  public void addAction(final Action action) {
    action.addPropertyChangeListener(this);
    LinkLabel label =
            new LinkLabel(null, null, (LinkListener)(aSource, aLinkData) -> action.actionPerformed(new ActionEvent(Banner.this, ActionEvent.ACTION_PERFORMED, Action.ACTION_COMMAND_KEY))) {
              @Override
              protected Color getTextColor() {
                return JBColor.BLUE;
              }
            };
    label.setFont(label.getFont().deriveFont(Font.BOLD));
    myActions.put(action, label);
    myActionsPanel.add(label);
    updateAction(action);
  }

  void updateAction(Action action) {
    LinkLabel label = myActions.get(action);
    label.setVisible(action.isEnabled());
    label.setText((String)action.getValue(Action.NAME));
    label.setToolTipText((String)action.getValue(Action.SHORT_DESCRIPTION));
  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    Object source = evt.getSource();
    if (source instanceof Action) {
      updateAction((Action)source);
    }
  }

  public void clearActions() {
    Set<Action> actions = myActions.keySet();
    for (Action each : actions) {
      each.removePropertyChangeListener(this);
    }
    myActions.clear();
    myActionsPanel.removeAll();
  }

  @Override
  public Dimension getMinimumSize() {
    Dimension size = super.getMinimumSize();
    size.height = Math.max(myBannerMinHeight, size.height);
    return size;
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension size = super.getPreferredSize();
    size.height = getMinimumSize().height;
    return size;
  }

  public void setMinHeight(int height) {
    myBannerMinHeight = height;
    revalidate();
    repaint();
  }

  public void setProjectIconDescription(@Nullable String toolTipText) {
    if (toolTipText != null) {
      myProjectIcon.setVisible(true);
      myProjectIcon.setToolTipText(toolTipText);
    }
    else {
      myProjectIcon.setVisible(false);
    }
  }

  public void setText(@Nonnull String... text) {
    myText.removeAll();
    for (int i = 0; i < text.length; i++) {
      JLabel eachLabel = new JLabel(text[i], SwingConstants.CENTER);
      eachLabel.setBorder(new EmptyBorder(0, 0, 0, 5));
      eachLabel.setVerticalTextPosition(SwingConstants.TOP);
      eachLabel.setFont(eachLabel.getFont().deriveFont(Font.BOLD, eachLabel.getFont().getSize()));
      myText.add(eachLabel);
      if (i < text.length - 1) {
        JLabel eachIcon = new JLabel("\u203A", SwingConstants.CENTER);
        eachIcon.setBorder(new EmptyBorder(0, 0, 0, 5));
        myText.add(eachIcon);
      }
    }
  }

  public void updateActions() {
    Set<Action> actions = myActions.keySet();
    for (Action action : actions) {
      updateAction(action);
    }
  }


  private static class MyText extends NonOpaquePanel {
    @Override
    public void doLayout() {
      int x = 0;
      for (int i = 0; i < getComponentCount(); i++) {
        Component each = getComponent(i);
        Dimension eachSize = each.getPreferredSize();
        each.setBounds(x, 0, eachSize.width, getHeight());
        x += each.getBounds().width;
      }
    }

    @Override
    public Dimension getPreferredSize() {
      Dimension size = new Dimension();
      for (int i = 0; i < getComponentCount(); i++) {
        Component each = getComponent(i);
        Dimension eachSize = each.getPreferredSize();
        size.width += eachSize.width;
        size.height = Math.max(size.height, eachSize.height);
      }

      return size;
    }
  }
}
