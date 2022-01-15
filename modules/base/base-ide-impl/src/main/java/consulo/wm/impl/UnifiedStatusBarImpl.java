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
package consulo.wm.impl;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.TaskInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.MessageType;
import com.intellij.openapi.ui.popup.BalloonHandler;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.wm.CustomStatusBarWidget;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.StatusBarWidget;
import com.intellij.openapi.wm.ex.ProgressIndicatorEx;
import com.intellij.openapi.wm.ex.StatusBarEx;
import com.intellij.openapi.wm.impl.status.ToolWindowsWidget;
import com.intellij.openapi.wm.impl.status.widget.StatusBarWidgetWrapper;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.Label;
import consulo.ui.PseudoComponent;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.border.BorderPosition;
import consulo.ui.border.BorderStyle;
import consulo.ui.image.Image;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.WrappedLayout;
import consulo.ui.style.ComponentColors;
import consulo.util.lang.StringUtil;
import consulo.wm.impl.status.UnifiedInfoAndProgressPanel;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.HyperlinkListener;
import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author VISTALL
 * @since 25-Sep-17
 */
public class UnifiedStatusBarImpl implements StatusBarEx {
  private IdeFrame myFrame;

  private enum Position {
    LEFT,
    RIGHT,
    CENTER
  }

  private final Map<String, WidgetBean> myWidgetMap = new HashMap<>();

  private HorizontalLayout myLeftPanel;
  private HorizontalLayout myRightPanel;
  private HorizontalLayout myCenterPanel;

  private String myInfo;
  private String myRequestor;

  private static class WidgetBean {
    PseudoComponent component;
    Position position;
    StatusBarWidget widget;
    String anchor;

    static WidgetBean create(@Nonnull final StatusBarWidget widget, @Nonnull final Position position, @Nonnull final PseudoComponent component, @Nonnull String anchor) {
      final WidgetBean bean = new WidgetBean();
      bean.widget = widget;
      bean.position = position;
      bean.component = component;
      bean.anchor = anchor;
      return bean;
    }
  }

  private final Application myApplication;

  private final DockLayout myComponent = DockLayout.create();

  private UnifiedInfoAndProgressPanel myInfoAndProgressPanel;

  private String myInfoText;

  @RequiredUIAccess
  public UnifiedStatusBarImpl(Application application, @Nullable StatusBar master) {
    myApplication = application;

    myInfoAndProgressPanel = new UnifiedInfoAndProgressPanel();
    addWidget(myInfoAndProgressPanel, Position.CENTER, "__IGNORED__");

    if (master == null) {
      addWidget(new ToolWindowsWidget(this), Position.LEFT);
    }

    myComponent.addUserDataProvider(CommonDataKeys.PROJECT, this::getProject);
    myComponent.addUserDataProvider(PlatformDataKeys.STATUS_BAR, () -> this);

    myComponent.addBorder(BorderPosition.TOP, BorderStyle.LINE, ComponentColors.BORDER, 1);
  }

  @Override
  public void setInfo(@Nullable String text) {
    myInfoText = text;

    myApplication.getLastUIAccess().give(() -> {
      myInfoAndProgressPanel.setStatusText(StringUtil.notNullize(text));
    });
  }

  @Override
  public void setInfo(@Nullable String text, @Nullable String requestor) {
    myInfoText = text;
  }

  @Override
  public String getInfo() {
    return myInfoText;
  }

  @Override
  public void addWidget(@Nonnull final StatusBarWidget widget) {
    UIAccess uiAccess = myApplication.getLastUIAccess();

    uiAccess.giveIfNeed(() -> addWidget(widget, Position.RIGHT, "__AUTODETECT__"));
  }

  @Override
  public void addWidget(@Nonnull final StatusBarWidget widget, @Nonnull final String anchor) {
    UIAccess uiAccess = myApplication.getLastUIAccess();

    uiAccess.giveIfNeed(() -> addWidget(widget, Position.RIGHT, anchor));
  }

