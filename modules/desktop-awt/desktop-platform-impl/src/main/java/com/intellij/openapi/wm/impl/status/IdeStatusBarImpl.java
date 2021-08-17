/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package com.intellij.openapi.wm.impl.status;

import com.intellij.diagnostic.IdeMessagePanel;
import com.intellij.ide.IdeEventQueue;
import com.intellij.notification.impl.IdeNotificationArea;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.TaskInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.BalloonHandler;
import com.intellij.openapi.util.Couple;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.*;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import com.intellij.openapi.wm.ex.StatusBarEx;
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetWrapper;
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetsActionGroup;
import com.intellij.ui.ComponentUtil;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.popup.NotificationPopup;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import consulo.awt.TargetAWT;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.image.Image;
import consulo.util.collection.ArrayUtil;
import consulo.util.dataholder.Key;
import consulo.util.lang.ObjectUtil;
import javax.annotation.Nonnull;

import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;

/**
 * User: spLeaner
 */
public class IdeStatusBarImpl extends JComponent implements StatusBarEx, IdeEventQueue.EventDispatcher, DataProvider {
  private static final String WIDGET_ID = "STATUS_BAR_WIDGET_ID";

  private static final int MIN_ICON_HEIGHT = 18 + 1 + 1;
  private final InfoAndProgressPanel myInfoAndProgressPanel;
  private IdeFrame myFrame;

  private enum Position {
    LEFT,
    RIGHT,
    CENTER
  }

  private static final String uiClassID = "IdeStatusBarUI";

  private final Map<String, WidgetBean> myWidgetMap = new HashMap<>();

  private JPanel myLeftPanel;
  private JPanel myRightPanel;
  private JPanel myCenterPanel;
  private Component myHoveredComponent;

  private String myInfo;
  private String myRequestor;

  private final Set<IdeStatusBarImpl> myChildren = new HashSet<>();

  private static class WidgetBean {
    JComponent component;
    Position position;
    StatusBarWidget widget;
    String anchor;

    static WidgetBean create(@Nonnull final StatusBarWidget widget, @Nonnull final Position position, @Nonnull final JComponent component, @Nonnull String anchor) {
      final WidgetBean bean = new WidgetBean();
      bean.widget = widget;
      bean.position = position;
      bean.component = component;
      bean.anchor = anchor;
      return bean;
    }
  }

  @Override
  public StatusBar findChild(Component c) {
    Component eachParent = c;
    IdeFrame frame = null;
    while (eachParent != null) {
      if (eachParent instanceof Window) {
        consulo.ui.Window uiWindow = TargetAWT.from((Window)eachParent);
        frame = uiWindow.getUserData(IdeFrame.KEY);
      }
      eachParent = eachParent.getParent();
    }

    return frame != null ? frame.getStatusBar() : this;
  }

  @Override
  public void install(IdeFrame frame) {
    myFrame = frame;
  }

  @Nullable
  @Override
  public Project getProject() {
    return myFrame == null ? null : myFrame.getProject();
  }

  private void updateChildren(ChildAction action) {
    for (IdeStatusBarImpl child : myChildren) {
      action.update(child);
    }
  }

  interface ChildAction {
    void update(IdeStatusBarImpl child);
  }

  @Override
  public StatusBar createChild() {
    ApplicationManager.getApplication().assertIsDispatchThread();
    IdeStatusBarImpl bar = new IdeStatusBarImpl(this);
    bar.setVisible(isVisible());
    myChildren.add(bar);
    Disposer.register(this, bar);
    Disposer.register(bar, () -> myChildren.remove(bar));

    for (WidgetBean eachBean : myWidgetMap.values()) {
      if (eachBean.widget instanceof StatusBarWidget.Multiframe) {
        StatusBarWidget copy = ((StatusBarWidget.Multiframe)eachBean.widget).copy();
        bar.addWidget(copy, eachBean.position, eachBean.anchor);
      }
    }
    bar.repaint();

    return bar;
  }

