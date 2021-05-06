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
package com.intellij.openapi.wm.impl;

import com.intellij.ide.actions.ResizeToolWindowAction;
import com.intellij.ide.actions.ToggleToolbarAction;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Queryable;
import com.intellij.openapi.ui.Splitter;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.wm.*;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.ui.UIBundle;
import com.intellij.ui.components.panels.NonOpaquePanel;
import com.intellij.ui.content.Content;
import com.intellij.ui.paint.LinePainter2D;
import com.intellij.util.EventDispatcher;
import com.intellij.util.ui.JBUI;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ui.ex.ToolWindowInternalDecorator;
import consulo.util.dataholder.Key;
import consulo.wm.impl.ToolWindowManagerBase;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;

/**
 * @author Eugene Belyaev
 * @author Vladimir Kondratyev
 */
public final class DesktopInternalDecorator extends JPanel implements Queryable, DataProvider, ToolWindowInternalDecorator {

  private Project myProject;
  private WindowInfoImpl myInfo;
  private final DesktopToolWindowImpl myToolWindow;
  private final MyDivider myDivider;
  private final EventDispatcher<InternalDecoratorListener> myDispatcher = EventDispatcher.create(InternalDecoratorListener.class);
  /*
   * Actions
   */
  private final TogglePinnedModeAction myToggleAutoHideModeAction;
  private final ToggleDockModeAction myToggleDockModeAction;
  private final ToggleFloatingModeAction myToggleFloatingModeAction;
  private final ToggleWindowedModeAction myToggleWindowedModeAction;
  private final ToggleSideModeAction myToggleSideModeAction;
  private final ToggleContentUiTypeAction myToggleContentUiTypeAction;
  private final RemoveStripeButtonAction myHideStripeButtonAction;

  private ActionGroup myAdditionalGearActions;

  private DesktopToolWindowHeader myHeader;
  private ActionGroup myToggleToolbarGroup;

  DesktopInternalDecorator(final Project project, @Nonnull WindowInfoImpl info, final DesktopToolWindowImpl toolWindow, boolean dumbAware) {
    super(new BorderLayout());
    myProject = project;
    myToolWindow = toolWindow;
    myToolWindow.setDecorator(this);
    myDivider = new MyDivider();

    myToggleFloatingModeAction = new ToggleFloatingModeAction();
    myToggleWindowedModeAction = new ToggleWindowedModeAction();
    myToggleSideModeAction = new ToggleSideModeAction();
    myToggleDockModeAction = new ToggleDockModeAction();
    myToggleAutoHideModeAction = new TogglePinnedModeAction();
    myToggleContentUiTypeAction = new ToggleContentUiTypeAction();
    myHideStripeButtonAction = new RemoveStripeButtonAction();
    myToggleToolbarGroup = ToggleToolbarAction.createToggleToolbarGroup(myProject, myToolWindow);

    myHeader = new DesktopToolWindowHeader(toolWindow, () -> createPopupGroup(true)) {
      @Override
      protected void hideToolWindow() {
        fireHidden();
      }
    };

    enableEvents(AWTEvent.COMPONENT_EVENT_MASK);

    final JPanel contentPane = new JPanel(new BorderLayout());
    installFocusTraversalPolicy(contentPane, new LayoutFocusTraversalPolicy());
    contentPane.add(myHeader, BorderLayout.NORTH);

    JPanel innerPanel = new JPanel(new BorderLayout());
    JComponent toolWindowComponent = myToolWindow.getComponent();
    if (!dumbAware) {
      toolWindowComponent = DumbService.getInstance(myProject).wrapGently(toolWindowComponent, myProject);
    }
    innerPanel.add(toolWindowComponent, BorderLayout.CENTER);

    final NonOpaquePanel inner = new NonOpaquePanel(innerPanel);

    contentPane.add(inner, BorderLayout.CENTER);
    add(contentPane, BorderLayout.CENTER);
    if (SystemInfo.isMac) {
      setBackground(new JBColor(Gray._200, Gray._90));
    }

    // Add listeners
    registerKeyboardAction(new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        ToolWindowManager.getInstance(myProject).activateEditorComponent();
      }
    }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