  private void addWidget(@Nonnull final StatusBarWidget widget, @Nonnull final Position pos) {
    UIAccess uiAccess = myApplication.getLastUIAccess();

    uiAccess.giveIfNeed(() -> addWidget(widget, pos, "__IGNORED__"));
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

  @RequiredUIAccess
  private void addWidget(@Nonnull StatusBarWidget widget, @Nonnull Position position, @Nonnull String anchor) {
    myApplication.assertIsDispatchThread();
    PseudoComponent c = wrap(widget);
    HorizontalLayout panel = getTargetPanel(position);
    //if (position == Position.LEFT && panel.getComponentCount() == 0) {
    //  c.setBorder(SystemInfo.isMac ? JBUI.Borders.empty(2, 0, 2, 4) : JBUI.Borders.empty());
    //}
    panel.add(c/*, getPositionIndex(position, anchor)*/);
    myWidgetMap.put(widget.ID(), WidgetBean.create(widget, position, c, anchor));
    if (c instanceof StatusBarWidgetWrapper) {
      ((StatusBarWidgetWrapper)c).beforeUpdate();
    }
    widget.install(this);
    //panel.revalidate();
    Disposer.register(this, widget);

    //if (widget instanceof StatusBarWidget.Multiframe) {
    //  StatusBarWidget.Multiframe multiFrameWidget = (StatusBarWidget.Multiframe)widget;
    //  updateChildren(child -> child.addWidget(multiFrameWidget.copy(), position, anchor));
    //}
  }

  @RequiredUIAccess
  private static PseudoComponent wrap(StatusBarWidget widget) {
    if (widget instanceof CustomStatusBarWidget) {
      return () -> {
        consulo.ui.Component component = ((CustomStatusBarWidget)widget).getUIComponent();
        if (component == null) {
          String id = widget.ID();
          component = Label.create(id.substring(0, 2));
        }

        WrappedLayout layout = WrappedLayout.create(component);
        layout.addBorder(BorderPosition.LEFT, BorderStyle.EMPTY, null, 4);
        layout.addBorder(BorderPosition.RIGHT, BorderStyle.EMPTY, null, 4);
        return layout;

      };
    }
    return UnifiedStatusBarWidgetWrapper.wrap(widget);
  }

  //private int getPositionIndex(@Nonnull Position position, @Nonnull String anchor) {
  //  if (Position.RIGHT == position && myRightPanel.getComponentCount() > 0) {
  //    WidgetBean widgetAnchor = null;
  //    boolean before = false;
  //    List<String> parts = StringUtil.split(anchor, " ");
  //    if (parts.size() > 1) {
  //      widgetAnchor = myWidgetMap.get(parts.get(1));
  //      before = "before".equalsIgnoreCase(parts.get(0));
  //    }
  //    if (widgetAnchor == null) {
  //      widgetAnchor = myWidgetMap.get(IdeNotificationArea.WIDGET_ID);
  //      if (widgetAnchor == null) {
  //        widgetAnchor = myWidgetMap.get(IdeMessagePanel.FATAL_ERROR);
  //      }
  //      before = true;
  //    }
  //    if (widgetAnchor != null) {
  //      int anchorIndex = ArrayUtil.indexOf(myRightPanel.getComponents(), widgetAnchor.component);
  //      return before ? anchorIndex : anchorIndex + 1;
  //    }
  //  }
  //  return -1;
  //}

  @Nonnull
  @RequiredUIAccess
  private HorizontalLayout getTargetPanel(@Nonnull Position position) {
    if (position == Position.RIGHT) {
      return rightPanel();
    }
    if (position == Position.LEFT) {
      return leftPanel();
    }
    return centerPanel();
  }

  @RequiredUIAccess
  private HorizontalLayout rightPanel() {
    if (myRightPanel == null) {
      myRightPanel = HorizontalLayout.create();
      myComponent.right(myRightPanel);
    }
    return myRightPanel;
  }

  @RequiredUIAccess
  private HorizontalLayout leftPanel() {
    if (myLeftPanel == null) {
      myLeftPanel = HorizontalLayout.create();
      myComponent.left(myLeftPanel);
    }
    return myLeftPanel;
  }

  @RequiredUIAccess
  private HorizontalLayout centerPanel() {
    if (myCenterPanel == null) {
      myCenterPanel = HorizontalLayout.create();
      myComponent.center(myCenterPanel);
    }
    return myCenterPanel;
  }

  @Override
  public void removeWidget(@Nonnull String id) {

  }

  @Nonnull
  @Override
  public consulo.ui.Component getUIComponent() {
    return myComponent;
  }

  @Override
  public boolean isUnified() {
    return true;
  }

  @Nullable
  @Override
  public StatusBarWidget getWidget(String id) {
    WidgetBean bean = myWidgetMap.get(id);
    return bean == null ? null : bean.widget;
  }

  @Override
  public void fireNotificationPopup(@Nonnull JComponent content, Color backgroundColor) {

  }

  @Override
  public StatusBar createChild() {
    return null;
  }

  @Override
  public StatusBar findChild(Component c) {
    return null;
  }

  @Override
  public IdeFrame getFrame() {
    return myFrame;
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

  @Override
  public void dispose() {
    myWidgetMap.clear();
  }

  @Override
  public void startRefreshIndication(String tooltipText) {

  }

  @Override
  public void stopRefreshIndication() {

  }

  @Override
  public BalloonHandler notifyProgressByBalloon(@Nonnull MessageType type, @Nonnull String htmlBody) {
    return null;
  }

  @Override
  public BalloonHandler notifyProgressByBalloon(@Nonnull MessageType type, @Nonnull String htmlBody, @Nullable Image icon, @Nullable HyperlinkListener listener) {
    return null;
  }

  @Override
  public void addProgress(@Nonnull ProgressIndicatorEx indicator, @Nonnull TaskInfo info) {

  }

  @Override
  public List<Pair<TaskInfo, ProgressIndicator>> getBackgroundProcesses() {
    return null;
  }


  @Override
  public void updateWidgets() {
    for (final String s : myWidgetMap.keySet()) {
      updateWidget(s);
    }

    //updateChildren(IdeStatusBarImpl::updateWidgets);
  }

  @Override
  public void updateWidget(@Nonnull final String id) {
    UIAccess uiAccess = myApplication.getLastUIAccess();
    uiAccess.giveIfNeed(() -> {
      PseudoComponent widgetComponent = getWidgetComponent(id);
      if (widgetComponent != null) {
        if (widgetComponent instanceof StatusBarWidgetWrapper) {
          ((StatusBarWidgetWrapper)widgetComponent).beforeUpdate();
        }

        //widgetComponent.repaint();
      }

      //updateChildren(child -> child.updateWidget(id));
    });
  }

  private PseudoComponent getWidgetComponent(String id) {
    WidgetBean bean = myWidgetMap.get(id);
    return bean == null ? null : bean.component;
  }

  @Override
  public boolean isProcessWindowOpen() {
    return false;
  }

  @Override
  public void setProcessWindowOpen(boolean open) {
  }

  @Override
  public Dimension getSize() {
    return null;
  }

  @Override
  public boolean isVisible() {
    return true;
  }

  @Nullable
  @Override
  public String getInfoRequestor() {
    return null;
  }
}