  @Override
  public JComponent getComponent() {
    return this;
  }

  public IdeStatusBarImpl() {
    this(null);
  }

  public IdeStatusBarImpl(@Nullable IdeStatusBarImpl master) {
    setLayout(new BorderLayout());
    setBorder(JBUI.Borders.empty(1, 0, 0, 6));

    myInfoAndProgressPanel = new InfoAndProgressPanel();
    addWidget(myInfoAndProgressPanel, Position.CENTER, "__IGNORED__");

    setOpaque(true);
    updateUI();

    if (master == null) {
      addWidget(new ToolWindowsWidget(this), Position.LEFT);
    }

    enableEvents(AWTEvent.MOUSE_EVENT_MASK);
    enableEvents(AWTEvent.MOUSE_MOTION_EVENT_MASK);

    IdeEventQueue.getInstance().addPostprocessor(this, this);
  }

  @Override
  public Dimension getPreferredSize() {
    Dimension size = super.getPreferredSize();
    if (size == null) return null;

    Insets insets = getInsets();
    int minHeight = insets.top + insets.bottom + MIN_ICON_HEIGHT;
    return new Dimension(size.width, Math.max(size.height, minHeight));
  }

  @Override
  public void addWidget(@Nonnull final StatusBarWidget widget) {
    UIUtil.invokeLaterIfNeeded(() -> addWidget(widget, Position.RIGHT, "__AUTODETECT__"));
  }

  @Override
  public void addWidget(@Nonnull final StatusBarWidget widget, @Nonnull final String anchor) {
    UIUtil.invokeLaterIfNeeded(() -> addWidget(widget, Position.RIGHT, anchor));
  }

  private void addWidget(@Nonnull final StatusBarWidget widget, @Nonnull final Position pos) {
    UIUtil.invokeLaterIfNeeded(() -> addWidget(widget, pos, "__IGNORED__"));
  }

  @Override
  public void addWidget(@Nonnull final StatusBarWidget widget, @Nonnull final Disposable parentDisposable) {
    addWidget(widget);
    Disposer.register(parentDisposable, () -> removeWidget(widget.ID()));
  }

  @Override
  public void addWidget(@Nonnull final StatusBarWidget widget, @Nonnull String anchor, @Nonnull final Disposable parentDisposable) {
    addWidget(widget, anchor);
    Disposer.register(parentDisposable, () -> removeWidget(widget.ID()));
  }

  @Override
  public void updateUI() {
    setUI(UIManager.getUI(this));
  }

  @Override
  public void dispose() {
    myWidgetMap.clear();
    myChildren.clear();

    if (myLeftPanel != null) myLeftPanel.removeAll();
    if (myRightPanel != null) myRightPanel.removeAll();
    if (myCenterPanel != null) myCenterPanel.removeAll();
  }

  private void addWidget(@Nonnull StatusBarWidget widget, @Nonnull Position position, @Nonnull String anchor) {
    ApplicationManager.getApplication().assertIsDispatchThread();
    JComponent c = wrap(widget);
    JPanel panel = getTargetPanel(position);
    if (position == Position.LEFT && panel.getComponentCount() == 0) {
      c.setBorder(SystemInfo.isMac ? JBUI.Borders.empty(2, 0, 2, 4) : JBUI.Borders.empty());
    }
    panel.add(c, getPositionIndex(position, anchor));
    myWidgetMap.put(widget.ID(), WidgetBean.create(widget, position, c, anchor));
    if (c instanceof StatusBarWidgetWrapper) {
      ((StatusBarWidgetWrapper)c).beforeUpdate();
    }
    widget.install(this);
    panel.revalidate();
    Disposer.register(this, widget);
    if (widget instanceof StatusBarWidget.Multiframe) {
      StatusBarWidget.Multiframe multiFrameWidget = (StatusBarWidget.Multiframe)widget;
      updateChildren(child -> child.addWidget(multiFrameWidget.copy(), position, anchor));
    }
  }

