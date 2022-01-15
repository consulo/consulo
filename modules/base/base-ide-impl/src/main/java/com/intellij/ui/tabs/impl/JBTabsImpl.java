/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package com.intellij.ui.tabs.impl;

import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.JBMenuItem;
import com.intellij.openapi.ui.JBPopupMenu;
import com.intellij.openapi.ui.Queryable;
import com.intellij.openapi.ui.ShadowAction;
import com.intellij.openapi.util.*;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.openapi.wm.IdeGlassPane;
import com.intellij.openapi.wm.IdeGlassPaneUtil;
import com.intellij.ui.Gray;
import com.intellij.ui.ScreenUtil;
import com.intellij.ui.awt.RelativePoint;
import com.intellij.ui.switcher.QuickActionProvider;
import com.intellij.ui.tabs.*;
import com.intellij.ui.tabs.impl.singleRow.SingleRowLayout;
import com.intellij.ui.tabs.impl.singleRow.SingleRowPassInfo;
import com.intellij.ui.tabs.impl.table.TableLayout;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.ui.Animator;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.TimedDeadzone;
import com.intellij.util.ui.UIUtil;
import com.intellij.util.ui.update.LazyUiDisposable;
import consulo.annotation.DeprecationInfo;
import consulo.awt.TargetAWT;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.ui.laf.JBEditorTabsUI;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;
import org.jetbrains.annotations.NonNls;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.ComponentUI;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;

import static com.intellij.openapi.wm.IdeFocusManager.getGlobalInstance;

/**
 * Consulo tab panel
 * <p>
 * For implementation use {@link JBEditorTabs} or {@link com.intellij.ui.TabbedPaneWrapper}
 */