    apply(info);

    setBorder(new InnerPanelBorder(myToolWindow));
  }

  @Override
  public boolean isFocused() {
    IdeFocusManager fm = IdeFocusManager.getInstance(myProject);
    Component component = fm.getFocusedDescendantFor(myToolWindow.getComponent());
    if (component != null) return true;

    Component owner = fm.getLastFocusedFor(WindowManager.getInstance().getIdeFrame(myProject));

    return owner != null && SwingUtilities.isDescendingFrom(owner, myToolWindow.getComponent());
  }

  /**
   * Applies specified decoration.
   */
  @Override
  public void apply(@Nonnull WindowInfo info) {
    if (Comparing.equal(myInfo, info) || myProject == null || myProject.isDisposed()) {
      return;
    }
    myInfo = (WindowInfoImpl)info;

    // Anchor
    final ToolWindowAnchor anchor = myInfo.getAnchor();
    if (info.isSliding()) {
      myDivider.invalidate();
      if (ToolWindowAnchor.TOP == anchor) {
        add(myDivider, BorderLayout.SOUTH);
      }
      else if (ToolWindowAnchor.LEFT == anchor) {
        add(myDivider, BorderLayout.EAST);
      }
      else if (ToolWindowAnchor.BOTTOM == anchor) {
        add(myDivider, BorderLayout.NORTH);
      }
      else if (ToolWindowAnchor.RIGHT == anchor) {
        add(myDivider, BorderLayout.WEST);
      }
      myDivider.setPreferredSize(new Dimension(0, 0));
    }
    else { // docked and floating windows don't have divider
      remove(myDivider);
    }

    validate();
    repaint();


    // Push "apply" request forward

    if (myInfo.isFloating() && myInfo.isVisible()) {
      final DesktopFloatingDecorator floatingDecorator = (DesktopFloatingDecorator)SwingUtilities.getAncestorOfClass(DesktopFloatingDecorator.class, this);
      if (floatingDecorator != null) {
        floatingDecorator.apply(myInfo);
      }
    }

    myToolWindow.getContentUI().setType(myInfo.getContentUiType());
  }

  @javax.annotation.Nullable
  @Override
  public Object getData(@Nonnull @NonNls Key<?> dataId) {
    if (PlatformDataKeys.TOOL_WINDOW == dataId) {
      return myToolWindow;
    }
    return null;
  }

  @Override
  public void addInternalDecoratorListener(InternalDecoratorListener l) {
    myDispatcher.addListener(l);
  }

  @Override
  public void removeInternalDecoratorListener(InternalDecoratorListener l) {
    myDispatcher.removeListener(l);
  }

  @Override
  public void dispose() {
    removeAll();

    Disposer.dispose(myHeader);
    myHeader = null;
    myProject = null;
  }

  private void fireAnchorChanged(@Nonnull ToolWindowAnchor anchor) {
    myDispatcher.getMulticaster().anchorChanged(this, anchor);
  }

  private void fireAutoHideChanged(boolean autoHide) {
    myDispatcher.getMulticaster().autoHideChanged(this, autoHide);
  }

  /**
   * Fires event that "hide" button has been pressed.
   */
  @Override
  public void fireHidden() {
    myDispatcher.getMulticaster().hidden(this);
  }

  /**
   * Fires event that "hide" button has been pressed.
   */
  @Override
  public void fireHiddenSide() {
    myDispatcher.getMulticaster().hiddenSide(this);
  }

  /**
   * Fires event that user performed click into the title bar area.
   */
  @Override
  public void fireActivated() {
    myDispatcher.getMulticaster().activated(this);
  }

  private void fireTypeChanged(@Nonnull ToolWindowType type) {
    myDispatcher.getMulticaster().typeChanged(this, type);
  }

  final void fireResized() {
    myDispatcher.getMulticaster().resized(this);
  }

  private void fireSideStatusChanged(boolean isSide) {
    myDispatcher.getMulticaster().sideStatusChanged(this, isSide);
  }

  private void fireContentUiTypeChanges(@Nonnull ToolWindowContentUiType type) {
    myDispatcher.getMulticaster().contentUiTypeChanges(this, type);
  }

  private void fireVisibleOnPanelChanged(final boolean visibleOnPanel) {
    myDispatcher.getMulticaster().visibleStripeButtonChanged(this, visibleOnPanel);
  }

  @Override
  public void setTitleActions(AnAction[] actions) {
    myHeader.setAdditionalTitleActions(actions);
  }

  @Override
  public void setTabActions(AnAction... actions) {
    myHeader.setTabActions(actions);
  }

  private class InnerPanelBorder implements Border {

    private final ToolWindow myWindow;

    private InnerPanelBorder(ToolWindow window) {
      myWindow = window;
    }

    @Override
    public void paintBorder(final Component c, final Graphics g, final int x, final int y, final int width, final int height) {
      g.setColor(JBColor.border());
      doPaintBorder(c, g, x, y, width, height);
    }

    private void doPaintBorder(Component c, Graphics g, int x, int y, int width, int height) {
      Insets insets = getBorderInsets(c);

      Graphics2D graphics2D = (Graphics2D)g;
      if (insets.top > 0) {
        LinePainter2D.paint(graphics2D, x, y + insets.top - 1, x + width - 1, y + insets.top - 1);
        LinePainter2D.paint(graphics2D, x, y + insets.top, x + width - 1, y + insets.top);
      }

      if (insets.left > 0) {
        LinePainter2D.paint(graphics2D, x, y, x, y + height);
        LinePainter2D.paint(graphics2D, x + 1, y, x + 1, y + height);
      }

      if (insets.right > 0) {
        LinePainter2D.paint(graphics2D, x + width - 1, y + insets.top, x + width - 1, y + height);
        LinePainter2D.paint(graphics2D, x + width, y + insets.top, x + width, y + height);
      }

      if (insets.bottom > 0) {
        LinePainter2D.paint(graphics2D, x, y + height - 1, x + width, y + height - 1);
        LinePainter2D.paint(graphics2D, x, y + height, x + width, y + height);
      }
    }

    @Override
    public Insets getBorderInsets(final Component c) {
      if (myProject == null) return JBUI.emptyInsets();
      ToolWindowManager toolWindowManager =  ToolWindowManager.getInstance(myProject);
      if (!(toolWindowManager instanceof ToolWindowManagerBase)
          || !((ToolWindowManagerBase)toolWindowManager).isToolWindowRegistered(myInfo.getId())
          || myWindow.getType() == ToolWindowType.FLOATING) {
        return JBUI.emptyInsets();
      }
      ToolWindowAnchor anchor = myWindow.getAnchor();
      Component component = myWindow.getComponent();
      Container parent = component.getParent();
      while(parent != null) {
        if (parent instanceof Splitter) {
          Splitter splitter = (Splitter)parent;
          boolean isFirst = splitter.getFirstComponent() == component;
          boolean isVertical = splitter.isVertical();
          return new Insets(0,
                            anchor == ToolWindowAnchor.RIGHT || (!isVertical && !isFirst) ? 1 : 0,
                            (isVertical && isFirst) ? 1 : 0,
                            anchor == ToolWindowAnchor.LEFT || (!isVertical && isFirst) ? 1 : 0);
        }
        component = parent;
        parent = component.getParent();
      }
      return new Insets(0, anchor == ToolWindowAnchor.RIGHT ? 1 : 0, anchor == ToolWindowAnchor.TOP ? 1 : 0, anchor == ToolWindowAnchor.LEFT ? 1 : 0);
    }

    @Override
    public boolean isBorderOpaque() {
      return false;
    }
  }

  @Override
  public final ActionGroup createPopupGroup() {
    return createPopupGroup(false);
  }

  public final ActionGroup createPopupGroup(boolean skipHideAction) {
    final DefaultActionGroup group = createGearPopupGroup();
    if (!ToolWindowId.PREVIEW.equals(myInfo.getId())) {
      group.add(myToggleContentUiTypeAction);
    }

    final DefaultActionGroup moveGroup = new DefaultActionGroup(UIBundle.message("tool.window.move.to.action.group.name"), true);
    final ToolWindowAnchor anchor = myInfo.getAnchor();
    if (anchor != ToolWindowAnchor.TOP) {
      final AnAction topAction = new ChangeAnchorAction(UIBundle.message("tool.window.move.to.top.action.name"), ToolWindowAnchor.TOP);
      moveGroup.add(topAction);
    }
    if (anchor != ToolWindowAnchor.LEFT) {
      final AnAction leftAction = new ChangeAnchorAction(UIBundle.message("tool.window.move.to.left.action.name"), ToolWindowAnchor.LEFT);
      moveGroup.add(leftAction);
    }
    if (anchor != ToolWindowAnchor.BOTTOM) {
      final AnAction bottomAction =
              new ChangeAnchorAction(UIBundle.message("tool.window.move.to.bottom.action.name"), ToolWindowAnchor.BOTTOM);
      moveGroup.add(bottomAction);
    }
    if (anchor != ToolWindowAnchor.RIGHT) {
      final AnAction rightAction =
              new ChangeAnchorAction(UIBundle.message("tool.window.move.to.right.action.name"), ToolWindowAnchor.RIGHT);
      moveGroup.add(rightAction);
    }
    group.add(moveGroup);

    DefaultActionGroup resize = new DefaultActionGroup(ActionsBundle.groupText("ResizeToolWindowGroup"), true);
    resize.add(new ResizeToolWindowAction.Left(myToolWindow, this));
    resize.add(new ResizeToolWindowAction.Right(myToolWindow, this));
    resize.add(new ResizeToolWindowAction.Up(myToolWindow, this));
    resize.add(new ResizeToolWindowAction.Down(myToolWindow, this));
    resize.add(ActionManager.getInstance().getAction("MaximizeToolWindow"));

    group.add(resize);
    if (!skipHideAction) {
      group.addSeparator();
      group.add(new HideAction());
    }
    return group;
  }

  private DefaultActionGroup createGearPopupGroup() {
    final DefaultActionGroup group = new DefaultActionGroup();

    if (myAdditionalGearActions != null) {
      if (myAdditionalGearActions.isPopup() && !StringUtil.isEmpty(myAdditionalGearActions.getTemplatePresentation().getText())) {
        group.add(myAdditionalGearActions);
      }
      else {
        addSorted(group, myAdditionalGearActions);
      }
      group.addSeparator();
    }
    group.addAction(myToggleToolbarGroup).setAsSecondary(true);
    if (myInfo.isDocked()) {
      group.add(myToggleAutoHideModeAction);
      group.add(myToggleDockModeAction);
      group.add(myToggleFloatingModeAction);
      group.add(myToggleWindowedModeAction);
      group.add(myToggleSideModeAction);
    }
    else if (myInfo.isFloating()) {
      group.add(myToggleAutoHideModeAction);
      group.add(myToggleFloatingModeAction);
      group.add(myToggleWindowedModeAction);
    }
    else if (myInfo.isWindowed()) {
      group.add(myToggleFloatingModeAction);
      group.add(myToggleWindowedModeAction);
    }
    else if (myInfo.isSliding()) {
      if (!ToolWindowId.PREVIEW.equals(myInfo.getId())) {
        group.add(myToggleDockModeAction);
      }
      group.add(myToggleFloatingModeAction);
      group.add(myToggleWindowedModeAction);
      group.add(myToggleSideModeAction);
    }
    group.add(myHideStripeButtonAction);
    return group;
  }

  private static void addSorted(DefaultActionGroup main, ActionGroup group) {
    final AnAction[] children = group.getChildren(null);
    boolean hadSecondary = false;
    for (AnAction action : children) {
      if (group.isPrimary(action)) {
        main.add(action);
      } else {
        hadSecondary = true;
      }
    }
    if (hadSecondary) {
      main.addSeparator();
      for (AnAction action : children) {
        if (!group.isPrimary(action)) {
          main.addAction(action).setAsSecondary(true);
        }
      }
    }
    String separatorText = group.getTemplatePresentation().getText();
    if (children.length > 0 && !StringUtil.isEmpty(separatorText)) {
      main.addAction(new AnSeparator(separatorText), Constraints.FIRST);
    }
  }

  /**
   * @return tool window associated with the decorator.
   */
  @Override
  public DesktopToolWindowImpl getToolWindow() {
    return myToolWindow;
  }

  /**
   * @return last window info applied to the decorator.
   */
  @Override
  @Nonnull
  public WindowInfoImpl getWindowInfo() {
    return myInfo;
  }

  public int getHeaderHeight() {
    return myHeader.getPreferredSize().height;
  }

  public void setHeaderVisible(boolean value) {
    myHeader.setVisible(value);
  }

  @Override
  protected final void processComponentEvent(final ComponentEvent e) {
    super.processComponentEvent(e);
    if (ComponentEvent.COMPONENT_RESIZED == e.getID()) {
      fireResized();
    }
  }

  private final class ChangeAnchorAction extends AnAction implements DumbAware {
    @Nonnull
    private final ToolWindowAnchor myAnchor;

    public ChangeAnchorAction(@Nonnull String title, @Nonnull ToolWindowAnchor anchor) {
      super(title);
      myAnchor = anchor;
    }

    @Override
    public final void actionPerformed(@Nonnull final AnActionEvent e) {
      fireAnchorChanged(myAnchor);
    }
  }

  private final class TogglePinnedModeAction extends ToggleAction implements DumbAware {
    public TogglePinnedModeAction() {
      copyFrom(ActionManager.getInstance().getAction(TOGGLE_PINNED_MODE_ACTION_ID));
    }

    @Override
    public final boolean isSelected(final AnActionEvent event) {
      return !myInfo.isAutoHide();
    }

    @Override
    public final void setSelected(final AnActionEvent event, final boolean flag) {
      fireAutoHideChanged(!myInfo.isAutoHide());
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      super.update(e);
      e.getPresentation().setVisible(myInfo.getType() != ToolWindowType.FLOATING && myInfo.getType() != ToolWindowType.WINDOWED);
    }
  }

  private final class ToggleDockModeAction extends ToggleAction implements DumbAware {
    public ToggleDockModeAction() {
      copyFrom(ActionManager.getInstance().getAction(TOGGLE_DOCK_MODE_ACTION_ID));
    }

    @Override
    public final boolean isSelected(final AnActionEvent event) {
      return myInfo.isDocked();
    }

    @Override
    public final void setSelected(final AnActionEvent event, final boolean flag) {
      if (myInfo.isDocked()) {
        fireTypeChanged(ToolWindowType.SLIDING);
      }
      else if (myInfo.isSliding()) {
        fireTypeChanged(ToolWindowType.DOCKED);
      }
    }
  }

  private final class ToggleFloatingModeAction extends ToggleAction implements DumbAware {
    public ToggleFloatingModeAction() {
      copyFrom(ActionManager.getInstance().getAction(TOGGLE_FLOATING_MODE_ACTION_ID));
    }

    @Override
    public final boolean isSelected(final AnActionEvent event) {
      return myInfo.isFloating();
    }

    @Override
    public final void setSelected(final AnActionEvent event, final boolean flag) {
      if (myInfo.isFloating()) {
        fireTypeChanged(myInfo.getInternalType());
      }
      else {
        fireTypeChanged(ToolWindowType.FLOATING);
      }
    }
  }

  private final class ToggleWindowedModeAction extends ToggleAction implements DumbAware {
    public ToggleWindowedModeAction() {
      copyFrom(ActionManager.getInstance().getAction(TOGGLE_WINDOWED_MODE_ACTION_ID));
    }

    @Override
    public final boolean isSelected(final AnActionEvent event) {
      return myInfo.isWindowed();
    }

    @Override
    public final void setSelected(final AnActionEvent event, final boolean flag) {
      if (myInfo.isWindowed()) {
        fireTypeChanged(myInfo.getInternalType());
      }
      else {
        fireTypeChanged(ToolWindowType.WINDOWED);
      }
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      super.update(e);
      if (SystemInfo.isMac) {
        e.getPresentation().setEnabledAndVisible(false);
      }
    }
  }

  private final class ToggleSideModeAction extends ToggleAction implements DumbAware {
    public ToggleSideModeAction() {
      copyFrom(ActionManager.getInstance().getAction(TOGGLE_SIDE_MODE_ACTION_ID));
    }

    @Override
    public final boolean isSelected(final AnActionEvent event) {
      return myInfo.isSplit();
    }

    @Override
    public final void setSelected(final AnActionEvent event, final boolean flag) {
      fireSideStatusChanged(flag);
    }

    @Override
    public void update(@Nonnull final AnActionEvent e) {
      super.update(e);
    }
  }

  private final class RemoveStripeButtonAction extends AnAction implements DumbAware {
    public RemoveStripeButtonAction() {
      Presentation presentation = getTemplatePresentation();
      presentation.setText(ActionsBundle.message("action.RemoveStripeButton.text"));
      presentation.setDescription(ActionsBundle.message("action.RemoveStripeButton.description"));
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      e.getPresentation().setEnabledAndVisible(myInfo.isShowStripeButton());
    }

    @Override
    public void actionPerformed(AnActionEvent e) {
      fireVisibleOnPanelChanged(false);
      if (getToolWindow().isActive()) {
        fireHidden();
      }
    }
  }

  private final class HideAction extends AnAction implements DumbAware {
    @NonNls public static final String HIDE_ACTIVE_WINDOW_ACTION_ID = DesktopInternalDecorator.HIDE_ACTIVE_WINDOW_ACTION_ID;

    public HideAction() {
      copyFrom(ActionManager.getInstance().getAction(HIDE_ACTIVE_WINDOW_ACTION_ID));
      getTemplatePresentation().setText(UIBundle.message("tool.window.hide.action.name"));
    }

    @Override
    public final void actionPerformed(@Nonnull final AnActionEvent e) {
      fireHidden();
    }

    @Override
    public final void update(@Nonnull final AnActionEvent event) {
      final Presentation presentation = event.getPresentation();
      presentation.setEnabled(myInfo.isVisible());
    }
  }


  private final class ToggleContentUiTypeAction extends ToggleAction implements DumbAware {
    private boolean myHadSeveralContents;

    private ToggleContentUiTypeAction() {
      copyFrom(ActionManager.getInstance().getAction(TOGGLE_CONTENT_UI_TYPE_ACTION_ID));
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      myHadSeveralContents = myHadSeveralContents || myToolWindow.getContentManager().getContentCount() > 1;
      super.update(e);
      e.getPresentation().setVisible(myHadSeveralContents);
    }

    @Override
    public boolean isSelected(AnActionEvent e) {
      return myInfo.getContentUiType() == ToolWindowContentUiType.COMBO;
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
      fireContentUiTypeChanges(state ? ToolWindowContentUiType.COMBO : ToolWindowContentUiType.TABBED);
    }
  }

  private final class MyDivider extends JPanel {
    private boolean myDragging;
    private Point myLastPoint;
    private Disposable myDisposable;
    private IdeGlassPane myGlassPane;

    private final MouseAdapter myListener = new MyMouseAdapter();

    @Override
    public void addNotify() {
      super.addNotify();
      myGlassPane = IdeGlassPaneUtil.find(this);
      myDisposable = Disposable.newDisposable();
      myGlassPane.addMouseMotionPreprocessor(myListener, myDisposable);
      myGlassPane.addMousePreprocessor(myListener, myDisposable);
    }

    @Override
    public void removeNotify() {
      super.removeNotify();
      if (myDisposable != null && !Disposer.isDisposed(myDisposable)) {
        Disposer.dispose(myDisposable);
      }
    }

    boolean isInDragZone(MouseEvent e) {
      final Point p = SwingUtilities.convertMouseEvent(e.getComponent(), e, this).getPoint();
      return Math.abs(myInfo.getAnchor().isHorizontal() ? p.y : p.x) < 6;
    }


    private class MyMouseAdapter extends MouseAdapter {

      private void updateCursor(MouseEvent e) {
        if (isInDragZone(e)) {
          myGlassPane.setCursor(MyDivider.this.getCursor(), MyDivider.this);
          e.consume();
        }
      }

      @Override
      public void mousePressed(MouseEvent e) {
        myDragging = isInDragZone(e);
        updateCursor(e);
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        updateCursor(e);
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        updateCursor(e);
        myDragging = false;
      }

      @Override
      public void mouseMoved(MouseEvent e) {
        updateCursor(e);
      }

      @Override
      public void mouseDragged(MouseEvent e) {
        if (!myDragging) return;
        MouseEvent event = SwingUtilities.convertMouseEvent(e.getComponent(), e, MyDivider.this);
        final ToolWindowAnchor anchor = myInfo.getAnchor();
        final Point point = event.getPoint();
        final Container windowPane = DesktopInternalDecorator.this.getParent();
        myLastPoint = SwingUtilities.convertPoint(MyDivider.this, point, windowPane);
        myLastPoint.x = Math.min(Math.max(myLastPoint.x, 0), windowPane.getWidth());
        myLastPoint.y = Math.min(Math.max(myLastPoint.y, 0), windowPane.getHeight());

        final Rectangle bounds = DesktopInternalDecorator.this.getBounds();
        if (anchor == ToolWindowAnchor.TOP) {
          DesktopInternalDecorator.this.setBounds(0, 0, bounds.width, myLastPoint.y);
        }
        else if (anchor == ToolWindowAnchor.LEFT) {
          DesktopInternalDecorator.this.setBounds(0, 0, myLastPoint.x, bounds.height);
        }
        else if (anchor == ToolWindowAnchor.BOTTOM) {
          DesktopInternalDecorator.this.setBounds(0, myLastPoint.y, bounds.width, windowPane.getHeight() - myLastPoint.y);
        }
        else if (anchor == ToolWindowAnchor.RIGHT) {
          DesktopInternalDecorator.this.setBounds(myLastPoint.x, 0, windowPane.getWidth() - myLastPoint.x, bounds.height);
        }
        DesktopInternalDecorator.this.validate();
        e.consume();
      }
    }

    @Nonnull
    @Override
    public Cursor getCursor() {
      final boolean isVerticalCursor = myInfo.isDocked() ? myInfo.getAnchor().isSplitVertically() : myInfo.getAnchor().isHorizontal();
      return isVerticalCursor ? Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR) : Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR);
    }
  }

  @Override
  public void putInfo(@Nonnull Map<String, String> info) {
    info.put("toolWindowTitle", myToolWindow.getTitle());

    final Content selection = myToolWindow.getContentManager().getSelectedContent();
    if (selection != null) {
      info.put("toolWindowTab", selection.getTabName());
    }
  }

  @Override
  public void setAdditionalGearActions(@Nullable ActionGroup additionalGearActions) {
    myAdditionalGearActions = additionalGearActions;
  }

  /**
   * Installs a focus traversal policy for the tool window.
   * If the policy cannot handle a keystroke, it delegates the handling to
   * the nearest ancestors focus traversal policy. For instance,
   * this policy does not handle KeyEvent.VK_ESCAPE, so it can delegate the handling
   * to a ThreeComponentSplitter instance.
   */
  static void installFocusTraversalPolicy(@Nonnull Container container, @Nonnull FocusTraversalPolicy policy) {
    container.setFocusCycleRoot(true);
    container.setFocusTraversalPolicyProvider(true);
    container.setFocusTraversalPolicy(policy);
    installDefaultFocusTraversalKeys(container, KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
    installDefaultFocusTraversalKeys(container, KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS);
  }

  private static void installDefaultFocusTraversalKeys(@Nonnull Container container, int id) {
    container.setFocusTraversalKeys(id, KeyboardFocusManager.getCurrentKeyboardFocusManager().getDefaultFocusTraversalKeys(id));
  }
}
