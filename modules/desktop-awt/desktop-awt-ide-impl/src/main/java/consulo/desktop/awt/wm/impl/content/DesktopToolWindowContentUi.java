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
package consulo.desktop.awt.wm.impl.content;

import consulo.dataContext.DataProvider;
import consulo.desktop.awt.wm.impl.DesktopToolWindowImpl;
import consulo.desktop.awt.wm.impl.DesktopToolWindowManagerImpl;
import consulo.disposer.Disposer;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.ide.impl.idea.ide.actions.CloseAction;
import consulo.ide.impl.idea.ide.actions.ShowContentAction;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionManagerImpl;
import consulo.ide.impl.idea.openapi.actionSystem.impl.MenuItemPresentationFactory;
import consulo.ide.impl.idea.openapi.ui.ThreeComponentsSplitter;
import consulo.ui.ex.content.TabbedContent;
import consulo.ide.impl.idea.ui.content.tabs.PinToolwindowTabAction;
import consulo.ide.impl.idea.ui.content.tabs.TabbedContentAction;
import consulo.ide.impl.idea.ui.popup.PopupState;
import consulo.ui.ex.content.ContentUtilEx;
import consulo.ide.impl.wm.impl.ToolWindowContentUI;
import consulo.project.ui.wm.IdeFrame;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.PopupHandler;
import consulo.ui.ex.awt.Splitter;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.ui.ex.popup.JBPopup;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.ex.toolWindow.ToolWindowContentUiType;
import consulo.ui.ex.toolWindow.ToolWindowType;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.util.lang.ref.Ref;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;
import org.jetbrains.annotations.NonNls;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

public class DesktopToolWindowContentUi extends JPanel implements ToolWindowContentUI, PropertyChangeListener, DataProvider {
  class MyLayoutManager extends FlowLayout {
    MyLayoutManager() {
      super(FlowLayout.CENTER, 0, 0);
    }

    @Override
    public void layoutContainer(Container target) {
      getCurrentLayout().layout();
    }

    @Override
    public Dimension minimumLayoutSize(Container target) {
      return calcSize(target, Component::getMinimumSize);
    }

    @Override
    public Dimension preferredLayoutSize(Container target) {
      return calcSize(target, Component::getPreferredSize);
    }

    @Nonnull
    private Dimension calcSize(Container target, Function<Component, Dimension> dimensionSupplier) {
      synchronized (target.getTreeLock()) {
        Dimension dim = new Dimension(0, 0);
        int nmembers = target.getComponentCount();
        boolean firstVisibleComponent = true;
        boolean useBaseline = getAlignOnBaseline();
        int maxAscent = 0;
        int maxDescent = 0;

        for (int i = 0; i < nmembers; i++) {
          Component m = target.getComponent(i);
          if (m.isVisible()) {
            Dimension d = dimensionSupplier.apply(m);
            dim.height = Math.max(dim.height, d.height);
            if (firstVisibleComponent) {
              firstVisibleComponent = false;
            }
            else {
              dim.width += 0;
            }
            dim.width += d.width;
            if (useBaseline) {
              int baseline = m.getBaseline(d.width, d.height);
              if (baseline >= 0) {
                maxAscent = Math.max(maxAscent, baseline);
                maxDescent = Math.max(maxDescent, d.height - baseline);
              }
            }
          }
        }
        if (useBaseline) {
          dim.height = Math.max(maxAscent + maxDescent, dim.height);
        }
        Insets insets = target.getInsets();
        dim.width += insets.left + insets.right;
        dim.height += insets.top + insets.bottom + 5 * 2;
        return dim;
      }
    }
  }

  public static final String POPUP_PLACE = ToolWindowContentUI.POPUP_PLACE;
  // when client property is put in toolwindow component, hides toolwindow label
  public static final String HIDE_ID_LABEL = ToolWindowContentUI.HIDE_ID_LABEL;

  ContentManager myManager;

  final JPanel myContent = new JPanel(new BorderLayout());
  DesktopToolWindowImpl myWindow;

  TabbedContentAction.CloseAllAction myCloseAllAction;
  TabbedContentAction.MyNextTabAction myNextTabAction;
  TabbedContentAction.MyPreviousTabAction myPreviousTabAction;

  ShowContentAction myShowContent;

  TabContentLayout myTabsLayout = new TabContentLayout(this);
  ComboContentLayout myComboLayout = new ComboContentLayout(this);

  private ToolWindowContentUiType myType = ToolWindowContentUiType.TABBED;
  public Predicate<Point> isResizableArea = p -> true;

