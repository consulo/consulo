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
package consulo.desktop.awt.wm.impl;

import consulo.application.Application;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.TaskInfo;
import consulo.dataContext.DataProvider;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.desktop.awt.wm.impl.status.InfoAndProgressPanel;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.openapi.ui.MessageType;
import consulo.ide.impl.idea.openapi.wm.impl.status.MemoryUsagePanel;
import consulo.ide.impl.idea.openapi.wm.impl.status.widget.StatusBarWidgetWrapper;
import consulo.ide.impl.idea.openapi.wm.impl.status.widget.StatusBarWidgetsActionGroup;
import consulo.ide.impl.idea.ui.popup.NotificationPopup;
import consulo.ide.impl.project.ui.impl.StatusWidgetBorders;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ui.internal.StatusBarEx;
import consulo.project.ui.wm.*;
import consulo.ui.NotificationType;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.awt.JBCurrentTheme;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.NonOpaquePanel;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.ComponentUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.popup.BalloonHandler;
import consulo.ui.image.Image;
import consulo.util.collection.Lists;
import consulo.util.dataholder.Key;
import consulo.util.lang.Couple;
import consulo.util.lang.ObjectUtil;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * User: spLeaner
 */
public class IdeStatusBarImpl extends JComponent implements StatusBarEx, Predicate<AWTEvent>, DataProvider {
  private static final Key<String> WIDGET_ID = Key.create("STATUS_BAR_WIDGET_ID");

  private static final int MIN_ICON_HEIGHT = 18 + 1 + 1;
  private final InfoAndProgressPanel myInfoAndProgressPanel;
  @Nonnull
  private final Application myApplication;
  private IdeFrame myFrame;

  private static final String uiClassID = "IdeStatusBarUI";

  private final Map<String, WidgetBean> myWidgetMap = new LinkedHashMap<>();

  private JPanel myLeftPanel;
  private JPanel myRightPanel;
  private JPanel myCenterPanel;
  private Component myHoveredComponent;

  private String myInfo;
  private String myRequestor;

  private final Set<IdeStatusBarImpl> myChildren = new HashSet<>();

  private static class WidgetBean {
    JComponent component;
    StatusBarWidget widget;

