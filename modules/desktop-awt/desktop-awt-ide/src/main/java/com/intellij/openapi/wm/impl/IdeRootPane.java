/*
 * Copyright 2000-2017 JetBrains s.r.o.
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
package com.intellij.openapi.wm.impl;

import com.intellij.ide.DataManager;
import com.intellij.ide.actions.CustomizeUIAction;
import com.intellij.ide.actions.ViewToolbarAction;
import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.UISettingsListener;
import com.intellij.ide.ui.customization.CustomActionsSchema;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.wm.IdeFrame;
import com.intellij.openapi.wm.IdeRootPaneNorthExtension;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.ex.IdeFrameEx;
import com.intellij.openapi.wm.ex.WindowManagerEx;
import com.intellij.openapi.wm.impl.status.IdeStatusBarImpl;
import com.intellij.ui.BalloonLayout;
import com.intellij.ui.DesktopBalloonLayoutImpl;
import com.intellij.ui.PopupHandler;
import com.intellij.ui.ScreenUtil;
import com.intellij.ui.components.JBLayeredPane;
import com.intellij.ui.components.JBPanel;
import com.intellij.util.ui.JBUI;
import consulo.desktop.wm.impl.DesktopIdeFrameUtil;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.decorator.SwingUIDecorator;
import consulo.ui.ex.ToolWindowPanel;

import javax.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseMotionAdapter;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public class IdeRootPane extends JRootPane implements Disposable, UISettingsListener {
  /**
   * Toolbar and status bar.
   */
  private JComponent myToolbar;
  private IdeStatusBarImpl myStatusBar;
  private boolean myStatusBarDisposed;

  private final Box myNorthPanel = Box.createVerticalBox();
  private final List<IdeRootPaneNorthExtension> myNorthComponents = new ArrayList<>();

  /**
   * Current {@code ToolWindowPanel}. If there is no such pane then this field is null.
   */
  private ToolWindowPanel myToolWindowsPane;
  private JBPanel myContentPane;
  private final ActionManager myActionManager;

  private final boolean myGlassPaneInitialized;

  private boolean myFullScreen;

  public IdeRootPane(ActionManager actionManager, DataManager dataManager, Application application, final IdeFrame frame) {
    myActionManager = actionManager;

    myContentPane.add(myNorthPanel, BorderLayout.NORTH);

    myContentPane.addMouseMotionListener(new MouseMotionAdapter() {}); // listen to mouse motion events for a11y

    createStatusBar(frame);

    updateStatusBarVisibility();

    myContentPane.add(myStatusBar, BorderLayout.SOUTH);

    if (WindowManagerEx.getInstanceEx().isFloatingMenuBarSupported()) {
      menuBar = new IdeMenuBar(actionManager, dataManager);
      getLayeredPane().add(menuBar, new Integer(JLayeredPane.DEFAULT_LAYER - 1));
      if (frame instanceof IdeFrameEx) {
        addPropertyChangeListener(WindowManagerEx.FULL_SCREEN, __ -> myFullScreen = ((IdeFrameEx)frame).isInFullScreen());
      }
    }
    else {
      setJMenuBar(new IdeMenuBar(actionManager, dataManager));
    }

    IdeGlassPaneImpl glassPane = new IdeGlassPaneImpl(this, true);
    setGlassPane(glassPane);
    myGlassPaneInitialized = true;
    SwingUIDecorator.apply(SwingUIDecorator::decorateWindowTitle, this);
    glassPane.setVisible(false);
  }

  @Override
  protected LayoutManager createRootLayout() {
    return WindowManagerEx.getInstanceEx().isFloatingMenuBarSupported() ? new MyRootLayout() : super.createRootLayout();
  }

  @Override
  public void setGlassPane(final Component glass) {
    if (myGlassPaneInitialized) throw new IllegalStateException("Setting of glass pane for IdeFrame is prohibited");
    super.setGlassPane(glass);
  }


  /**
   * Invoked when enclosed frame is being shown.
   */
  @Override
  public final void addNotify(){
    super.addNotify();
  }

  /**
   * Invoked when enclosed frame is being disposed.
   */
  @Override
  public final void removeNotify(){
    if (ScreenUtil.isStandardAddRemoveNotify(this)) {
      if (!myStatusBarDisposed) {
        myStatusBarDisposed = true;
        Disposer.dispose(myStatusBar);
      }
      removeToolbar();
      setJMenuBar(null);
    }
    super.removeNotify();
  }

  /**
   * Sets current tool windows pane (panel where all tool windows are located).
   * If {@code toolWindowsPane} is {@code null} then the method just removes
   * the current tool windows pane.
   */
  public final void setToolWindowsPane(@Nullable final ToolWindowPanel toolWindowsPane) {
    final JComponent contentPane = (JComponent)getContentPane();
    if(myToolWindowsPane != null){
      contentPane.remove((Component)myToolWindowsPane);
    }

    myToolWindowsPane = toolWindowsPane;
    if(myToolWindowsPane != null) {
      contentPane.add((Component)myToolWindowsPane, BorderLayout.CENTER);
    }

    contentPane.revalidate();
  }

  @Override
  protected JLayeredPane createLayeredPane() {
    JLayeredPane p = new JBLayeredPane();
    p.setName(getName() + ".layeredPane");
    return p;
  }

  @Override
  protected final Container createContentPane(){
    return myContentPane = new IdePanePanel(new BorderLayout());
  }

  public void updateToolbar() {
    removeToolbar();
    myToolbar = createToolbar();
    myNorthPanel.add(myToolbar, 0);
    updateToolbarVisibility();
    myContentPane.revalidate();
  }

  private void removeToolbar() {
    if (myToolbar != null) {
      myNorthPanel.remove(myToolbar);
      myToolbar = null;
    }
  }

  public void updateNorthComponents() {
    for (IdeRootPaneNorthExtension northComponent : myNorthComponents) {
      northComponent.revalidate();
    }
    myContentPane.revalidate();
  }

  public void updateMainMenuActions(){
    ((IdeMenuBar)menuBar).updateMenuActions();
    menuBar.repaint();
  }

  private JComponent createToolbar() {
    ActionGroup group = (ActionGroup)CustomActionsSchema.getInstance().getCorrectedAction(IdeActions.GROUP_MAIN_TOOLBAR);
    final ActionToolbar toolBar= myActionManager.createActionToolbar(
            ActionPlaces.MAIN_TOOLBAR,
            group,
            true
    );
    toolBar.setLayoutPolicy(ActionToolbar.WRAP_LAYOUT_POLICY);
    toolBar.setTargetComponent(null);

    DefaultActionGroup menuGroup = new DefaultActionGroup();
    menuGroup.add(new ViewToolbarAction());
    menuGroup.add(new CustomizeUIAction());
    PopupHandler.installUnknownPopupHandler(toolBar.getComponent(), menuGroup, myActionManager);

    return toolBar.getComponent();
  }

  private void createStatusBar(IdeFrame frame) {
    myStatusBar = new IdeStatusBarImpl();
    Disposer.register(this, myStatusBar);
    myStatusBar.install(frame);
  }

  @Nullable
  public final StatusBar getStatusBar() {
    return myStatusBar;
  }

  public int getStatusBarHeight() {
    return myStatusBar.isVisible() ? myStatusBar.getHeight() : 0;
  }

  private void updateToolbarVisibility(){
    myToolbar.setVisible(UISettings.getInstance().getShowMainToolbar() && !UISettings.getInstance().getPresentationMode());
  }

  private void updateStatusBarVisibility(){
    myStatusBar.setVisible(UISettings.getInstance().getShowStatusBar() && !UISettings.getInstance().getPresentationMode());
  }

  public void installNorthComponents(final Project project) {
    myNorthComponents.addAll(IdeRootPaneNorthExtension.EP_NAME.getExtensionList(project));
    for (IdeRootPaneNorthExtension northComponent : myNorthComponents) {
      myNorthPanel.add(northComponent.getComponent());
      northComponent.uiSettingsChanged(UISettings.getInstance());
    }
  }

  public void deinstallNorthComponents(){
    for (IdeRootPaneNorthExtension northComponent : myNorthComponents) {
      myNorthPanel.remove(northComponent.getComponent());
      Disposer.dispose(northComponent);
    }
    myNorthComponents.clear();
  }

  public IdeRootPaneNorthExtension findByName(String name) {
    for (IdeRootPaneNorthExtension northComponent : myNorthComponents) {
      if (Comparing.strEqual(name, northComponent.getKey())) {
        return northComponent;
      }
    }
    return null;
  }

  @Override
  public void uiSettingsChanged(UISettings uiSettings) {
    SwingUIDecorator.apply(SwingUIDecorator::decorateWindowTitle, this);
    updateToolbarVisibility();
    updateStatusBarVisibility();
    for (IdeRootPaneNorthExtension component : myNorthComponents) {
      component.uiSettingsChanged(uiSettings);
    }
    IdeFrame frame = DesktopIdeFrameUtil.findIdeFrameFromParent(this);
    BalloonLayout layout = frame != null ? frame.getBalloonLayout() : null;
    if (layout instanceof DesktopBalloonLayoutImpl) ((DesktopBalloonLayoutImpl)layout).queueRelayout();
  }

  @Override
  public void dispose() {
  }

  private class MyRootLayout extends RootLayout {
    @Override
    public Dimension preferredLayoutSize(Container parent) {
      Dimension rd;
      Insets i = getInsets();

      if (contentPane != null) {
        rd = contentPane.getPreferredSize();
      }
      else {
        rd = parent.getSize();
      }
      Dimension mbd;
      if (menuBar != null && menuBar.isVisible() && !myFullScreen) {
        mbd = menuBar.getPreferredSize();
      }
      else {
        mbd = JBUI.emptySize();
      }
      return new Dimension(Math.max(rd.width, mbd.width) + i.left + i.right,
                           rd.height + mbd.height + i.top + i.bottom);
    }

    @Override
    public Dimension minimumLayoutSize(Container parent) {
      Dimension rd;
      Insets i = getInsets();
      if (contentPane != null) {
        rd = contentPane.getMinimumSize();
      }
      else {
        rd = parent.getSize();
      }
      Dimension mbd;
      if (menuBar != null && menuBar.isVisible() && !myFullScreen) {
        mbd = menuBar.getMinimumSize();
      }
      else {
        mbd = JBUI.emptySize();
      }
      return new Dimension(Math.max(rd.width, mbd.width) + i.left + i.right,
                           rd.height + mbd.height + i.top + i.bottom);
    }

    @Override
    public Dimension maximumLayoutSize(Container target) {
      Dimension mbd;
      Insets i = getInsets();
      if (menuBar != null && menuBar.isVisible() && !myFullScreen) {
        mbd = menuBar.getMaximumSize();
      }
      else {
        mbd = JBUI.emptySize();
      }
      Dimension rd;
      if (contentPane != null) {
        rd = contentPane.getMaximumSize();
      }
      else {
        rd = new Dimension(Integer.MAX_VALUE,
                           Integer.MAX_VALUE - i.top - i.bottom - mbd.height - 1);
      }
      return new Dimension(Math.min(rd.width, mbd.width) + i.left + i.right,
                           rd.height + mbd.height + i.top + i.bottom);
    }

    @Override
    public void layoutContainer(Container parent) {
      Rectangle b = parent.getBounds();
      Insets i = getInsets();
      int w = b.width - i.right - i.left;
      int h = b.height - i.top - i.bottom;

      if (layeredPane != null) {
        layeredPane.setBounds(i.left, i.top, w, h);
      }
      if (glassPane != null) {
        glassPane.setBounds(i.left, i.top, w, h);
      }
      int contentY = 0;
      if (menuBar != null && menuBar.isVisible()) {
        Dimension mbd = menuBar.getPreferredSize();
        menuBar.setBounds(0, 0, w, mbd.height);
        if (!myFullScreen) {
          contentY += mbd.height;
        }
      }
      if (contentPane != null) {
        contentPane.setBounds(0, contentY, w, h - contentY);
      }
    }
  }
}