  private int getPositionIndex(@Nonnull IdeStatusBarImpl.Position position, @Nonnull String anchor) {
    if (Position.RIGHT == position && myRightPanel.getComponentCount() > 0) {
      WidgetBean widgetAnchor = null;
      boolean before = false;
      List<String> parts = StringUtil.split(anchor, " ");
      if (parts.size() > 1) {
        widgetAnchor = myWidgetMap.get(parts.get(1));
        before = "before".equalsIgnoreCase(parts.get(0));
      }
      if (widgetAnchor == null) {
        widgetAnchor = myWidgetMap.get(IdeNotificationArea.WIDGET_ID);
        if (widgetAnchor == null) {
          widgetAnchor = myWidgetMap.get(IdeMessagePanel.FATAL_ERROR);
        }
        before = true;
      }
      if (widgetAnchor != null) {
        int anchorIndex = ArrayUtil.indexOf(myRightPanel.getComponents(), widgetAnchor.component);
        return before ? anchorIndex : anchorIndex + 1;
      }
    }
    return -1;
  }


  @Nonnull
  private JPanel getTargetPanel(@Nonnull IdeStatusBarImpl.Position position) {
    if (position == Position.RIGHT) {
      return rightPanel();
    }
    if (position == Position.LEFT) {
      return leftPanel();
    }
    return centerPanel();
  }

  @Nonnull
  private JPanel centerPanel() {
    if (myCenterPanel == null) {
      myCenterPanel = JBUI.Panels.simplePanel().andTransparent();
      myCenterPanel.setBorder(JBUI.Borders.empty(0, 1));
      add(myCenterPanel, BorderLayout.CENTER);
    }
    return myCenterPanel;
  }

  @Nonnull
  private JPanel rightPanel() {
    if (myRightPanel == null) {
      myRightPanel = new JPanel();
      myRightPanel.setBorder(JBUI.Borders.emptyLeft(1));
      myRightPanel.setLayout(new BoxLayout(myRightPanel, BoxLayout.X_AXIS) {
        @Override
        public void layoutContainer(Container target) {
          super.layoutContainer(target);
          for (Component component : target.getComponents()) {
            if (component instanceof MemoryUsagePanel) {
              Rectangle r = component.getBounds();
              r.y = 0;
              r.width += SystemInfo.isMac ? 4 : 0;
              r.height = target.getHeight();
              component.setBounds(r);
            }
          }
        }
      });
      myRightPanel.setOpaque(false);
      add(myRightPanel, BorderLayout.EAST);
    }
    return myRightPanel;
  }

  @Nonnull
  private JPanel leftPanel() {
    if (myLeftPanel == null) {
      myLeftPanel = new JPanel();
      myLeftPanel.setBorder(JBUI.Borders.empty(0, 4, 0, 1));
      myLeftPanel.setLayout(new BoxLayout(myLeftPanel, BoxLayout.X_AXIS));
      myLeftPanel.setOpaque(false);
      add(myLeftPanel, BorderLayout.WEST);
    }
    return myLeftPanel;
  }

  @Override
  @Nullable
  public Object getData(@Nonnull Key dataId) {
    if (CommonDataKeys.PROJECT == dataId) {
      return getProject();
    }
    if (PlatformDataKeys.STATUS_BAR == dataId) {
      return this;
    }
    if (HOVERED_WIDGET_ID == dataId) {
      return myHoveredComponent instanceof JComponent ? ((JComponent)myHoveredComponent).getClientProperty(WIDGET_ID) : null;
    }
    return null;
  }

  @Override
  public void setInfo(@Nullable final String s) {
    setInfo(s, null);
  }