  public DesktopToolWindowContentUi(DesktopToolWindowImpl window) {
    setLayout(new MyLayoutManager());
    setBorder(JBUI.Borders.empty(0, 0, 0, 5));
    setOpaque(false);

    myWindow = window;
    myContent.setOpaque(false);
    myContent.setFocusable(false);

    myShowContent = new ShowContentAction(myWindow, myContent);
  }

  @Nonnull
  public ContentManager getContentManager() {
    return myManager;
  }

  public void setType(@Nonnull ToolWindowContentUiType type) {
    if (myType != type) {

      if (myType != null) {
        getCurrentLayout().reset();
      }

      myType = type;

      getCurrentLayout().init();
      rebuild();
    }
  }

  @RequiredUIAccess
  private boolean isResizeable() {
    if (myWindow.getType() == ToolWindowType.FLOATING || myWindow.getType() == ToolWindowType.WINDOWED) return false;
    if (myWindow.getAnchor() == ToolWindowAnchor.BOTTOM) return true;
    if (myWindow.getAnchor() == ToolWindowAnchor.TOP) return false;
    if (!myWindow.isSplitMode()) return false;
    DesktopToolWindowManagerImpl manager = (DesktopToolWindowManagerImpl)myWindow.getToolWindowManager();
    List<String> ids = manager.getIdsOn(myWindow.getAnchor());
    for (String id : ids) {
      if (id.equals(myWindow.getId())) continue;
      ToolWindow window = manager.getToolWindow(id);
      if (window != null && window.isVisible() && (window.getType() == ToolWindowType.DOCKED || window.getType() == ToolWindowType.SLIDING)) {
        return true;
      }
    }
    return false;
  }

  public void setTabDoubleClickActions(@Nonnull AnAction... actions) {
    myTabsLayout.setTabDoubleClickActions(actions);
  }

  private boolean isResizeable(@Nonnull Point point) {
    return isResizableArea.test(point);
  }

  private ContentLayout getCurrentLayout() {
    assert myManager != null;
    return myType == ToolWindowContentUiType.TABBED ? myTabsLayout : myComboLayout;
  }

  @Override
  public JComponent getComponent() {
    return myContent;
  }

  public JComponent getTabComponent() {
    return this;
  }

  @Override
  public void setManager(@Nonnull final ContentManager manager) {
    if (myManager != null) {
      getCurrentLayout().reset();
    }

    myManager = manager;

    getCurrentLayout().init();

    myManager.addContentManagerListener(new ContentManagerListener() {
      @Override
      @RequiredUIAccess
      public void contentAdded(final ContentManagerEvent event) {
        getCurrentLayout().contentAdded(event);
        event.getContent().addPropertyChangeListener(DesktopToolWindowContentUi.this);
        rebuild();
      }

      @Override
      @RequiredUIAccess
      public void contentRemoved(final ContentManagerEvent event) {
        event.getContent().removePropertyChangeListener(DesktopToolWindowContentUi.this);
        getCurrentLayout().contentRemoved(event);
        ensureSelectedContentVisible();
        rebuild();
      }

      @Override
      @RequiredUIAccess
      public void contentRemoveQuery(final ContentManagerEvent event) {
      }

      @Override
      @RequiredUIAccess
      public void selectionChanged(final ContentManagerEvent event) {
        ensureSelectedContentVisible();

        update();

        myContent.revalidate();
        myContent.repaint();
      }
    });

    initMouseListeners(this, this, true);

    rebuild();

    myCloseAllAction = new TabbedContentAction.CloseAllAction(myManager);
    myNextTabAction = new TabbedContentAction.MyNextTabAction(myManager);
    myPreviousTabAction = new TabbedContentAction.MyPreviousTabAction(myManager);
  }

  private void ensureSelectedContentVisible() {
    final Content selected = myManager.getSelectedContent();
    if (selected == null) {
      myContent.removeAll();
      return;
    }

    if (myContent.getComponentCount() == 1) {
      final Component visible = myContent.getComponent(0);
      if (visible == selected.getComponent()) return;
    }

    myContent.removeAll();
    myContent.add(selected.getComponent(), BorderLayout.CENTER);

    myContent.revalidate();
    myContent.repaint();
  }

  @RequiredUIAccess
  private void rebuild() {
    getCurrentLayout().rebuild();
    getCurrentLayout().update();

    revalidate();
    repaint();

    if (myManager.getContentCount() == 0 && myWindow.isToHideOnEmptyContent()) {
      myWindow.hide(null);
    }
  }