public abstract class JBTabsImpl extends JComponent
        implements JBTabs, PropertyChangeListener, TimerListener, DataProvider, PopupMenuListener, consulo.disposer.Disposable, JBTabsPresentation, Queryable, QuickActionProvider {

  private static final String uiClassID = "JBEditorTabsUI";

  public static final Key<JBTabsImpl> NAVIGATION_ACTIONS_KEY = Key.create("JBTabs");
  @NonNls
  public static final Key<Integer> SIDE_TABS_SIZE_LIMIT_KEY = Key.create("SIDE_TABS_SIZE_LIMIT_KEY");
  public static final int DEFAULT_MAX_TAB_WIDTH = JBUI.scale(300);

  public static final Color MAC_AQUA_BG_COLOR = Gray._200;

  final ActionManager myActionManager;
  private final List<TabInfo> myVisibleInfos = new ArrayList<>();
  private final Map<TabInfo, Integer> myHiddenInfos = new HashMap<>();

  private TabInfo mySelectedInfo;
  public final Map<TabInfo, TabLabel> myInfo2Label = new HashMap<>();
  public final Map<TabInfo, Toolbar> myInfo2Toolbar = new HashMap<>();
  public Dimension myHeaderFitSize;

  private Insets myInnerInsets = JBUI.emptyInsets();

  private final List<EventListener> myTabMouseListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  private final List<TabsListener> myTabListeners = ContainerUtil.createLockFreeCopyOnWriteList();
  private boolean myFocused;

  private Getter<ActionGroup> myPopupGroup;
  private String myPopupPlace;

  TabInfo myPopupInfo;
  final DefaultActionGroup myNavigationActions;

  final PopupMenuListener myPopupListener;
  JPopupMenu myActivePopup;

  public boolean myHorizontalSide = true;

  private boolean myStealthTabMode = false;

  private boolean mySideComponentOnTabs = true;

  private DataProvider myDataProvider;

  private final WeakHashMap<Component, Component> myDeferredToRemove = new WeakHashMap<>();

  private final SingleRowLayout mySingleRowLayout;
  private final TableLayout myTableLayout = new TableLayout(this);


  private TabLayout myLayout;
  private LayoutPassInfo myLastLayoutPass;

  public boolean myForcedRelayout;

  private UiDecorator myUiDecorator;
  static final UiDecorator ourDefaultDecorator = new DefaultDecorator();

  private boolean myPaintFocus;

  private boolean myHideTabs = false;
  @Nullable
  private Project myProject;

  private boolean myRequestFocusOnLastFocusedComponent = false;
  private boolean myListenerAdded;
  final Set<TabInfo> myAttractions = new HashSet<>();
  private final Animator myAnimator;
  private List<TabInfo> myAllTabs;
  private boolean myPaintBlocked;
  private BufferedImage myImage;
  private IdeFocusManager myFocusManager;

  boolean myAddNavigationGroup = true;

  private boolean myDisposed;
  private boolean myToDrawBorderIfTabsHidden = true;
  private Color myActiveTabFillIn;

  private boolean myTabLabelActionsAutoHide;

  private final TabActionsAutoHideListener myTabActionsAutoHideListener = new TabActionsAutoHideListener();
  private IdeGlassPane myGlassPane;
  @NonNls
  private static final String LAYOUT_DONE = "Layout.done";
  @NonNls
  public static final String STRETCHED_BY_WIDTH = "Layout.stretchedByWidth";

  private TimedDeadzone.Length myTabActionsMouseDeadzone = TimedDeadzone.DEFAULT;

  private long myRemoveDeferredRequest;

  private JBTabsPosition myPosition = JBTabsPosition.top;

  private BaseNavigationAction myNextAction;
  private BaseNavigationAction myPrevAction;

  private boolean myTabDraggingEnabled;
  private DragHelper myDragHelper;
  private boolean myNavigationActionsEnabled = true;
  private boolean myUseBufferedPaint = true;

  protected TabInfo myDropInfo;
  private int myDropInfoIndex;
  protected boolean myShowDropLocation = true;

  private TabInfo myOldSelection;
  private SelectionChangeHandler mySelectionChangeHandler;

  private Runnable myDeferredFocusRequest;
  private int myFirstTabOffset;

  private TabLabel tabLabelAtMouse;

  @Deprecated
  @DeprecationInfo("Use JBEditorTabs or TabbedPaneWrapper")
  @SuppressWarnings("deprecation")
  public JBTabsImpl(@Nonnull Project project) {
    this(project, project);
  }

  @Deprecated
  @DeprecationInfo("Use JBEditorTabs or TabbedPaneWrapper")
  @SuppressWarnings("deprecation")
  private JBTabsImpl(@Nonnull Project project, @Nonnull Disposable parent) {
    this(project, ActionManager.getInstance(), IdeFocusManager.getInstance(project), parent);
  }

  @Deprecated
  @DeprecationInfo("Use JBEditorTabs or TabbedPaneWrapper")
  @SuppressWarnings("deprecation")
  public JBTabsImpl(@Nullable Project project, IdeFocusManager focusManager, @Nonnull Disposable parent) {
    this(project, ActionManager.getInstance(), focusManager, parent);
  }

  @Deprecated
  @DeprecationInfo("Use JBEditorTabs or TabbedPaneWrapper")
  @SuppressWarnings("deprecation")
  public JBTabsImpl(@Nullable Project project, ActionManager actionManager, IdeFocusManager focusManager, @Nonnull Disposable parent) {
    this(project, actionManager, focusManager, parent, false);
  }

  @SuppressWarnings("unused")
  protected JBTabsImpl(@Nullable Project project, @Nullable ActionManager actionManager, @Nullable IdeFocusManager focusManager, @Nullable Disposable parent, boolean onlyForChildren) {
    myProject = project;
    myActionManager = actionManager;
    myFocusManager = focusManager != null ? focusManager : getGlobalInstance();

    setOpaque(true);

    setPaintBorder(-1, -1, -1, -1);

    myNavigationActions = new DefaultActionGroup();

    setUiDecorator(null);

    mySingleRowLayout = createSingleRowLayout();
    myLayout = mySingleRowLayout;

    myPopupListener = new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
      }

      @Override
      public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
        disposePopupListener();
      }

      @Override
      public void popupMenuCanceled(final PopupMenuEvent e) {
        disposePopupListener();
      }
    };

    addMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(final MouseEvent e) {
        if (mySingleRowLayout.myLastSingRowLayout != null && mySingleRowLayout.myLastSingRowLayout.moreRect != null && mySingleRowLayout.myLastSingRowLayout.moreRect.contains(e.getPoint())) {
          showMorePopup(e);
        }
      }
    });
    addMouseWheelListener(e -> {
      if (mySingleRowLayout.myLastSingRowLayout != null) {
        mySingleRowLayout.scroll(e.getUnitsToScroll() * mySingleRowLayout.getScrollUnitIncrement());
        revalidateAndRepaint(false);
      }
    });

    myAnimator = new Animator("JBTabs Attractions", 2, 500, true) {
      @Override
      public void paintNow(final int frame, final int totalFrames, final int cycle) {
        repaintAttractions();
      }
    };

    setFocusCycleRoot(true);
    setFocusTraversalPolicy(new LayoutFocusTraversalPolicy() {
      @Override
      public Component getDefaultComponent(final Container aContainer) {
        return getToFocus();
      }
    });

    add(mySingleRowLayout.myLeftGhost);
    add(mySingleRowLayout.myRightGhost);


    if(parent != null) {
      consulo.disposer.Disposer.register(parent, this);

      if (myActionManager != null) {
        myNextAction = new SelectNextAction(this, myActionManager);
        myPrevAction = new SelectPreviousAction(this, myActionManager);

        myNavigationActions.add(myNextAction);
        myNavigationActions.add(myPrevAction);
      }

      new LazyUiDisposable<JBTabsImpl>(parent, this, this) {
        @Override
        protected void initialize(@Nonnull consulo.disposer.Disposable parent, @Nonnull JBTabsImpl child, @Nullable Project project) {
          if (project != null) {
            myProject = project;
          }

          consulo.disposer.Disposer.register(child, myAnimator);
          consulo.disposer.Disposer.register(child, () -> removeTimerUpdate());

          final IdeGlassPane gp = IdeGlassPaneUtil.find(child);
          if (gp != null) {
            gp.addMouseMotionPreprocessor(myTabActionsAutoHideListener, child);
            myGlassPane = gp;
          }

          UIUtil.addAwtListener(event -> {
            if (mySingleRowLayout.myMorePopup != null) return;
            processFocusChange();
          }, AWTEvent.FOCUS_EVENT_MASK, child);

          myDragHelper = new DragHelper(child);
          myDragHelper.start();

          if (myProject != null && myFocusManager == getGlobalInstance()) {
            myFocusManager = IdeFocusManager.getInstance(myProject);
          }
        }
      };
    }

    updateUI();
  }

  protected SingleRowLayout createSingleRowLayout() {
    return new SingleRowLayout(this);
  }

  @Override
  public JBTabs setNavigationActionBinding(String prevActionId, String nextActionId) {
    if (myNextAction != null) {
      myNextAction.reconnect(nextActionId);
    }
    if (myPrevAction != null) {
      myPrevAction.reconnect(prevActionId);
    }

    return this;
  }

  public void setHovered(TabLabel label) {
    TabLabel old = tabLabelAtMouse;
    tabLabelAtMouse = label;

    if (old != null) {
      old.repaint();
    }

    if (tabLabelAtMouse != null) {
      tabLabelAtMouse.repaint();
    }
  }

  void unHover(TabLabel label) {
    if (tabLabelAtMouse == label) {
      tabLabelAtMouse = null;
      label.repaint();
    }
  }

  public boolean isHoveredTab(TabLabel label) {
    return label != null && label == tabLabelAtMouse;
  }

  @Deprecated
  @DeprecationInfo("Always true")
  public final boolean isEditorTabs() {
    return true;
  }

  public boolean supportsCompression() {
    return false;
  }

  @Override
  public JBTabs setNavigationActionsEnabled(boolean enabled) {
    myNavigationActionsEnabled = enabled;
    return this;
  }

  @Override
  public final boolean isDisposed() {
    return myDisposed;
  }

  public static Image getComponentImage(TabInfo info) {
    JComponent cmp = info.getComponent();

    BufferedImage img;
    if (cmp.isShowing()) {
      final int width = cmp.getWidth();
      final int height = cmp.getHeight();
      img = UIUtil.createImage(info.getComponent(), width > 0 ? width : 500, height > 0 ? height : 500, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g = img.createGraphics();
      cmp.paint(g);
    }
    else {
      img = UIUtil.createImage(info.getComponent(), 500, 500, BufferedImage.TYPE_INT_ARGB);
    }
    return img;
  }

  @Override
  public void dispose() {
    myDisposed = true;
    mySelectedInfo = null;
    resetTabsCache();
    myAttractions.clear();
    myVisibleInfos.clear();
    myUiDecorator = null;
    myImage = null;
    myActivePopup = null;
    myInfo2Label.clear();
    myInfo2Toolbar.clear();
    myTabListeners.clear();
  }

  void resetTabsCache() {
    myAllTabs = null;
  }

  private void processFocusChange() {
    Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    if (owner == null) {
      setFocused(false);
      return;
    }

    if (owner == this || SwingUtilities.isDescendingFrom(owner, this)) {
      setFocused(true);
    }
    else {
      setFocused(false);
    }
  }

  private void repaintAttractions() {
    boolean needsUpdate = false;
    for (TabInfo each : myVisibleInfos) {
      TabLabel eachLabel = myInfo2Label.get(each);
      needsUpdate |= eachLabel.repaintAttraction();
    }

    if (needsUpdate) {
      relayout(true, false);
    }
  }

  @Override
  public void addNotify() {
    super.addNotify();
    addTimerUpdate();

    if (myDeferredFocusRequest != null) {
      final Runnable request = myDeferredFocusRequest;
      myDeferredFocusRequest = null;

      request.run();
    }
  }

  @Override
  public void removeNotify() {
    super.removeNotify();

    setFocused(false);

    removeTimerUpdate();

    if (ScreenUtil.isStandardAddRemoveNotify(this) && myGlassPane != null) {
      myGlassPane.removeMouseMotionPreprocessor(myTabActionsAutoHideListener);
      myGlassPane = null;
    }
  }

  @Override
  public void processMouseEvent(MouseEvent e) {
    super.processMouseEvent(e);
  }

  private void addTimerUpdate() {
    if (myActionManager != null && !myListenerAdded) {
      myActionManager.addTimerListener(500, this);
      myListenerAdded = true;
    }
  }

  private void removeTimerUpdate() {
    if (myActionManager != null && myListenerAdded) {
      myActionManager.removeTimerListener(this);
      myListenerAdded = false;
    }
  }

  public void layoutComp(SingleRowPassInfo data, int deltaX, int deltaY) {
    if (data.hToolbar != null) {
      final int toolbarHeight = data.hToolbar.getPreferredSize().height;
      final Rectangle compRect = layoutComp(deltaX, toolbarHeight + deltaY, data.comp);
      layout(data.hToolbar, compRect.x, compRect.y - toolbarHeight, compRect.width, toolbarHeight);
    }
    else if (data.vToolbar != null) {
      final int toolbarWidth = data.vToolbar.getPreferredSize().width;
      final Rectangle compRect = layoutComp(toolbarWidth + deltaX, deltaY, data.comp);
      layout(data.vToolbar, compRect.x - toolbarWidth, compRect.y, toolbarWidth, compRect.height);
    }
    else {
      layoutComp(deltaX, deltaY, data.comp);
    }
  }

  public boolean isDropTarget(TabInfo info) {
    return myDropInfo != null && myDropInfo == info;
  }

  public TabInfo getDropInfo() {
    return myDropInfo;
  }

  protected void setDropInfoIndex(int dropInfoIndex) {
    myDropInfoIndex = dropInfoIndex;
  }

  public int getFirstTabOffset() {
    return myFirstTabOffset;
  }

  public void setFirstTabOffset(int firstTabOffset) {
    myFirstTabOffset = firstTabOffset;
  }

  public int tabMSize() {
    return 20;
  }

  class TabActionsAutoHideListener extends MouseMotionAdapter implements Weighted {

    private TabLabel myCurrentOverLabel;
    private Point myLastOverPoint;

    @Override
    public double getWeight() {
      return 1;
    }

    @Override
    public void mouseMoved(final MouseEvent e) {
      if (!myTabLabelActionsAutoHide) return;

      myLastOverPoint = SwingUtilities.convertPoint(e.getComponent(), e.getX(), e.getY(), JBTabsImpl.this);
      processMouseOver();
    }

    void processMouseOver() {
      if (!myTabLabelActionsAutoHide) return;

      if (myLastOverPoint == null) return;

      if (myLastOverPoint.x >= 0 && myLastOverPoint.x < getWidth() && myLastOverPoint.y > 0 && myLastOverPoint.y < getHeight()) {
        final TabLabel label = myInfo2Label.get(_findInfo(myLastOverPoint, true));
        if (label != null) {
          if (myCurrentOverLabel != null) {
            myCurrentOverLabel.toggleShowActions(false);
          }
          label.toggleShowActions(true);
          myCurrentOverLabel = label;
          return;
        }
      }

      if (myCurrentOverLabel != null) {
        myCurrentOverLabel.toggleShowActions(false);
        myCurrentOverLabel = null;
      }
    }
  }

  @Override
  public ModalityState getModalityState() {
    return ModalityState.stateForComponent(this);
  }

  @Override
  public void run() {
    updateTabActions(false);
  }

  public void updateTabActions(final boolean validateNow) {
    final Ref<Boolean> changed = new Ref<>(Boolean.FALSE);
    for (final TabInfo eachInfo : myInfo2Label.keySet()) {
      updateTab(() -> {
        final boolean changes = myInfo2Label.get(eachInfo).updateTabActions();
        changed.set(changed.get() || changes);
        return changes;
      }, eachInfo);
    }

    if (changed.get()) {
      if (validateNow) {
        validate();
        paintImmediately(0, 0, getWidth(), getHeight());
      }
    }
  }

  public boolean canShowMorePopup() {
    final SingleRowPassInfo lastLayout = mySingleRowLayout.myLastSingRowLayout;
    return lastLayout != null && lastLayout.moreRect != null;
  }

  public void showMorePopup(@Nullable final MouseEvent e) {
    final SingleRowPassInfo lastLayout = mySingleRowLayout.myLastSingRowLayout;
    if (lastLayout == null) {
      return;
    }
    mySingleRowLayout.myMorePopup = new JBPopupMenu();
    for (final TabInfo each : myVisibleInfos) {
      if (!mySingleRowLayout.isTabHidden(each)) continue;
      final JBMenuItem item = new JBMenuItem(each.getText(), TargetAWT.to(each.getIcon()));
      item.setForeground(each.getDefaultForeground());
      item.setBackground(each.getTabColor());
      mySingleRowLayout.myMorePopup.add(item);
      item.addActionListener(e1 -> select(each, true));
    }

    mySingleRowLayout.myMorePopup.addPopupMenuListener(new PopupMenuListener() {
      @Override
      public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
      }

      @Override
      public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
        mySingleRowLayout.myMorePopup = null;
      }

      @Override
      public void popupMenuCanceled(final PopupMenuEvent e) {
        mySingleRowLayout.myMorePopup = null;
      }
    });

    if (e != null) {
      mySingleRowLayout.myMorePopup.show(this, e.getX(), e.getY());
    }
    else {
      final Rectangle rect = lastLayout.moreRect;
      if (rect != null) {
        mySingleRowLayout.myMorePopup.show(this, rect.x, rect.y + rect.height);
      }
    }
  }


  @Nullable
  private JComponent getToFocus() {
    final TabInfo info = getSelectedInfo();

    if (info == null) return null;

    JComponent toFocus = null;

    if (isRequestFocusOnLastFocusedComponent() && info.getLastFocusOwner() != null && !isMyChildIsFocusedNow()) {
      toFocus = info.getLastFocusOwner();
    }

    if (toFocus == null && info.getPreferredFocusableComponent() == null) {
      return null;
    }


    if (toFocus == null) {
      toFocus = info.getPreferredFocusableComponent();
      final JComponent policyToFocus = myFocusManager.getFocusTargetFor(toFocus);
      if (policyToFocus != null) {
        toFocus = policyToFocus;
      }
    }

    return toFocus;
  }

  @Override
  public void requestFocus() {
    final JComponent toFocus = getToFocus();
    if (toFocus != null) {
      getGlobalInstance().doWhenFocusSettlesDown(() -> getGlobalInstance().requestFocus(toFocus, true));
    }
    else {
      getGlobalInstance().doWhenFocusSettlesDown(super::requestFocus);
    }
  }

  @Override
  public boolean requestFocusInWindow() {
    final JComponent toFocus = getToFocus();
    if (toFocus != null) {
      return toFocus.requestFocusInWindow();
    }
    else {
      return super.requestFocusInWindow();
    }
  }


  @Override
  @Nonnull
  public TabInfo addTab(TabInfo info, int index) {
    return addTab(info, index, false, true);
  }

  public TabInfo addTabSilently(TabInfo info, int index) {
    return addTab(info, index, false, false);
  }

  private TabInfo addTab(TabInfo info, int index, boolean isDropTarget, boolean fireEvents) {
    if (!isDropTarget && getTabs().contains(info)) {
      return getTabs().get(getTabs().indexOf(info));
    }

    info.getChangeSupport().addPropertyChangeListener(this);
    final TabLabel label = createTabLabel(info);
    myInfo2Label.put(info, label);

    if (!isDropTarget) {
      if (index < 0 || index > myVisibleInfos.size() - 1) {
        myVisibleInfos.add(info);
      }
      else {
        myVisibleInfos.add(index, info);
      }
    }

    resetTabsCache();


    updateText(info);
    updateIcon(info);
    updateSideComponent(info);
    updateTabActions(info);

    add(label);

    adjust(info);

    updateAll(false, false);

    if (info.isHidden()) {
      updateHiding();
    }

    if (!isDropTarget && fireEvents) {
      if (getTabCount() == 1) {
        fireBeforeSelectionChanged(null, info);
        fireSelectionChanged(null, info);
      }
    }

    revalidateAndRepaint(false);

    return info;
  }

  protected TabLabel createTabLabel(TabInfo info) {
    return new TabLabel(this, info);
  }

  @Override
  @Nonnull
  public TabInfo addTab(TabInfo info) {
    return addTab(info, -1);
  }

  @Nullable
  public ActionGroup getPopupGroup() {
    return myPopupGroup != null ? myPopupGroup.get() : null;
  }

  public String getPopupPlace() {
    return myPopupPlace;
  }

  @Override
  @Nonnull
  public JBTabs setPopupGroup(@Nonnull final ActionGroup popupGroup, @Nonnull String place, final boolean addNavigationGroup) {
    return setPopupGroup(() -> popupGroup, place, addNavigationGroup);
  }

  @Nonnull
  public JBTabs setPopupGroup(@Nonnull final Getter<ActionGroup> popupGroup, @Nonnull final String place, final boolean addNavigationGroup) {
    myPopupGroup = popupGroup;
    myPopupPlace = place;
    myAddNavigationGroup = addNavigationGroup;
    return this;
  }

  private void updateAll(final boolean forcedRelayout, final boolean now) {
    mySelectedInfo = getSelectedInfo();
    updateContainer(forcedRelayout, now);
    removeDeferred();
    updateListeners();
    updateTabActions(false);
    updateEnabling();
  }

  private boolean isMyChildIsFocusedNow() {
    final Component owner = getFocusOwner();
    if (owner == null) return false;


    if (mySelectedInfo != null) {
      if (!SwingUtilities.isDescendingFrom(owner, mySelectedInfo.getComponent())) return false;
    }

    return SwingUtilities.isDescendingFrom(owner, this);
  }

  @Nullable
  private static JComponent getFocusOwner() {
    final Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    return (JComponent)(owner instanceof JComponent ? owner : null);
  }

  @Override
  @Nonnull
  public ActionCallback select(@Nonnull TabInfo info, boolean requestFocus) {
    return _setSelected(info, requestFocus);
  }

  @Nonnull
  private ActionCallback _setSelected(final TabInfo info, final boolean requestFocus) {
    if (mySelectionChangeHandler != null) {
      return mySelectionChangeHandler.execute(info, requestFocus, new ActiveRunnable() {
        @Nonnull
        @Override
        public AsyncResult<Void> run() {
          return executeSelectionChange(info, requestFocus);
        }
      });
    }
    else {
      return executeSelectionChange(info, requestFocus);
    }
  }

  @Nonnull
  private AsyncResult<Void> executeSelectionChange(TabInfo info, boolean requestFocus) {
    if (mySelectedInfo != null && mySelectedInfo.equals(info)) {
      if (!requestFocus) {
        return AsyncResult.done(null);
      }
      else {
        Component owner = myFocusManager.getFocusOwner();
        JComponent c = info.getComponent();
        if (c != null && owner != null) {
          if (c == owner || SwingUtilities.isDescendingFrom(owner, c)) {
            return AsyncResult.done(null);
          }
        }
        return requestFocus(getToFocus());
      }
    }

    if (myRequestFocusOnLastFocusedComponent && mySelectedInfo != null) {
      if (isMyChildIsFocusedNow()) {
        mySelectedInfo.setLastFocusOwner(getFocusOwner());
      }
    }

    TabInfo oldInfo = mySelectedInfo;
    mySelectedInfo = info;
    final TabInfo newInfo = getSelectedInfo();

    fireBeforeSelectionChanged(oldInfo, newInfo);

    updateContainer(false, true);

    fireSelectionChanged(oldInfo, newInfo);

    if (requestFocus) {
      final JComponent toFocus = getToFocus();
      if (myProject != null && toFocus != null) {
        final AsyncResult<Void> result = new AsyncResult<Void>();
        requestFocus(toFocus).doWhenProcessed(() -> {
          if (myDisposed) {
            result.setRejected();
          }
          else {
            removeDeferred().notifyWhenDone(result);
          }
        });
        return result;
      }
      else {
        requestFocus();
        return removeDeferred();
      }
    }
    else {
      return removeDeferred();
    }
  }

  private void fireBeforeSelectionChanged(@Nullable TabInfo oldInfo, TabInfo newInfo) {
    if (oldInfo != newInfo) {
      myOldSelection = oldInfo;
      try {
        for (TabsListener eachListener : myTabListeners) {
          eachListener.beforeSelectionChanged(oldInfo, newInfo);
        }
      }
      finally {
        myOldSelection = null;
      }
    }
  }

  private void fireSelectionChanged(@Nullable TabInfo oldInfo, TabInfo newInfo) {
    if (oldInfo != newInfo) {
      for (TabsListener eachListener : myTabListeners) {
        if (eachListener != null) {
          eachListener.selectionChanged(oldInfo, newInfo);
        }
      }
    }
  }

  void fireTabsMoved() {
    for (TabsListener eachListener : myTabListeners) {
      if (eachListener != null) {
        eachListener.tabsMoved();
      }
    }
  }


  void fireTabRemoved(TabInfo info) {
    for (TabsListener eachListener : myTabListeners) {
      if (eachListener != null) {
        eachListener.tabRemoved(info);
      }
    }
  }

  @Nonnull
  private AsyncResult<Void> requestFocus(final JComponent toFocus) {
    if (toFocus == null) return AsyncResult.resolved();

    if (isShowing()) {
      return myFocusManager.requestFocus(toFocus, true);
    }
    else {
      return AsyncResult.rejected();
    }
  }

  @Nonnull
  private AsyncResult<Void> removeDeferred() {
    final AsyncResult<Void> callback = AsyncResult.undefined();

    final long executionRequest = ++myRemoveDeferredRequest;

    final Runnable onDone = () -> {
      if (myRemoveDeferredRequest == executionRequest) {
        removeDeferredNow();
      }

      callback.setDone();
    };

    myFocusManager.doWhenFocusSettlesDown(onDone);

    return callback;
  }

  private void queueForRemove(Component c) {
    if (c instanceof JComponent) {
      addToDeferredRemove(c);
    }
    else {
      remove(c);
    }
  }

  private void unqueueFromRemove(Component c) {
    myDeferredToRemove.remove(c);
  }

  private void removeDeferredNow() {
    for (Component each : myDeferredToRemove.keySet()) {
      if (each != null && each.getParent() == this) {
        remove(each);
      }
    }
    myDeferredToRemove.clear();
  }

  @Override
  public void propertyChange(final PropertyChangeEvent evt) {
    final TabInfo tabInfo = (TabInfo)evt.getSource();
    switch (evt.getPropertyName()) {
      case TabInfo.ACTION_GROUP:
        updateSideComponent(tabInfo);
        relayout(false, false);
        break;
      case TabInfo.COMPONENT:
        relayout(true, false);
        break;
      case TabInfo.TEXT:
        updateText(tabInfo);
        break;
      case TabInfo.ICON:
        updateIcon(tabInfo);
        break;
      case TabInfo.TAB_COLOR:
        updateColor(tabInfo);
        break;
      case TabInfo.ALERT_STATUS:
        boolean start = (Boolean)evt.getNewValue();
        updateAttraction(tabInfo, start);
        break;
      case TabInfo.TAB_ACTION_GROUP:
        updateTabActions(tabInfo);
        relayout(false, false);
        break;
      case TabInfo.HIDDEN:
        updateHiding();
        relayout(false, false);
        break;
      case TabInfo.ENABLED:
        updateEnabling();
        break;
    }
  }

  private void updateEnabling() {
    final List<TabInfo> all = getTabs();
    for (TabInfo each : all) {
      final TabLabel eachLabel = myInfo2Label.get(each);
      eachLabel.setTabEnabled(each.isEnabled());
    }

    final TabInfo selected = getSelectedInfo();
    if (selected != null && !selected.isEnabled()) {
      final TabInfo toSelect = getToSelectOnRemoveOf(selected);
      if (toSelect != null) {
        select(toSelect, myFocusManager.getFocusedDescendantFor(this) != null);
      }
    }
  }

  private void updateHiding() {
    boolean update = false;

    Iterator<TabInfo> visible = myVisibleInfos.iterator();
    while (visible.hasNext()) {
      TabInfo each = visible.next();
      if (each.isHidden() && !myHiddenInfos.containsKey(each)) {
        myHiddenInfos.put(each, myVisibleInfos.indexOf(each));
        visible.remove();
        update = true;
      }
    }


    Iterator<TabInfo> hidden = myHiddenInfos.keySet().iterator();
    while (hidden.hasNext()) {
      TabInfo each = hidden.next();
      if (!each.isHidden() && myHiddenInfos.containsKey(each)) {
        myVisibleInfos.add(getIndexInVisibleArray(each), each);
        hidden.remove();
        update = true;
      }
    }


    if (update) {
      resetTabsCache();
      if (mySelectedInfo != null && myHiddenInfos.containsKey(mySelectedInfo)) {
        mySelectedInfo = getToSelectOnRemoveOf(mySelectedInfo);
      }
      updateAll(true, false);
    }
  }

  private int getIndexInVisibleArray(TabInfo each) {
    Integer index = myHiddenInfos.get(each);
    if (index == null) {
      index = myVisibleInfos.size();
    }

    if (index > myVisibleInfos.size()) {
      index = myVisibleInfos.size();
    }

    if (index < 0) {
      index = 0;
    }

    return index;
  }

  private void updateIcon(final TabInfo tabInfo) {
    updateTab(() -> {
      myInfo2Label.get(tabInfo).setIcon(tabInfo.getIcon());
      return true;
    }, tabInfo);
  }

  private void updateColor(final TabInfo tabInfo) {
    updateTab(() -> {
      repaint();
      return true;
    }, tabInfo);
  }

  private void updateTab(Computable<Boolean> update, TabInfo info) {
    final TabLabel label = myInfo2Label.get(info);
    Boolean changes = update.compute();
    if (label.getRootPane() != null) {
      if (label.isValid()) {
        if (changes) {
          label.repaint();
        }
      }
      else {
        revalidateAndRepaint(false);
      }
    }
  }

  void revalidateAndRepaint(final boolean layoutNow) {

    if (myVisibleInfos.isEmpty()) {
      setOpaque(false);
      final Component nonOpaque = UIUtil.findUltimateParent(this);
      if (nonOpaque != null && getParent() != null) {
        final Rectangle toRepaint = SwingUtilities.convertRectangle(getParent(), getBounds(), nonOpaque);
        nonOpaque.repaint(toRepaint.x, toRepaint.y, toRepaint.width, toRepaint.height);
      }
    }
    else {
      setOpaque(true);
    }

    if (layoutNow) {
      validate();
    }
    else {
      revalidate();
    }

    repaint();
  }


  private void updateAttraction(final TabInfo tabInfo, boolean start) {
    if (start) {
      myAttractions.add(tabInfo);
    }
    else {
      myAttractions.remove(tabInfo);
      tabInfo.setBlinkCount(0);
    }

    if (start && !myAnimator.isRunning()) {
      myAnimator.resume();
    }
    else if (!start && myAttractions.isEmpty()) {
      myAnimator.suspend();
      repaintAttractions();
    }
  }

  private void updateText(final TabInfo tabInfo) {
    updateTab(() -> {
      final TabLabel label = myInfo2Label.get(tabInfo);
      label.setText(tabInfo.getColoredText());
      label.setToolTipText(tabInfo.getTooltipText());
      return true;
    }, tabInfo);
  }

  private void updateSideComponent(final TabInfo tabInfo) {
    final Toolbar old = myInfo2Toolbar.get(tabInfo);
    if (old != null) {
      remove(old);
    }

    final Toolbar toolbar = createToolbarComponent(tabInfo);
    myInfo2Toolbar.put(tabInfo, toolbar);
    add(toolbar);
  }

  private void updateTabActions(final TabInfo info) {
    myInfo2Label.get(info).setTabActions(info.getTabLabelActions());
  }

  @Override
  @Nullable
  public TabInfo getSelectedInfo() {
    if (myOldSelection != null) return myOldSelection;

    if (!myVisibleInfos.contains(mySelectedInfo)) {
      mySelectedInfo = null;
    }
    return mySelectedInfo != null ? mySelectedInfo : !myVisibleInfos.isEmpty() ? myVisibleInfos.get(0) : null;
  }

  @Nullable
  public TabInfo getSelectedInfoInternal() {
    return mySelectedInfo;
  }

  public SingleRowLayout getSingleRowLayoutInternal() {
    return mySingleRowLayout;
  }

  @Nullable
  private TabInfo getToSelectOnRemoveOf(TabInfo info) {
    if (!myVisibleInfos.contains(info)) return null;
    if (mySelectedInfo != info) return null;

    if (myVisibleInfos.size() == 1) return null;

    int index = myVisibleInfos.indexOf(info);

    TabInfo result = null;
    if (index > 0) {
      result = findEnabledBackward(index, false);
    }

    if (result == null) {
      result = findEnabledForward(index, false);
    }

    return result;
  }

  public boolean isAlphabeticalMode() {
    return false;
  }

  @Nullable
  private TabInfo findEnabledForward(int from, boolean cycle) {
    if (from < 0) return null;
    int index = from;
    while (true) {
      index++;
      if (index == myVisibleInfos.size()) {
        if (!cycle) break;
        index = 0;
      }
      if (index == from) break;
      final TabInfo each = myVisibleInfos.get(index);
      if (each.isEnabled()) return each;
    }

    return null;
  }

  @Nullable
  private TabInfo findEnabledBackward(int from, boolean cycle) {
    if (from < 0) return null;
    int index = from;
    while (true) {
      index--;
      if (index == -1) {
        if (!cycle) break;
        index = myVisibleInfos.size() - 1;
      }
      if (index == from) break;
      final TabInfo each = myVisibleInfos.get(index);
      if (each.isEnabled()) return each;
    }

    return null;
  }

  protected Toolbar createToolbarComponent(final TabInfo tabInfo) {
    return new Toolbar(this, tabInfo);
  }

  @Override
  @Nonnull
  public TabInfo getTabAt(final int tabIndex) {
    return getTabs().get(tabIndex);
  }

  @Nonnull
  public List<TabInfo> getTabs() {
    if (myAllTabs != null) return myAllTabs;

    ArrayList<TabInfo> result = new ArrayList<>();
    result.addAll(myVisibleInfos);

    for (TabInfo each : myHiddenInfos.keySet()) {
      result.add(getIndexInVisibleArray(each), each);
    }

    myAllTabs = result;

    return result;
  }

  @Override
  public TabInfo getTargetInfo() {
    return myPopupInfo != null ? myPopupInfo : getSelectedInfo();
  }

  @Override
  public void popupMenuWillBecomeVisible(final PopupMenuEvent e) {
  }

  @Override
  public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
    resetPopup();
  }

  @Override
  public void popupMenuCanceled(final PopupMenuEvent e) {
    resetPopup();
  }

  private void resetPopup() {
//todo [kirillk] dirty hack, should rely on ActionManager to understand that menu item was either chosen on or cancelled
    SwingUtilities.invokeLater(() -> myPopupInfo = null);
  }

  @Override
  public void setPaintBlocked(boolean blocked, final boolean takeSnapshot) {
    if (blocked && !myPaintBlocked) {
      if (takeSnapshot) {
        if (getWidth() > 0 && getHeight() > 0) {
          myImage = UIUtil.createImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_ARGB);
          final Graphics2D g = myImage.createGraphics();
          super.paint(g);
          g.dispose();
        }
      }
    }

    myPaintBlocked = blocked;

    if (!myPaintBlocked) {
      if (myImage != null) {
        myImage.flush();
      }

      myImage = null;
      repaint();
    }
  }


  private void addToDeferredRemove(final Component c) {
    if (!myDeferredToRemove.containsKey(c)) {
      myDeferredToRemove.put(c, c);
    }
  }

  private boolean isToDrawBorderIfTabsHidden() {
    return myToDrawBorderIfTabsHidden;
  }

  @Override
  @Nonnull
  public JBTabsPresentation setToDrawBorderIfTabsHidden(final boolean toDrawBorderIfTabsHidden) {
    myToDrawBorderIfTabsHidden = toDrawBorderIfTabsHidden;
    return this;
  }

  @Override
  @Nonnull
  public JBTabs getJBTabs() {
    return this;
  }

  public static class Toolbar extends JPanel {
    private final JBTabsImpl myTabs;

    public Toolbar(JBTabsImpl tabs, TabInfo info) {
      super(new BorderLayout());
      myTabs = tabs;

      final ActionGroup group = info.getGroup();
      final JComponent side = info.getSideComponent();

      if (group != null && myTabs.myActionManager != null) {
        final String place = info.getPlace();
        ActionToolbar toolbar = myTabs.myActionManager.createActionToolbar(place != null ? place : ActionPlaces.UNKNOWN, group, myTabs.myHorizontalSide);
        toolbar.setTargetComponent(info.getActionsContextComponent());
        final JComponent actionToolbar = toolbar.getComponent();
        add(actionToolbar, BorderLayout.CENTER);
      }

      if (side != null) {
        if (group != null) {
          add(side, BorderLayout.EAST);
        }
        else {
          add(side, BorderLayout.CENTER);
        }
      }
    }

    public boolean isEmpty() {
      return getComponentCount() == 0;
    }
  }


  @Override
  public void doLayout() {
    try {
      myHeaderFitSize = computeHeaderFitSize();

      final Collection<TabLabel> labels = myInfo2Label.values();
      for (TabLabel each : labels) {
        each.setTabActionsAutoHide(myTabLabelActionsAutoHide);
      }


      List<TabInfo> visible = new ArrayList<>();
      visible.addAll(myVisibleInfos);

      if (myDropInfo != null && !visible.contains(myDropInfo) && myShowDropLocation) {
        if (getDropInfoIndex() >= 0 && getDropInfoIndex() < visible.size()) {
          visible.add(getDropInfoIndex(), myDropInfo);
        }
        else {
          visible.add(myDropInfo);
        }
      }

      if (isSingleRow()) {
        myLastLayoutPass = mySingleRowLayout.layoutSingleRow(visible);
        mySingleRowLayout.scroll(0);
        myLastLayoutPass = mySingleRowLayout.layoutSingleRow(visible);
        myTableLayout.myLastTableLayout = null;
      }
      else {
        myLastLayoutPass = myTableLayout.layoutTable(visible);
        mySingleRowLayout.myLastSingRowLayout = null;
      }

      if (isStealthModeEffective() && !isHideTabs()) {
        final TabLabel label = getSelectedLabel();
        final Rectangle bounds = label.getBounds();
        final Insets insets = getLayoutInsets();
        layout(label, insets.left, bounds.y, getWidth() - insets.right - insets.left, bounds.height);
      }

      moveDraggedTabLabel();

      myTabActionsAutoHideListener.processMouseOver();
    }
    finally {
      myForcedRelayout = false;
    }

    applyResetComponents();
  }

  void moveDraggedTabLabel() {
    if (myDragHelper != null && myDragHelper.myDragRec != null) {
      final TabLabel selectedLabel = myInfo2Label.get(getSelectedInfo());
      if (selectedLabel != null) {
        final Rectangle bounds = selectedLabel.getBounds();
        if (isHorizontalTabs()) {
          selectedLabel.setBounds(myDragHelper.myDragRec.x, bounds.y, bounds.width, bounds.height);
        }
        else {
          selectedLabel.setBounds(bounds.x, myDragHelper.myDragRec.y, bounds.width, bounds.height);
        }
      }
    }
  }

  private Dimension computeHeaderFitSize() {
    final Max max = computeMaxSize();

    if (myPosition == JBTabsPosition.top || myPosition == JBTabsPosition.bottom) {
      return new Dimension(getSize().width, myHorizontalSide ? Math.max(max.myLabel.height, max.myToolbar.height) : max.myLabel.height);
    }
    else {
      return new Dimension(max.myLabel.width + (myHorizontalSide ? 0 : max.myToolbar.width), getSize().height);
    }
  }

  public Rectangle layoutComp(int componentX, int componentY, final JComponent comp) {
    final Insets insets = getLayoutInsets();

    final Insets border = JBUI.emptyInsets();

    final Insets inner = getInnerInsets();
    border.top += inner.top;
    border.bottom += inner.bottom;
    border.left += inner.left;
    border.right += inner.right;


    int x = insets.left + componentX + border.left;
    int y = insets.top + componentY + border.top;
    int width = getWidth() - insets.left - insets.right - componentX - border.left - border.right;
    int height = getHeight() - insets.top - insets.bottom - componentY - border.top - border.bottom;

    return layout(comp, x, y, width, height);
  }


  @Override
  public JBTabsPresentation setInnerInsets(final Insets innerInsets) {
    myInnerInsets = innerInsets;
    return this;
  }

  private Insets getInnerInsets() {
    return myInnerInsets;
  }

  public Insets getLayoutInsets() {
    Insets insets = getInsets();
    if (insets == null) {
      insets = JBUI.emptyInsets();
    }
    return insets;
  }

  public void resetLayout(boolean resetLabels) {
    if (resetLabels) {
      mySingleRowLayout.myLeftGhost.reset();
      mySingleRowLayout.myRightGhost.reset();
    }

    for (TabInfo each : myVisibleInfos) {
      reset(each, resetLabels);
    }

    if (myDropInfo != null) {
      reset(myDropInfo, resetLabels);
    }

    for (TabInfo each : myHiddenInfos.keySet()) {
      reset(each, resetLabels);
    }

    for (Component eachDeferred : myDeferredToRemove.keySet()) {
      resetLayout((JComponent)eachDeferred);
    }
  }

  private void reset(final TabInfo each, final boolean resetLabels) {
    final JComponent c = each.getComponent();
    if (c != null) {
      resetLayout(c);
    }

    resetLayout(myInfo2Toolbar.get(each));

    if (resetLabels) {
      resetLayout(myInfo2Label.get(each));
    }
  }

  public JBTabsPosition getPosition() {
    return myPosition;
  }

  public TabLabel getSelectedLabel() {
    return myInfo2Label.get(getSelectedInfo());
  }

  public List<TabInfo> getVisibleInfos() {
    return myVisibleInfos;
  }

  public LayoutPassInfo getLastLayoutPass() {
    return myLastLayoutPass;
  }

  public static int getSelectionTabVShift() {
    return 2;
  }

  public boolean isStealthModeEffective() {
    return myStealthTabMode && getTabCount() == 1 && (isSideComponentVertical() || !isSideComponentOnTabs()) && getTabsPosition() == JBTabsPosition.top;
  }

  private boolean isNavigationVisible() {
    if (myStealthTabMode && getTabCount() == 1) return false;
    return !myVisibleInfos.isEmpty();
  }


  @Override
  public void paint(final Graphics g) {
    Rectangle clip = g.getClipBounds();
    if (clip == null) {
      return;
    }

    if (myPaintBlocked) {
      if (myImage != null) {
        g.drawImage(myImage, 0, 0, getWidth(), getHeight(), null);
      }
      return;
    }

    super.paint(g);
  }

  private Max computeMaxSize() {
    Max max = new Max();
    for (TabInfo eachInfo : myVisibleInfos) {
      final TabLabel label = myInfo2Label.get(eachInfo);
      max.myLabel.height = Math.max(max.myLabel.height, label.getPreferredSize().height);
      max.myLabel.width = Math.max(max.myLabel.width, label.getPreferredSize().width);
      final Toolbar toolbar = myInfo2Toolbar.get(eachInfo);
      if (myLayout.isSideComponentOnTabs() && toolbar != null && !toolbar.isEmpty()) {
        max.myToolbar.height = Math.max(max.myToolbar.height, toolbar.getPreferredSize().height);
        max.myToolbar.width = Math.max(max.myToolbar.width, toolbar.getPreferredSize().width);
      }
    }

    max.myToolbar.height++;

    return max;
  }

  public int getTabBorderSize() {
    return TabsUtil.TABS_BORDER;
  }

  @Override
  public int getTabCount() {
    return getTabs().size();
  }

  @Override
  @Nonnull
  public JBTabsPresentation getPresentation() {
    return this;
  }

  @Override
  @Nonnull
  public ActionCallback removeTab(final TabInfo info) {
    return removeTab(info, null, true);
  }

  @Nonnull
  public ActionCallback removeTab(final TabInfo info, @Nullable TabInfo forcedSelectionTransfer, boolean transferFocus) {
    return removeTab(info, forcedSelectionTransfer, transferFocus, false);
  }

  @Nonnull
  private ActionCallback removeTab(TabInfo info, @Nullable TabInfo forcedSelectionTransfer, boolean transferFocus, boolean isDropTarget) {
    if (!isDropTarget) {
      if (info == null || !getTabs().contains(info)) return new ActionCallback.Done();
    }

    if (isDropTarget && myLastLayoutPass != null) {
      myLastLayoutPass.myVisibleInfos.remove(info);
    }

    final ActionCallback result = new ActionCallback();

    TabInfo toSelect;
    if (forcedSelectionTransfer == null) {
      toSelect = getToSelectOnRemoveOf(info);
    }
    else {
      assert myVisibleInfos.contains(forcedSelectionTransfer) : "Cannot find tab for selection transfer, tab=" + forcedSelectionTransfer;
      toSelect = forcedSelectionTransfer;
    }


    if (toSelect != null) {
      boolean clearSelection = info.equals(mySelectedInfo);
      processRemove(info, false);
      if (clearSelection) {
        mySelectedInfo = info;
      }
      _setSelected(toSelect, transferFocus).doWhenProcessed(() -> removeDeferred().notifyWhenDone(result));
    }
    else {
      processRemove(info, true);
      removeDeferred().notifyWhenDone(result);
    }

    if (myVisibleInfos.isEmpty()) {
      removeDeferredNow();
    }

    revalidateAndRepaint(true);

    fireTabRemoved(info);

    return result;
  }

  private void processRemove(final TabInfo info, boolean forcedNow) {
    remove(myInfo2Label.get(info));
    remove(myInfo2Toolbar.get(info));

    JComponent tabComponent = info.getComponent();

    if (!isToDeferRemoveForLater(tabComponent) || forcedNow) {
      remove(tabComponent);
    }
    else {
      queueForRemove(tabComponent);
    }

    myVisibleInfos.remove(info);
    myHiddenInfos.remove(info);
    myInfo2Label.remove(info);
    myInfo2Toolbar.remove(info);
    resetTabsCache();

    updateAll(false, false);

    // avoid leaks
    getUIInternal().clearLastPaintedTab();
  }

  /**
   * return ui object. Do not rename it to 'getUI' due it will conflict on Java 9
   */
  protected JBEditorTabsUI getUIInternal() {
    return (JBEditorTabsUI)ui;
  }

  @Nullable
  public TabInfo findInfo(Component component) {
    for (TabInfo each : getTabs()) {
      if (each.getComponent() == component) return each;
    }

    return null;
  }

  @Override
  public TabInfo findInfo(MouseEvent event) {
    return findInfo(event, false);
  }

  @Nullable
  private TabInfo findInfo(final MouseEvent event, final boolean labelsOnly) {
    final Point point = SwingUtilities.convertPoint(event.getComponent(), event.getPoint(), this);
    return _findInfo(point, labelsOnly);
  }

  @Override
  public TabInfo findInfo(final Object object) {
    for (int i = 0; i < getTabCount(); i++) {
      final TabInfo each = getTabAt(i);
      final Object eachObject = each.getObject();
      if (eachObject != null && eachObject.equals(object)) return each;
    }
    return null;
  }

  @Nullable
  private TabInfo _findInfo(final Point point, boolean labelsOnly) {
    Component component = findComponentAt(point);
    if (component == null) return null;
    while (component != this || component != null) {
      if (component instanceof TabLabel) {
        return ((TabLabel)component).getInfo();
      }
      if (!labelsOnly) {
        final TabInfo info = findInfo(component);
        if (info != null) return info;
      }
      if (component == null) break;
      component = component.getParent();
    }

    return null;
  }

  @Override
  public void removeAllTabs() {
    for (TabInfo each : getTabs()) {
      removeTab(each);
    }
  }


  private static class Max {
    final Dimension myLabel = new Dimension();
    final Dimension myToolbar = new Dimension();
  }

  private void updateContainer(boolean forced, final boolean layoutNow) {
    for (TabInfo each : new ArrayList<>(myVisibleInfos)) {
      final JComponent eachComponent = each.getComponent();
      if (getSelectedInfo() == each && getSelectedInfo() != null) {
        unqueueFromRemove(eachComponent);

        final Container parent = eachComponent.getParent();
        if (parent != null && parent != this) {
          parent.remove(eachComponent);
        }

        if (eachComponent.getParent() == null) {
          add(eachComponent);
        }
      }
      else {
        if (eachComponent.getParent() == null) continue;
        if (isToDeferRemoveForLater(eachComponent)) {
          queueForRemove(eachComponent);
        }
        else {
          remove(eachComponent);
        }
      }
    }

    mySingleRowLayout.scrollSelectionInView();
    relayout(forced, layoutNow);
  }

  @Override
  protected void addImpl(final Component comp, final Object constraints, final int index) {
    unqueueFromRemove(comp);

    if (comp instanceof TabLabel) {
      ((TabLabel)comp).apply(myUiDecorator.getDecoration());
    }

    super.addImpl(comp, constraints, index);
  }

  private static boolean isToDeferRemoveForLater(JComponent c) {
    return c.getRootPane() != null;
  }

  void relayout(boolean forced, final boolean layoutNow) {
    if (!myForcedRelayout) {
      myForcedRelayout = forced;
    }
    revalidateAndRepaint(layoutNow);
  }

  @Override
  @Nonnull
  public JBTabs addTabMouseListener(@Nonnull MouseListener listener) {
    removeListeners();
    myTabMouseListeners.add(listener);
    addListeners();
    return this;
  }

  @Override
  @Nonnull
  public JComponent getComponent() {
    return this;
  }

  @Override
  public boolean isCycleRoot() {
    return false;
  }

  private void addListeners() {
    for (TabInfo eachInfo : myVisibleInfos) {
      final TabLabel label = myInfo2Label.get(eachInfo);
      for (EventListener eachListener : myTabMouseListeners) {
        if (eachListener instanceof MouseListener) {
          label.addMouseListener((MouseListener)eachListener);
        }
        else if (eachListener instanceof MouseMotionListener) {
          label.addMouseMotionListener((MouseMotionListener)eachListener);
        }
        else {
          assert false;
        }
      }
    }
  }

  private void removeListeners() {
    for (TabInfo eachInfo : myVisibleInfos) {
      final TabLabel label = myInfo2Label.get(eachInfo);
      for (EventListener eachListener : myTabMouseListeners) {
        if (eachListener instanceof MouseListener) {
          label.removeMouseListener((MouseListener)eachListener);
        }
        else if (eachListener instanceof MouseMotionListener) {
          label.removeMouseMotionListener((MouseMotionListener)eachListener);
        }
        else {
          assert false;
        }
      }
    }
  }

  private void updateListeners() {
    removeListeners();
    addListeners();
  }

  @Override
  public JBTabs addListener(@Nonnull TabsListener listener) {
    myTabListeners.add(listener);
    return this;
  }

  @Override
  public JBTabs setSelectionChangeHandler(SelectionChangeHandler handler) {
    mySelectionChangeHandler = handler;
    return this;
  }

  public void setFocused(final boolean focused) {
    if (myFocused == focused) return;

    myFocused = focused;

    if (myPaintFocus) {
      repaint();
    }
  }

  @Override
  public int getIndexOf(@Nullable final TabInfo tabInfo) {
    return myVisibleInfos.indexOf(tabInfo);
  }

  @Override
  public boolean isHideTabs() {
    return myHideTabs;
  }

  @Override
  public void setHideTabs(final boolean hideTabs) {
    if (isHideTabs() == hideTabs) return;

    myHideTabs = hideTabs;

    relayout(true, false);
  }

  public boolean isPaintFocus() {
    return myPaintFocus;
  }

  @Override
  @Nonnull
  public JBTabsPresentation setActiveTabFillIn(@Nullable final Color color) {
    if (!isChanged(myActiveTabFillIn, color)) return this;

    myActiveTabFillIn = color;
    revalidateAndRepaint(false);
    return this;
  }

  private static boolean isChanged(Object oldObject, Object newObject) {
    if (oldObject == null && newObject == null) return false;
    return oldObject != null && !oldObject.equals(newObject) || newObject != null && !newObject.equals(oldObject);
  }

  @Override
  @Nonnull
  public JBTabsPresentation setTabLabelActionsAutoHide(final boolean autoHide) {
    if (myTabLabelActionsAutoHide != autoHide) {
      myTabLabelActionsAutoHide = autoHide;
      revalidateAndRepaint(false);
    }
    return this;
  }

  @Nullable
  protected Color getActiveTabFillIn() {
    return myActiveTabFillIn;
  }

  @Override
  public JBTabsPresentation setFocusCycle(final boolean root) {
    setFocusCycleRoot(root);
    return this;
  }


  @Override
  public JBTabsPresentation setPaintFocus(final boolean paintFocus) {
    myPaintFocus = paintFocus;
    return this;
  }

  private abstract static class BaseNavigationAction extends AnAction {

    private final ShadowAction myShadow;
    private final ActionManager myActionManager;
    private final JBTabsImpl myTabs;

    protected BaseNavigationAction(final String copyFromID, JBTabsImpl tabs, ActionManager mgr) {
      myActionManager = mgr;
      myTabs = tabs;
      myShadow = new ShadowAction(this, myActionManager.getAction(copyFromID), tabs);
      Disposer.register(tabs, myShadow);
      setEnabledInModalContext(true);
    }

    @RequiredUIAccess
    @Override
    public final void update(@Nonnull final AnActionEvent e) {
      JBTabsImpl tabs = e.getData(NAVIGATION_ACTIONS_KEY);
      e.getPresentation().setVisible(tabs != null);
      if (tabs == null) return;

      tabs = findNavigatableTabs(tabs);
      e.getPresentation().setEnabled(tabs != null);
      if (tabs != null) {
        _update(e, tabs, tabs.myVisibleInfos.indexOf(tabs.getSelectedInfo()));
      }
    }

    @Nullable
    protected JBTabsImpl findNavigatableTabs(JBTabsImpl tabs) {
      // The debugger UI contains multiple nested JBTabsImpl, where the innermost JBTabsImpl has only one tab. In this case,
      // the action should target the outer JBTabsImpl.
      if (tabs == null || tabs != myTabs) {
        return null;
      }
      if (isNavigatable(tabs)) {
        return tabs;
      }
      Component c = tabs.getParent();
      while (c != null) {
        if (c instanceof JBTabsImpl && isNavigatable((JBTabsImpl)c)) {
          return (JBTabsImpl)c;
        }
        c = c.getParent();
      }
      return null;
    }

    private static boolean isNavigatable(JBTabsImpl tabs) {
      final int selectedIndex = tabs.myVisibleInfos.indexOf(tabs.getSelectedInfo());
      return tabs.isNavigationVisible() && selectedIndex >= 0 && tabs.myNavigationActionsEnabled;
    }

    public void reconnect(String actionId) {
      myShadow.reconnect(myActionManager.getAction(actionId));
    }

    protected abstract void _update(AnActionEvent e, final JBTabsImpl tabs, int selectedIndex);

    @RequiredUIAccess
    @Override
    public final void actionPerformed(@Nonnull final AnActionEvent e) {
      JBTabsImpl tabs = e.getData(NAVIGATION_ACTIONS_KEY);
      tabs = findNavigatableTabs(tabs);
      if (tabs == null) return;

      final int index = tabs.myVisibleInfos.indexOf(tabs.getSelectedInfo());
      if (index == -1) return;
      _actionPerformed(e, tabs, index);
    }

    protected abstract void _actionPerformed(final AnActionEvent e, final JBTabsImpl tabs, final int selectedIndex);
  }

  private static class SelectNextAction extends BaseNavigationAction {

    private SelectNextAction(JBTabsImpl tabs, ActionManager mgr) {
      super(IdeActions.ACTION_NEXT_TAB, tabs, mgr);
    }

    @Override
    protected void _update(final AnActionEvent e, final JBTabsImpl tabs, int selectedIndex) {
      e.getPresentation().setEnabled(tabs.findEnabledForward(selectedIndex, true) != null);
    }

    @Override
    protected void _actionPerformed(final AnActionEvent e, final JBTabsImpl tabs, final int selectedIndex) {
      tabs.select(tabs.findEnabledForward(selectedIndex, true), true);
    }
  }

  private static class SelectPreviousAction extends BaseNavigationAction {
    private SelectPreviousAction(JBTabsImpl tabs, ActionManager mgr) {
      super(IdeActions.ACTION_PREVIOUS_TAB, tabs, mgr);
    }

    @Override
    protected void _update(final AnActionEvent e, final JBTabsImpl tabs, int selectedIndex) {
      e.getPresentation().setEnabled(tabs.findEnabledBackward(selectedIndex, true) != null);
    }

    @Override
    protected void _actionPerformed(final AnActionEvent e, final JBTabsImpl tabs, final int selectedIndex) {
      tabs.select(tabs.findEnabledBackward(selectedIndex, true), true);
    }
  }

  private void disposePopupListener() {
    if (myActivePopup != null) {
      myActivePopup.removePopupMenuListener(myPopupListener);
      myActivePopup = null;
    }
  }

  @Override
  public JBTabsPresentation setStealthTabMode(final boolean stealthTabMode) {
    myStealthTabMode = stealthTabMode;

    relayout(true, false);

    return this;
  }

  public boolean isStealthTabMode() {
    return myStealthTabMode;
  }

  @Override
  public JBTabsPresentation setSideComponentVertical(final boolean vertical) {
    myHorizontalSide = !vertical;

    for (TabInfo each : myVisibleInfos) {
      each.getChangeSupport().firePropertyChange(TabInfo.ACTION_GROUP, "new1", "new2");
    }


    relayout(true, false);

    return this;
  }

  @Override
  public JBTabsPresentation setSideComponentOnTabs(boolean onTabs) {
    mySideComponentOnTabs = onTabs;

    relayout(true, false);

    return this;
  }

  @Override
  public JBTabsPresentation setSingleRow(boolean singleRow) {
    myLayout = singleRow ? mySingleRowLayout : myTableLayout;

    relayout(true, false);

    return this;
  }

  public boolean useSmallLabels() {
    return false;
  }

  public boolean useBoldLabels() {
    return false;
  }

  @Override
  public boolean isSingleRow() {
    return getEffectiveLayout() == mySingleRowLayout;
  }

  public boolean isSideComponentVertical() {
    return !myHorizontalSide;
  }

  public boolean isSideComponentOnTabs() {
    return mySideComponentOnTabs;
  }

  public TabLayout getEffectiveLayout() {
    if (myLayout == myTableLayout && getTabsPosition() == JBTabsPosition.top) return myTableLayout;
    return mySingleRowLayout;
  }

  @Override
  public JBTabsPresentation setUiDecorator(@Nullable UiDecorator decorator) {
    myUiDecorator = decorator == null ? ourDefaultDecorator : decorator;
    applyDecoration();
    return this;
  }

  @Override
  protected void setUI(final ComponentUI newUI) {
    super.setUI(newUI);
    applyDecoration();
  }

  @Override
  public String getUIClassID() {
    return uiClassID;
  }

  @Override
  public void updateUI() {
    setUI(UIManager.getUI(this));

    SwingUtilities.invokeLater(() -> {
      applyDecoration();

      revalidateAndRepaint(false);
    });
  }

  private void applyDecoration() {
    if (myUiDecorator != null) {
      UiDecorator.UiDecoration uiDecoration = myUiDecorator.getDecoration();
      for (TabLabel each : myInfo2Label.values()) {
        each.apply(uiDecoration);
      }
    }


    for (TabInfo each : getTabs()) {
      adjust(each);
    }

    relayout(true, false);
  }

  private void adjust(final TabInfo each) {
    UIUtil.removeScrollBorder(each.getComponent());
  }

  public void sortTabs(Comparator<TabInfo> comparator) {
    Collections.sort(myVisibleInfos, comparator);

    relayout(true, false);
  }

  private boolean isRequestFocusOnLastFocusedComponent() {
    return myRequestFocusOnLastFocusedComponent;
  }

  @Override
  public JBTabsPresentation setRequestFocusOnLastFocusedComponent(final boolean requestFocusOnLastFocusedComponent) {
    myRequestFocusOnLastFocusedComponent = requestFocusOnLastFocusedComponent;
    return this;
  }


  @Override
  @Nullable
  public Object getData(@Nonnull @NonNls final Key<?> dataId) {
    if (myDataProvider != null) {
      final Object value = myDataProvider.getData(dataId);
      if (value != null) return value;
    }

    if (QuickActionProvider.KEY == dataId) {
      return this;
    }

    return NAVIGATION_ACTIONS_KEY == dataId ? this : null;
  }

  @Override
  public List<AnAction> getActions(boolean originalProvider) {
    ArrayList<AnAction> result = new ArrayList<>();

    TabInfo selection = getSelectedInfo();
    if (selection != null) {
      ActionGroup group = selection.getGroup();
      if (group != null) {
        AnAction[] children = group.getChildren(null);
        Collections.addAll(result, children);
      }
    }

    return result;
  }

  @Override
  public DataProvider getDataProvider() {
    return myDataProvider;
  }

  public JBTabsImpl setDataProvider(@Nonnull final DataProvider dataProvider) {
    myDataProvider = dataProvider;
    return this;
  }


  public static boolean isSelectionClick(final MouseEvent e, boolean canBeQuick) {
    if (e.getClickCount() == 1 || canBeQuick) {
      if (!e.isPopupTrigger()) {
        return e.getButton() == MouseEvent.BUTTON1 && !e.isControlDown() && !e.isAltDown() && !e.isMetaDown();
      }
    }

    return false;
  }


  private static class DefaultDecorator implements UiDecorator {
    @Override
    @Nonnull
    public UiDecoration getDecoration() {
      return new UiDecoration(null, JBUI.insets(0, 4, 0, 5));
    }
  }

  public Rectangle layout(JComponent c, Rectangle bounds) {
    final Rectangle now = c.getBounds();

    if (!bounds.equals(now)) {
      c.setBounds(bounds);
    }
    c.putClientProperty(LAYOUT_DONE, Boolean.TRUE);

    return bounds;
  }

  public Rectangle layout(JComponent c, int x, int y, int width, int height) {
    return layout(c, new Rectangle(x, y, width, height));
  }

  public static void resetLayout(JComponent c) {
    if (c == null) return;
    c.putClientProperty(LAYOUT_DONE, null);
    c.putClientProperty(STRETCHED_BY_WIDTH, null);
  }

  private void applyResetComponents() {
    for (int i = 0; i < getComponentCount(); i++) {
      final Component each = getComponent(i);
      if (each instanceof JComponent) {
        final JComponent jc = (JComponent)each;
        final Object done = jc.getClientProperty(LAYOUT_DONE);
        if (!Boolean.TRUE.equals(done)) {
          layout(jc, new Rectangle(0, 0, 0, 0));
        }
      }
    }
  }


  @Override
  @Nonnull
  public JBTabsPresentation setTabLabelActionsMouseDeadzone(final TimedDeadzone.Length length) {
    myTabActionsMouseDeadzone = length;
    final List<TabInfo> all = getTabs();
    for (TabInfo each : all) {
      final TabLabel eachLabel = myInfo2Label.get(each);
      eachLabel.updateTabActions();
    }
    return this;
  }

  @Override
  @Nonnull
  public JBTabsPresentation setTabsPosition(final JBTabsPosition position) {
    myPosition = position;
    relayout(true, false);
    return this;
  }

  @Override
  public JBTabsPosition getTabsPosition() {
    return myPosition;
  }

  public TimedDeadzone.Length getTabActionsMouseDeadzone() {
    return myTabActionsMouseDeadzone;
  }

  @Override
  public JBTabsPresentation setTabDraggingEnabled(boolean enabled) {
    myTabDraggingEnabled = enabled;
    return this;
  }

  public boolean isTabDraggingEnabled() {
    return myTabDraggingEnabled;
  }

  void reallocate(TabInfo source, TabInfo target) {
    if (source == target || source == null || target == null) return;

    final int targetIndex = myVisibleInfos.indexOf(target);

    myVisibleInfos.remove(source);
    myVisibleInfos.add(targetIndex, source);

    invalidate();
    relayout(true, true);
  }

  public boolean isHorizontalTabs() {
    return getTabsPosition() == JBTabsPosition.top || getTabsPosition() == JBTabsPosition.bottom;
  }

  @Override
  public void putInfo(@Nonnull Map<String, String> info) {
    final TabInfo selected = getSelectedInfo();
    if (selected != null) {
      selected.putInfo(info);
    }
  }

  public void setUseBufferedPaint(boolean useBufferedPaint) {
    myUseBufferedPaint = useBufferedPaint;
    revalidate();
    repaint();
  }

  @Override
  public void resetDropOver(TabInfo tabInfo) {
    if (myDropInfo != null) {
      TabInfo dropInfo = myDropInfo;
      myDropInfo = null;
      myShowDropLocation = true;
      setDropInfoIndex(-1);
      if (!isDisposed()) {
        removeTab(dropInfo, null, false, true);
      }
    }
  }

  @Override
  public Image startDropOver(TabInfo tabInfo, RelativePoint point) {
    myDropInfo = tabInfo;

    int index = myLayout.getDropIndexFor(point.getPoint(this));
    setDropInfoIndex(index);
    addTab(myDropInfo, index, true, true);

    TabLabel label = myInfo2Label.get(myDropInfo);
    Dimension size = label.getPreferredSize();
    label.setBounds(0, 0, size.width, size.height);

    BufferedImage img = UIUtil.createImage(size.width, size.height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = img.createGraphics();
    label.paintOffscreen(g);
    g.dispose();

    relayout(true, false);

    return img;
  }

  @Override
  public void processDropOver(TabInfo over, RelativePoint point) {
    int index = myLayout.getDropIndexFor(point.getPoint(this));
    if (index != getDropInfoIndex()) {
      setDropInfoIndex(index);
      relayout(true, false);
    }
  }

  public int getDropInfoIndex() {
    return myDropInfoIndex;
  }

  public boolean isEmptyVisible() {
    return myVisibleInfos.isEmpty();
  }

  public final int getInterTabSpaceLength() {
    // join two borders into one
    return -1;
  }

  public boolean isTabsBorderEnabled() {
    return true;
  }

  @Override
  public String toString() {
    return "JBTabs visible=" + myVisibleInfos + " selected=" + mySelectedInfo;
  }
}