  @Override
  public void setInfo(@Nullable final String s, @Nullable final String requestor) {
    UIUtil.invokeLaterIfNeeded(() -> {
      if (myInfoAndProgressPanel != null) {
        Couple<String> pair = myInfoAndProgressPanel.setText(s, requestor);
        myInfo = pair.first;
        myRequestor = pair.second;
      }
    });
  }

  @Override
  public String getInfo() {
    return myInfo;
  }

  @Override
  public String getInfoRequestor() {
    return myRequestor;
  }

  @Override
  public void addProgress(@Nonnull ProgressIndicatorEx indicator, @Nonnull TaskInfo info) {
    myInfoAndProgressPanel.addProgress(indicator, info);
  }

  @Override
  public List<Pair<TaskInfo, ProgressIndicator>> getBackgroundProcesses() {
    return myInfoAndProgressPanel.getBackgroundProcesses();
  }

  @Override
  public void setProcessWindowOpen(final boolean open) {
    myInfoAndProgressPanel.setProcessWindowOpen(open);
  }

  @Override
  public boolean isProcessWindowOpen() {
    return myInfoAndProgressPanel.isProcessWindowOpen();
  }

  @Override
  public void startRefreshIndication(final String tooltipText) {
    myInfoAndProgressPanel.setRefreshToolTipText(tooltipText);
    myInfoAndProgressPanel.setRefreshVisible(true);

    updateChildren(child -> child.startRefreshIndication(tooltipText));
  }

  @Override
  public void stopRefreshIndication() {
    myInfoAndProgressPanel.setRefreshVisible(false);

    updateChildren(IdeStatusBarImpl::stopRefreshIndication);
  }

  @Override
  public BalloonHandler notifyProgressByBalloon(@Nonnull MessageType type, @Nonnull String htmlBody) {
    return notifyProgressByBalloon(type, htmlBody, null, null);
  }

  @Override
  public BalloonHandler notifyProgressByBalloon(@Nonnull MessageType type, @Nonnull String htmlBody, @Nullable Image icon, @Nullable HyperlinkListener listener) {
    return myInfoAndProgressPanel.notifyByBalloon(type, htmlBody, icon, listener);
  }

  @Override
  public void fireNotificationPopup(@Nonnull JComponent content, Color backgroundColor) {
    new NotificationPopup(this, content, backgroundColor);
  }

  private static JComponent wrap(@Nonnull final StatusBarWidget widget) {
    if (widget instanceof CustomStatusBarWidget) {
      JComponent component = ((CustomStatusBarWidget)widget).getComponent();
      if (component.getBorder() == null) {
        component.setBorder(widget instanceof IconLikeCustomStatusBarWidget ? StatusBarWidget.WidgetBorder.ICON : StatusBarWidget.WidgetBorder.INSTANCE);
      }
      // wrap with a panel, so it will fill entire status bar height
      JComponent result = component instanceof JLabel ? new NonOpaquePanel(new BorderLayout(), component) : component;
      result.putClientProperty(WIDGET_ID, widget.ID());
      return result;
    }

    JComponent wrapper = StatusBarWidgetWrapper.wrap(Objects.requireNonNull(widget.getPresentation()));
    wrapper.putClientProperty(WIDGET_ID, widget.ID());
    wrapper.putClientProperty(UIUtil.CENTER_TOOLTIP_DEFAULT, Boolean.TRUE);
    return wrapper;
  }

  private void hoverComponent(@Nullable Component component) {
    if (myHoveredComponent == component) return;
    myHoveredComponent = component;
    // widgets shall not be opaque, as it may conflict with bg images
    // the following code can be dropped in future
    if (myHoveredComponent != null) {
      myHoveredComponent.setBackground(null);
    }
    if (component != null && component.isEnabled()) {
      component.setBackground(JBUI.CurrentTheme.StatusBar.hoverBackground());
    }
    repaint();
  }

  @Override
  public boolean dispatch(@Nonnull AWTEvent e) {
    if (e instanceof MouseEvent) {
      return dispatchMouseEvent((MouseEvent)e);
    }
    return false;
  }