  @Override
  protected void paintComponent(final Graphics g) {
    super.paintComponent(g);
    getCurrentLayout().paintComponent(g);
  }

  @Override
  protected void paintChildren(final Graphics g) {
    super.paintChildren(g);
    getCurrentLayout().paintChildren(g);
  }

  @Override
  public Dimension getMinimumSize() {
    Insets insets = getInsets();
    return new Dimension(insets.left + insets.right + getCurrentLayout().getMinimumWidth(), super.getMinimumSize().height);
  }

  @Override
  public void propertyChange(final PropertyChangeEvent evt) {
    update();
  }

  private void update() {
    getCurrentLayout().update();

    revalidate();
    repaint();
  }

  @Override
  public boolean isSingleSelection() {
    return true;
  }

  @Override
  public boolean isToSelectAddedContent() {
    return false;
  }

  @Override
  public boolean canBeEmptySelection() {
    return false;
  }

  @Override
  public void beforeDispose() {
  }

  @Override
  public boolean canChangeSelectionTo(@Nonnull Content content, boolean implicit) {
    return true;
  }

  @Nonnull
  @Override
  public String getCloseActionName() {
    return getCurrentLayout().getCloseActionName();
  }

  @Nonnull
  @Override
  public String getCloseAllButThisActionName() {
    return getCurrentLayout().getCloseAllButThisActionName();
  }

  @Nonnull
  @Override
  public String getPreviousContentActionName() {
    return getCurrentLayout().getPreviousContentActionName();
  }

  @Nonnull
  @Override
  public String getNextContentActionName() {
    return getCurrentLayout().getNextContentActionName();
  }

