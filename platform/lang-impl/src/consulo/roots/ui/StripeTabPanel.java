/*
 * Copyright 2013-2015 must-be.org
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
package consulo.roots.ui;

import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.impl.StaticAnchoredButton;
import com.intellij.openapi.wm.impl.StripeButtonUI;
import com.intellij.ui.border.CustomLineBorder;
import com.intellij.util.EventDispatcher;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mustbe.consulo.RequiredDispatchThread;

import javax.swing.*;
import javax.swing.plaf.ButtonUI;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.List;

/**
 * @author VISTALL
 * @since 05.08.2015
 */
public class StripeTabPanel extends JPanel {
  public static interface SelectListener extends EventListener {
    @RequiredDispatchThread
    void selected(@NotNull TabInfo tabInfo);
  }

  public static class TabInfo extends UserDataHolderBase {
    private JComponent myComponent;
    private JComponent myPreferredFocusableComponent;
    private StaticAnchoredButton myButton;

    public TabInfo(StaticAnchoredButton button, JComponent component, JComponent preferredFocusableComponent) {
      myButton = button;
      myComponent = component;
      myPreferredFocusableComponent = preferredFocusableComponent;
    }

    @Nullable
    public JComponent getPreferredFocusableComponent() {
      return myPreferredFocusableComponent;
    }

    @NotNull
    public String getTabName() {
      return myButton.getText();
    }

    @NotNull
    public JComponent getComponent() {
      return myComponent;
    }

    public boolean isSelected() {
      return myButton.isSelected();
    }

    @RequiredDispatchThread
    public void select() {
      myButton.setSelected(true);
    }

    public void setEnabled(boolean enabled) {
      myButton.setEnabled(enabled);
    }
  }

  private final JPanel myTabPanel;
  private final JPanel myContentPanel;
  private final ButtonGroup myButtonGroup = new ButtonGroup();
  private final ItemListener myItemListener = new ItemListener() {
    @Override
    @RequiredDispatchThread
    public void itemStateChanged(ItemEvent e) {
      if (e.getStateChange() == ItemEvent.SELECTED) {
        StaticAnchoredButton source = (StaticAnchoredButton)e.getSource();

        TabInfo tabInfo = (TabInfo)source.getClientProperty(TabInfo.class);
        CardLayout contentPanel = (CardLayout)myContentPanel.getLayout();
        contentPanel.show(myContentPanel, tabInfo.getTabName());

        mySelectListenerDispatcher.getMulticaster().selected(tabInfo);

        JComponent preferredFocusableComponent = tabInfo.getPreferredFocusableComponent();
        if(preferredFocusableComponent != null) {
          preferredFocusableComponent.requestFocus();
        }
      }
    }
  };

  private final EventDispatcher<SelectListener> mySelectListenerDispatcher = EventDispatcher.create(SelectListener.class);

  public StripeTabPanel() {
    super(new BorderLayout());
    myTabPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, true, false));
    myTabPanel.setBorder(new CustomLineBorder(0, 0, 0, JBUI.scale(1)));

    add(myTabPanel, BorderLayout.WEST);

    myContentPanel = new JPanel(new CardLayout());
    add(myContentPanel, BorderLayout.CENTER);
  }

  public void addSelectListener(@NotNull SelectListener listener) {
    mySelectListenerDispatcher.addListener(listener);
  }

  public void removeSelectListener(@NotNull SelectListener listener) {
    mySelectListenerDispatcher.removeListener(listener);
  }

  @NotNull
  @RequiredDispatchThread
  public TabInfo addTab(@NotNull String tabName, @NotNull JComponent component) {
    return addTab(tabName, component, component);
  }

  @NotNull
  @RequiredDispatchThread
  public TabInfo addTab(@NotNull String tabName, @NotNull JComponent component, @Nullable JComponent preferredFocusableComponent) {
    StaticAnchoredButton button = new StaticAnchoredButton(tabName, ToolWindowAnchor.LEFT);

    button.addItemListener(myItemListener);
    button.setFont(UIUtil.getLabelFont(UIUtil.FontSize.SMALL));
    button.setBackground(new Color(247, 243, 239));
    button.setUI((ButtonUI)StripeButtonUI.createUI(button));

    myTabPanel.add(button);

    TabInfo tabInfo = new TabInfo(button, component, preferredFocusableComponent);
    button.putClientProperty(TabInfo.class, tabInfo);

    myButtonGroup.add(button);
    myContentPanel.add(component, tabName);
    if(myButtonGroup.getSelection() == null) {
      tabInfo.select();
    }
    return tabInfo;
  }

  @Nullable
  public TabInfo getSelectedTab() {
    Enumeration<AbstractButton> elements = myButtonGroup.getElements();
    while (elements.hasMoreElements()) {
      AbstractButton button = elements.nextElement();

      if (button.isSelected()) {
        return (TabInfo)button.getClientProperty(TabInfo.class);
      }
    }
    return null;
  }

  @NotNull
  public List<TabInfo> getTabs() {
    int buttonCount = myButtonGroup.getButtonCount();
    List<TabInfo> list = new ArrayList<TabInfo>(buttonCount);
    Enumeration<AbstractButton> elements = myButtonGroup.getElements();
    while (elements.hasMoreElements()) {
      AbstractButton button = elements.nextElement();

      TabInfo tabInfo = (TabInfo)button.getClientProperty(TabInfo.class);
      list.add(tabInfo);
    }
    return list;
  }
}