    static WidgetBean create(@Nonnull final StatusBarWidget widget, @Nonnull final JComponent component) {
      final WidgetBean bean = new WidgetBean();
      bean.widget = widget;
      bean.component = component;
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

  private void updateChildren(@RequiredUIAccess Consumer<IdeStatusBarImpl> action) {
    for (IdeStatusBarImpl child : myChildren) {
      action.accept(child);
    }
  }

  @Override
  @RequiredUIAccess
  public StatusBar createChild() {
    myApplication.assertIsDispatchThread();
    IdeStatusBarImpl bar = new IdeStatusBarImpl(myApplication, this);
    bar.setVisible(isVisible());
    myChildren.add(bar);
    Disposer.register(this, bar);
    Disposer.register(bar, () -> myChildren.remove(bar));

    JPanel rightPanel = rightPanel();

    List<String> order = new ArrayList<>();
    for (int i = 0; i < rightPanel.getComponentCount(); i++) {
      Component component = rightPanel.getComponent(i);

      String widgetId = UIUtil.getClientProperty(component, WIDGET_ID);
      if (widgetId != null) {
        order.add(widgetId);
      }
    }
    
    for (WidgetBean eachBean : myWidgetMap.values()) {
      if (eachBean.widget instanceof StatusBarWidget.Multiframe) {
        StatusBarWidget copy = ((StatusBarWidget.Multiframe)eachBean.widget).copy();
        bar.addWidget(copy, order);
      }
    }
    bar.repaint();

    return bar;
  }

  @Override
  public JComponent getComponent() {
    return this;
  }

  @RequiredUIAccess
  public IdeStatusBarImpl(@Nonnull Application application) {
    this(application, null);
  }

  @RequiredUIAccess
  public IdeStatusBarImpl(@Nonnull Application application, @Nullable IdeStatusBarImpl master) {
    myApplication = application;
    setLayout(new BorderLayout());
    setBorder(JBUI.Borders.empty(1, 0, 0, 6));

    myInfoAndProgressPanel = new InfoAndProgressPanel(this::getProject);
    Disposer.register(this, myInfoAndProgressPanel);
    centerPanel().add(myInfoAndProgressPanel.getComponent());

    setOpaque(true);
    updateUI();

    if (master == null) {
      DesktopToolWindowsSwicher swicher = new DesktopToolWindowsSwicher(this);
      swicher.update();

      Disposer.register(this, swicher);

      leftPanel().add(TargetAWT.to(swicher.getUIComponent()));
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
  public void addWidget(@Nonnull final StatusBarWidget widget, @Nonnull List<String> order, @Nonnull final Disposable parentDisposable) {
    myApplication.invokeLater(() -> addWidget(widget, order));
    Disposer.register(parentDisposable, () -> removeWidget(widget.getId()));
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

  @RequiredUIAccess
  private void addWidget(@Nonnull StatusBarWidget widget, @Nonnull List<String> order) {
    myApplication.assertIsDispatchThread();

    JPanel panel = rightPanel();
    JComponent c = wrap(widget);
    c.putClientProperty(WIDGET_ID, widget.getId());

    List<Component> children = new ArrayList<>(List.of(panel.getComponents()));
    children.add(c);

    Lists.weightSort(children, value -> {
      String widgetId = UIUtil.getClientProperty(value, WIDGET_ID);
      if (widgetId == null) {
        return 0;
      }
      
      return order.indexOf(widgetId);
    });
    
    myWidgetMap.put(widget.getId(), WidgetBean.create(widget, c));

    if (c instanceof StatusBarWidgetWrapper) {
      ((StatusBarWidgetWrapper)c).beforeUpdate();
    } else {
      widget.beforeUpdate();
    }
    
    widget.install(this);

    panel.removeAll();
    for (Component child : children) {
      panel.add(child);
    }
    panel.revalidate();

    Disposer.register(this, widget);

    if (widget instanceof StatusBarWidget.Multiframe) {
      StatusBarWidget.Multiframe multiFrameWidget = (StatusBarWidget.Multiframe)widget;
      updateChildren(child -> child.addWidget(multiFrameWidget.copy(), order));
    }
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
              r.width += Platform.current().os().isMac() ? 4 : 0;
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
    if (Project.KEY == dataId) {
      return getProject();
    }
    if (StatusBar.KEY == dataId) {
      return this;
    }
    if (HOVERED_WIDGET_ID == dataId) {
      return myHoveredComponent instanceof JComponent ? UIUtil.getClientProperty((JComponent)myHoveredComponent, WIDGET_ID): null;
    }
    return null;
  }

  @Override
  public void setInfo(@Nullable final String s) {
    setInfo(s, null);
  }

  @Override
  public void setInfo(@Nullable final String s, @Nullable final String requestor) {
    myApplication.invokeLater(() -> {
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
  public void addProgress(@Nonnull ProgressIndicator indicator, @Nonnull TaskInfo info) {
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
  public BalloonHandler notifyProgressByBalloon(@Nonnull NotificationType type, @Nonnull String htmlBody, @Nullable Image icon, @Nullable HyperlinkListener listener) {
    return myInfoAndProgressPanel.notifyByBalloon(MessageType.from(type), htmlBody, icon, listener);
  }

  @Override
  public void fireNotificationPopup(@Nonnull JComponent content, Color backgroundColor) {
    new NotificationPopup(this, content, backgroundColor);
  }

  private static JComponent wrap(@Nonnull final StatusBarWidget widget) {
    if (widget instanceof CustomStatusBarWidget) {
      JComponent component = ((CustomStatusBarWidget)widget).getComponent();
      if (component.getBorder() == null) {
        component.setBorder(widget instanceof IconLikeCustomStatusBarWidget ? StatusWidgetBorders.ICON : StatusWidgetBorders.INSTANCE);
      }
      // wrap with a panel, so it will fill entire status bar height
      JComponent result = component instanceof JLabel ? new NonOpaquePanel(new BorderLayout(), component) : component;
      return result;
    }

    JComponent wrapper = StatusBarWidgetWrapper.wrap(widget, Objects.requireNonNull(widget.getPresentation()));
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
      component.setBackground(JBCurrentTheme.StatusBar.hoverBackground());
    }
    repaint();
  }

  @Override
  public boolean test(@Nonnull AWTEvent e) {
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
    myApplication.invokeLater(() -> {
      WidgetBean bean = myWidgetMap.remove(id);
      if (bean != null) {
        JPanel targetPanel = rightPanel();
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

  @Nonnull
  @Override
  @SuppressWarnings("unchecked")
  public <W extends StatusBarWidget> Optional<W> findWidget(@Nonnull Predicate<StatusBarWidget> predicate) {
    return myWidgetMap
      .values()
      .stream()
      .filter(widgetBean -> predicate.test(widgetBean.widget))
      .map(widgetBean -> (W)widgetBean.widget)
      .findFirst();
  }

  @Override
  public void updateWidget(@Nonnull Predicate<StatusBarWidget> widgetPredicate) {
    myWidgetMap
      .entrySet()
      .stream()
      .filter(it -> widgetPredicate.test(it.getValue().widget))
      .forEach(it -> updateWidget(it.getKey()));
  }

  @Override
  public void updateWidget(@Nonnull final String id) {
    myApplication.invokeLater(() -> {
      WidgetBean bean = myWidgetMap.get(id);
      if (bean != null) {
        JComponent widgetComponent = bean.component;

        if (widgetComponent instanceof StatusBarWidgetWrapper) {
          ((StatusBarWidgetWrapper)widgetComponent).beforeUpdate();
        } else {
          bean.widget.beforeUpdate();
        }

        widgetComponent.repaint();
      }

      updateChildren(child -> child.updateWidget(id));
    });
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
      g.setColor(JBCurrentTheme.StatusBar.hoverBackground());
      g.fillRect(point.x, point.y, bounds.width, bounds.height);
    }
  }

  @Override
  public IdeFrame getFrame() {
    return myFrame;
  }
}