  public static void initMouseListeners(final JComponent c, final DesktopToolWindowContentUi ui, final boolean allowResize) {
    if (c.getClientProperty(ui) != null) return;

    MouseAdapter mouseAdapter = new MouseAdapter() {
      final Ref<Point> myLastPoint = Ref.create();
      final Ref<Point> myPressPoint = Ref.create();
      final Ref<Integer> myInitialHeight = Ref.create(0);
      final Ref<Boolean> myIsLastComponent = Ref.create();

      @RequiredUIAccess
      private Component getActualSplitter() {
        if (!allowResize || !ui.isResizeable()) return null;

        Component component = c;
        Component parent = component.getParent();
        while (parent != null) {

          if (parent instanceof ThreeComponentsSplitter threeComponentsSplitter && threeComponentsSplitter.getOrientation()) {
            if (component != threeComponentsSplitter.getFirstComponent()) {
              return parent;
            }
          }
          if (parent instanceof Splitter splitter && splitter.isVertical()
            && splitter.getSecondComponent() == component && splitter.getFirstComponent() != null) {
            return parent;
          }
          component = parent;
          parent = parent.getParent();
        }
        return null;
      }

      @RequiredUIAccess
      private void arm(Component c) {
        Component component = c != null ? getActualSplitter() : null;
        if (component instanceof ThreeComponentsSplitter splitter) {
          myIsLastComponent.set(SwingUtilities.isDescendingFrom(c, splitter.getLastComponent()));
          myInitialHeight.set(myIsLastComponent.get() ? splitter.getLastSize() : splitter.getFirstSize());
          return;
        }
        if (component instanceof Splitter splitter) {
          myIsLastComponent.set(true);
          myInitialHeight.set(splitter.getSecondComponent().getHeight());
          return;
        }
        myIsLastComponent.set(null);
        myInitialHeight.set(null);
        myPressPoint.set(null);
      }

      @Override
      @RequiredUIAccess
      public void mousePressed(MouseEvent e) {
        PointerInfo info = MouseInfo.getPointerInfo();
        if (!e.isPopupTrigger()) {
          if (!UIUtil.isCloseClick(e)) {
            myLastPoint.set(info != null ? info.getLocation() : e.getLocationOnScreen());
            myPressPoint.set(myLastPoint.get());
            if (allowResize && ui.isResizeable()) {
              arm(c.getComponentAt(e.getPoint()) == c && ui.isResizeable(e.getPoint()) ? c : null);
            }
            ui.myWindow.fireActivated();
          }
        }
      }

      @Override
      @RequiredUIAccess
      public void mouseReleased(MouseEvent e) {
        if (!e.isPopupTrigger()) {
          if (UIUtil.isCloseClick(e, MouseEvent.MOUSE_RELEASED)) {
            ui.processHide(e);
          }
          arm(null);
        }
      }

      @Override
      @RequiredUIAccess
      public void mouseMoved(MouseEvent e) {
        boolean resize = allowResize && ui.isResizeable() && getActualSplitter() != null
          && c.getComponentAt(e.getPoint()) == c && ui.isResizeable(e.getPoint());
        c.setCursor(resize ? Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR) : Cursor.getDefaultCursor());
      }

      @Override
      public void mouseExited(MouseEvent e) {
        c.setCursor(null);
      }

      @Override
      @RequiredUIAccess
      public void mouseDragged(MouseEvent e) {
        if (myLastPoint.isNull() || myPressPoint.isNull()) return;

        PointerInfo info = MouseInfo.getPointerInfo();
        if (info == null) return;
        final Point newPoint = info.getLocation();
        Point p = myLastPoint.get();

        final Window awtWindow = SwingUtilities.windowForComponent(c);
        consulo.ui.Window uiWindow = TargetAWT.from(awtWindow);

        IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);
        if (ideFrame == null) {
          final Point windowLocation = awtWindow.getLocationOnScreen();
          windowLocation.translate(newPoint.x - p.x, newPoint.y - p.y);
          awtWindow.setLocation(windowLocation);
        }

        myLastPoint.set(newPoint);
        Component component = getActualSplitter();
        if (component instanceof ThreeComponentsSplitter) {
          ThreeComponentsSplitter splitter = (ThreeComponentsSplitter)component;
          if (myIsLastComponent.get() == Boolean.TRUE) {
            splitter.setLastSize(myInitialHeight.get() + myPressPoint.get().y - myLastPoint.get().y);
          }
          else {
            splitter.setFirstSize(myInitialHeight.get() + myLastPoint.get().y - myPressPoint.get().y);
          }
        }
        if (component instanceof Splitter splitter) {
          float proportion = 1f - (float)(myInitialHeight.get() + myPressPoint.get().y - myLastPoint.get().y) / splitter.getHeight();
          splitter.setProportion(Math.max(0, Math.min(1, proportion)));
        }
      }
    };

    c.addMouseMotionListener(mouseAdapter);
    c.addMouseListener(mouseAdapter);


    c.addMouseListener(new PopupHandler() {
      @Override
      public void invokePopup(final Component comp, final int x, final int y) {
        final Content content = c instanceof BaseLabel ? ((BaseLabel)c).getContent() : null;
        ui.showContextMenu(comp, x, y, ui.myWindow.getPopupGroup(), content);
      }
    });

    c.putClientProperty(ui, Boolean.TRUE);
  }

  private void initActionGroup(DefaultActionGroup group, final Content content) {
    if (content == null) {
      return;
    }
    group.addSeparator();
    group.add(new TabbedContentAction.CloseAction(content));
    group.add(myCloseAllAction);
    group.add(new TabbedContentAction.CloseAllButThisAction(content));
    group.addSeparator();
    if (content.isPinnable()) {
      group.add(PinToolwindowTabAction.getPinAction());
      group.addSeparator();
    }

    group.add(myNextTabAction);
    group.add(myPreviousTabAction);
    group.add(myShowContent);

    if (content instanceof TabbedContent && ((TabbedContent)content).getTabs().size() > 1) {
      group.addAction(createSplitTabsAction((TabbedContent)content));
    }

    if (Boolean.TRUE == content.getUserData(Content.TABBED_CONTENT_KEY)) {
      final String groupName = content.getUserData(Content.TAB_GROUP_NAME_KEY);
      if (groupName != null) {
        group.addAction(createMergeTabsAction(myManager, groupName));
      }
    }

    group.addSeparator();
  }

  public void showContextMenu(Component comp, int x, int y, ActionGroup toolWindowGroup, @Nullable Content selectedContent) {
    if (selectedContent == null && toolWindowGroup == null) {
      return;
    }
    DefaultActionGroup group = new DefaultActionGroup();
    if (selectedContent != null) {
      initActionGroup(group, selectedContent);
    }

    if (toolWindowGroup != null) {
      group.addAll(toolWindowGroup);
    }

    final ActionPopupMenu popupMenu = ((ActionManagerImpl)ActionManager.getInstance()).createActionPopupMenu(POPUP_PLACE, group, new MenuItemPresentationFactory(true));
    popupMenu.getComponent().show(comp, x, y);
  }

  private static AnAction createSplitTabsAction(final TabbedContent content) {
    return new DumbAwareAction("Split '" + content.getTitlePrefix() + "' group") {
      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        content.split();
      }
    };
  }

  private static AnAction createMergeTabsAction(final ContentManager manager, final String tabPrefix) {
    return new DumbAwareAction("Merge tabs to '" + tabPrefix + "' group") {
      @RequiredUIAccess
      @Override
      public void actionPerformed(@Nonnull AnActionEvent e) {
        final Content selectedContent = manager.getSelectedContent();
        final List<Pair<String, JComponent>> tabs = new ArrayList<>();
        int selectedTab = -1;
        for (Content content : manager.getContents()) {
          if (tabPrefix.equals(content.getUserData(Content.TAB_GROUP_NAME_KEY))) {
            final String label = content.getTabName().substring(tabPrefix.length() + 2);
            final JComponent component = content.getComponent();
            if (content == selectedContent) {
              selectedTab = tabs.size();
            }
            tabs.add(Pair.create(label, component));
            manager.removeContent(content, false);
            content.setComponent(null);
            Disposer.dispose(content);
          }
        }
        PropertiesComponent.getInstance().unsetValue(TabbedContent.SPLIT_PROPERTY_PREFIX + tabPrefix);
        for (int i = 0; i < tabs.size(); i++) {
          final Pair<String, JComponent> tab = tabs.get(i);
          ContentUtilEx.addTabbedContent(manager, tab.second, tabPrefix, tab.first, i == selectedTab);
        }
      }
    };
  }

  private void processHide(final MouseEvent e) {
    IdeEventQueue.getInstance().blockNextEvents(e);
    final Component c = e.getComponent();
    if (c instanceof BaseLabel) {
      final BaseLabel tab = (BaseLabel)c;
      if (tab.getContent() != null) {
        if (myManager.canCloseContents() && tab.getContent().isCloseable()) {
          myManager.removeContent(tab.getContent(), true, true, true);
        }
        else {
          if (myManager.getContentCount() == 1) {
            hideWindow(e);
          }
        }
      }
      else {
        hideWindow(e);
      }
    }
    else {
      hideWindow(e);
    }
  }

  private void hideWindow(final MouseEvent e) {
    if (e.isControlDown()) {
      myWindow.fireHiddenSide();
    }
    else {
      myWindow.fireHidden();
    }
  }

  @Override
  @Nullable
  public Object getData(@Nonnull @NonNls Key<?> dataId) {
    if (ToolWindow.KEY == dataId) return myWindow;

    if (CloseAction.CloseTarget.KEY == dataId) {
      return computeCloseTarget();
    }

    return null;
  }

  private CloseAction.CloseTarget computeCloseTarget() {
    if (myManager.canCloseContents()) {
      Content selected = myManager.getSelectedContent();
      if (selected != null && selected.isCloseable()) {
        return new CloseContentTarget(selected);
      }
    }

    return new HideToolwindowTarget();
  }

  private class HideToolwindowTarget implements CloseAction.CloseTarget {
    @Override
    public void close() {
      myWindow.fireHidden();
    }
  }

  private class CloseContentTarget implements CloseAction.CloseTarget {

    private Content myContent;

    private CloseContentTarget(Content content) {
      myContent = content;
    }

    @Override
    public void close() {
      myManager.removeContent(myContent, true, true, true);
    }
  }

  @Override
  public void dispose() {

  }

  boolean isCurrent(ContentLayout layout) {
    return getCurrentLayout() == layout;
  }

  public static void toggleContentPopup(@Nonnull DesktopToolWindowContentUi content, @Nonnull ContentManager contentManager) {
    toggleContentPopup(content, contentManager, null);
  }

  static void toggleContentPopup(@Nonnull DesktopToolWindowContentUi content, @Nonnull ContentManager contentManager, @Nullable PopupState<JBPopup> popupState) {
    SelectContentStep step = new SelectContentStep(contentManager.getContents());
    Content selectedContent = contentManager.getSelectedContent();
    if (selectedContent != null) {
      step.setDefaultOptionIndex(contentManager.getIndexOfContent(selectedContent));
    }

    ListPopup popup = JBPopupFactory.getInstance().createListPopup(step);
    if (popupState != null) popupState.prepareToShow(popup);
    content.getCurrentLayout().showContentPopup(popup);

    if (selectedContent instanceof TabbedContent) {
      new Alarm(Alarm.ThreadToUse.SWING_THREAD, popup).addRequest(() -> popup.handleSelect(false), 50);
    }
  }
}