  private boolean dispatchMouseEvent(@Nonnull MouseEvent e) {
    if (myRightPanel == null || myCenterPanel == null || !myRightPanel.isVisible()) {
      return false;
    }
    Component component = e.getComponent();
    if (component == null) {
      return false;
    }

    if (ComponentUtil.getWindow(myFrame.getComponent()) != ComponentUtil.getWindow(component)) {
      hoverComponent(null);
      return false;
    }

    Point point = SwingUtilities.convertPoint(component, e.getPoint(), myRightPanel);
    Component widget = myRightPanel.getComponentAt(point);
    if (e.getClickCount() == 0) {
      hoverComponent(widget != myRightPanel ? widget : null);
    }
    if (e.isConsumed() || widget == null) {
      return false;
    }
    if (e.isPopupTrigger() && (e.getID() == MouseEvent.MOUSE_PRESSED || e.getID() == MouseEvent.MOUSE_RELEASED)) {
      Project project = getProject();
      if (project != null) {
        ActionManager actionManager = ActionManager.getInstance();
        ActionGroup group = ObjectUtil.tryCast(actionManager.getAction(StatusBarWidgetsActionGroup.GROUP_ID), ActionGroup.class);
        if (group != null) {
          ActionPopupMenu menu = actionManager.createActionPopupMenu(ActionPlaces.STATUS_BAR_PLACE, group);
          menu.setTargetComponent(this);
          menu.getComponent().show(myRightPanel, point.x, point.y);
          e.consume();
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public String getUIClassID() {
    return uiClassID;
  }

  @Override
  public void removeWidget(@Nonnull final String id) {
    UIUtil.invokeLaterIfNeeded(() -> {
      WidgetBean bean = myWidgetMap.remove(id);
      if (bean != null) {
        JPanel targetPanel = getTargetPanel(bean.position);
        targetPanel.remove(bean.component);
        targetPanel.revalidate();
        Disposer.dispose(bean.widget);
      }
      updateChildren(child -> child.removeWidget(id));
    });
  }

  @Override
  public void updateWidgets() {
    for (final String s : myWidgetMap.keySet()) {
      updateWidget(s);
    }

    updateChildren(IdeStatusBarImpl::updateWidgets);
  }

  @Override
  public void updateWidget(@Nonnull final String id) {
    UIUtil.invokeLaterIfNeeded(() -> {
      JComponent widgetComponent = getWidgetComponent(id);
      if (widgetComponent != null) {
        if (widgetComponent instanceof StatusBarWidgetWrapper) {
          ((StatusBarWidgetWrapper)widgetComponent).beforeUpdate();
        }
        widgetComponent.repaint();
      }

      updateChildren(child -> child.updateWidget(id));
    });
  }

  @Override
  @Nullable
  public StatusBarWidget getWidget(String id) {
    WidgetBean bean = myWidgetMap.get(id);
    return bean == null ? null : bean.widget;
  }

  @Nullable
  private JComponent getWidgetComponent(@Nonnull String id) {
    WidgetBean bean = myWidgetMap.get(id);
    return bean == null ? null : bean.component;
  }

  @Override
  protected void paintChildren(Graphics g) {
    paintHoveredComponentBackground(g);
    super.paintChildren(g);
  }

  private void paintHoveredComponentBackground(Graphics g) {
    if (myHoveredComponent != null && myHoveredComponent.isEnabled() && !(myHoveredComponent instanceof MemoryUsagePanel)) {
      Rectangle bounds = myHoveredComponent.getBounds();
      Point point = new RelativePoint(myHoveredComponent.getParent(), bounds.getLocation()).getPoint(this);
      g.setColor(JBUI.CurrentTheme.StatusBar.hoverBackground());
      g.fillRect(point.x, point.y, bounds.width, bounds.height);
    }
  }

  @Override
  public IdeFrame getFrame() {
    return myFrame;
  }
}
