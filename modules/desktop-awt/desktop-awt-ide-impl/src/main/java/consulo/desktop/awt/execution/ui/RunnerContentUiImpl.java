/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package consulo.desktop.awt.execution.ui;

import consulo.application.ui.wm.IdeFocusManager;
import consulo.component.util.ActiveRunnable;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.execution.impl.internal.ui.layout.*;
import consulo.execution.ui.RunContentManager;
import consulo.execution.ui.layout.LayoutAttractionPolicy;
import consulo.execution.ui.layout.LayoutViewOptions;
import consulo.execution.ui.layout.PlaceInGrid;
import consulo.execution.ui.layout.RunnerLayoutUi;
import consulo.execution.impl.internal.ui.layout.action.CloseViewAction;
import consulo.execution.impl.internal.ui.layout.action.MinimizeViewAction;
import consulo.execution.impl.internal.ui.layout.action.RestoreViewAction;
import consulo.ide.impl.idea.execution.ui.layout.impl.JBRunnerTabs;
import consulo.ide.impl.idea.ide.actions.CloseAction;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.ui.tabs.impl.JBTabsImpl;
import consulo.ide.impl.idea.util.NotNullFunction;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.project.ui.wm.dock.BaseDockManager;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.dock.DockContainer;
import consulo.project.ui.wm.dock.DockManager;
import consulo.project.ui.wm.dock.DockableContent;
import consulo.project.ui.wm.dock.DragSession;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.UIBundle;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.internal.AbstractPainter;
import consulo.ui.ex.awt.internal.SwingDockContainer;
import consulo.ui.ex.awt.tab.JBTabs;
import consulo.ui.ex.awt.tab.TabInfo;
import consulo.ui.ex.awt.tab.TabsListener;
import consulo.ui.ex.awt.util.ColorUtil;
import consulo.ui.ex.awt.util.GraphicsUtil;
import consulo.ui.ex.awt.util.IdeGlassPaneUtil;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.content.ContentManager;
import consulo.ui.ex.content.ContentUI;
import consulo.ui.ex.content.event.ContentManagerEvent;
import consulo.ui.ex.content.event.ContentManagerListener;
import consulo.util.concurrent.ActionCallback;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import consulo.util.lang.ref.Ref;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import kava.beans.PropertyChangeEvent;
import kava.beans.PropertyChangeListener;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

public class RunnerContentUiImpl implements RunnerContentUi, ViewContextEx, PropertyChangeListener, QuickActionProvider, SwingDockContainer, DockContainer.Dialog {
  private static final String LAYOUT = "Runner.Layout";
  private static final String SETTINGS = "XDebugger.Settings";
  private static final String VIEW_POPUP = "Runner.View.Popup";
  static final String VIEW_TOOLBAR = "Runner.View.Toolbar";

  private ContentManager myManager;
  private final RunnerLayout myLayoutSettings;

  @Nonnull
  private final ActionManager myActionManager;
  private final String mySessionName;
  private NonOpaquePanel myComponent;

  private final Wrapper myToolbar = new Wrapper();
  final MyDragOutDelegate myDragOutDelegate = new MyDragOutDelegate();

  JBRunnerTabs myTabs;
  private final Comparator<TabInfo> myTabsComparator = (o1, o2) -> {
    //noinspection ConstantConditions
    TabImpl tab1 = getTabFor(o1);
    TabImpl tab2 = getTabFor(o2);
    int index1 = tab1 != null ? tab1.getIndex() : -1;
    int index2 = tab2 != null ? tab2.getIndex() : -1;
    return index1 - index2;
  };
  private final Project myProject;

  private ActionGroup myTopActions = new DefaultActionGroup();

  private final DefaultActionGroup myMinimizedViewActions = new DefaultActionGroup();

  private final Map<GridImpl, Wrapper> myMinimizedButtonsPlaceholder = new HashMap<>();
  private final Map<GridImpl, Wrapper> myCommonActionsPlaceholder = new HashMap<>();
  private final Map<GridImpl, AnAction[]> myContextActions = new HashMap<>();

  private boolean myUiLastStateWasRestored;

  private final Set<Object> myRestoreStateRequestors = new HashSet<>();
  private String myActionsPlace = ActionPlaces.UNKNOWN;
  private final IdeFocusManager myFocusManager;

  private boolean myMinimizeActionEnabled = true;
  private boolean myMoveToGridActionEnabled = true;
  private final RunnerLayoutUi myRunnerUi;

  private final Map<String, LayoutAttractionPolicy> myAttractions = new HashMap<>();
  private final Map<String, LayoutAttractionPolicy> myConditionAttractions = new HashMap<>();

  private ActionGroup myTabPopupActions;
  private ActionGroup myAdditionalFocusActions;

  private final ActionCallback myInitialized = new ActionCallback();
  private boolean myToDisposeRemovedContent = true;

  private int myAttractionCount;
  private ActionGroup myLeftToolbarActions;

  private boolean myContentToolbarBefore = true;
  private boolean myTopLeftActionsVisible = true;
  private boolean myTopLeftActionsBefore = false;

  private JBTabs myCurrentOver;
  private Image myCurrentOverImg;
  private TabInfo myCurrentOverInfo;
  private MyDropAreaPainter myCurrentPainter;

  private RunnerContentUiImpl myOriginal;
  private final CopyOnWriteArraySet<Listener> myDockingListeners = new CopyOnWriteArraySet<>();
  private final Set<RunnerContentUiImpl> myChildren = new TreeSet<>((o1, o2) -> o1.myWindow - o2.myWindow);
  private int myWindow;
  private boolean myDisposing;

  public RunnerContentUiImpl(@Nonnull Project project,
                             @Nonnull RunnerLayoutUi ui,
                             @Nonnull ActionManager actionManager,
                             @Nonnull IdeFocusManager focusManager,
                             @Nonnull RunnerLayout settings,
                             @Nonnull String sessionName) {
    myProject = project;
    myRunnerUi = ui;
    myLayoutSettings = settings;
    myActionManager = actionManager;
    mySessionName = sessionName;
    myFocusManager = focusManager;
  }

  public RunnerContentUiImpl(@Nonnull RunnerContentUiImpl ui, @Nonnull RunnerContentUiImpl original, int window) {
    this(ui.myProject, ui.myRunnerUi, ui.myActionManager, ui.myFocusManager, ui.myLayoutSettings, ui.mySessionName);
    myOriginal = original;
    original.myChildren.add(this);
    myWindow = window == 0 ? original.findFreeWindow() : window;
  }

  public void setTopActions(@Nonnull final ActionGroup topActions, @Nonnull String place) {
    myTopActions = topActions;
    myActionsPlace = place;

    rebuildCommonActions();
  }

  public void setTabPopupActions(ActionGroup tabPopupActions) {
    myTabPopupActions = tabPopupActions;
    rebuildTabPopup();
  }

  public void setAdditionalFocusActions(final ActionGroup group) {
    myAdditionalFocusActions = group;
    rebuildTabPopup();
  }

  public void setLeftToolbar(ActionGroup group, String place) {
    final ActionToolbar tb = myActionManager.createActionToolbar(place, group, false);
    tb.setTargetComponent(myComponent);
    myToolbar.setContent(tb.getComponent());
    myLeftToolbarActions = group;

    myComponent.revalidate();
    myComponent.repaint();
  }

  void setLeftToolbarVisible(boolean value) {
    myToolbar.setVisible(value);
//    TODO ?
//    Border border = myTabs.getComponent().getBorder();
//    if (border instanceof JBRunnerTabs.JBRunnerTabsBorder) {
//      ((JBRunnerTabs.JBRunnerTabsBorder)border).setSideMask(value ? SideBorder.LEFT : SideBorder.NONE);
//    }
    myComponent.revalidate();
    myComponent.repaint();
  }

  void setTopLeftActionsBefore(boolean value) {
    myTopLeftActionsBefore = value;
    rebuildCommonActions();
  }

  void setContentToolbarBefore(boolean value) {
    myContentToolbarBefore = value;
    getGrids().forEach(grid -> grid.setToolbarBefore(value));

    myContextActions.clear();

    rebuildToolbar();
    Set<String> usedNames = new HashSet<>();
    for (TabInfo each : myTabs.getTabs()) {
      updateTabUI(each, usedNames);
    }
    myTabs.updateTabActions(false);
  }

  void setTopLeftActionsVisible(boolean visible) {
    myTopLeftActionsVisible = visible;
    rebuildCommonActions();
  }

  public void initUi() {
    if (myTabs != null) return;

    myTabs = (JBRunnerTabs)new JBRunnerTabs(myProject, myActionManager, myFocusManager, this).setDataProvider(new DataProvider() {
                                                                                               @Override
                                                                                               public Object getData(@Nonnull Key<?> dataId) {
                                                                                                 if (ViewContext.CONTENT_KEY == dataId) {
                                                                                                   TabInfo info = myTabs.getTargetInfo();
                                                                                                   if (info != null) {
                                                                                                     return getGridFor(info).getData(dataId);
                                                                                                   }
                                                                                                 }
                                                                                                 else if (ViewContext.CONTEXT_KEY == dataId) {
                                                                                                   return RunnerContentUiImpl.this;
                                                                                                 }
                                                                                                 return null;
                                                                                               }
                                                                                             })
                                                                                             .setTabLabelActionsAutoHide(false)
                                                                                             .setInnerInsets(JBUI.emptyInsets())
                                                                                             .setToDrawBorderIfTabsHidden(false)
                                                                                             .setTabDraggingEnabled(
                                                                                               isMoveToGridActionEnabled())
                                                                                             .setUiDecorator(null)
                                                                                             .getJBTabs();
    rebuildTabPopup();

    myTabs.getPresentation().setPaintFocus(false).setRequestFocusOnLastFocusedComponent(true);
    myTabs.getComponent().setBackground(myToolbar.getBackground());
    myToolbar.setBorder(JBUI.Borders.customLine(JBColor.border(), 0, 0, 0, 1));

    final NonOpaquePanel wrappper = new MyComponent(new BorderLayout(0, 0));
    wrappper.add(myToolbar, BorderLayout.WEST);
    wrappper.add(myTabs.getComponent(), BorderLayout.CENTER);
    wrappper.setBorder(new EmptyBorder(-1, 0, 0, 0));

    myComponent = wrappper;

    myTabs.addListener(new TabsListener.Adapter() {

      @Override
      public void beforeSelectionChanged(TabInfo oldSelection, TabInfo newSelection) {
        if (oldSelection != null && !isStateBeingRestored()) {
          final GridImpl grid = getGridFor(oldSelection);
          if (grid != null && getTabFor(grid) != null) {
            grid.saveUiState();
          }
        }
      }

      @Override
      public void tabsMoved() {
        saveUiState();
      }

      @Override
      public void selectionChanged(final TabInfo oldSelection, final TabInfo newSelection) {
        if (!myTabs.getComponent().isShowing()) return;

        if (newSelection != null) {
          newSelection.stopAlerting();
          getGridFor(newSelection).processAddToUi(false);
        }

        if (oldSelection != null) {
          getGridFor(oldSelection).processRemoveFromUi();
        }
      }
    });
    myTabs.addTabMouseListener(new MouseAdapter() {
      @Override
      public void mousePressed(@Nonnull MouseEvent e) {
        if (UIUtil.isCloseClick(e)) {
          final TabInfo tabInfo = myTabs.findInfo(e);
          final GridImpl grid = tabInfo == null ? null : getGridFor(tabInfo);
          final Content[] contents = grid != null ? (Content[])grid.getData(ViewContext.CONTENT_KEY) : null;
          if (contents == null) return;
          // see GridCellImpl.closeOrMinimize as well
          if (CloseViewAction.isEnabled(contents)) {
            CloseViewAction.perform(RunnerContentUiImpl.this, contents[0]);
          }
          else if (MinimizeViewAction.isEnabled(RunnerContentUiImpl.this, contents, ViewContext.TAB_TOOLBAR_PLACE)) {
            grid.getCellFor(contents[0]).minimize(contents[0]);
          }
        }
      }
    });

    if (myOriginal != null) {
      final ContentManager manager = ContentFactory.getInstance().createContentManager(this, false, myProject);
      Disposer.register((Disposable)myRunnerUi, manager);
      manager.getComponent();
    }
    else {
      final DockManager dockManager = DockManager.getInstance(myProject);
      if (dockManager != null) { //default project
        dockManager.register(this);
      }
    }
  }

  private void rebuildTabPopup() {
    initUi();

    myTabs.setPopupGroup(getCellPopupGroup(ViewContext.TAB_POPUP_PLACE), ViewContext.TAB_POPUP_PLACE, true);

    for (GridImpl each : getGrids()) {
      each.rebuildTabPopup();
    }
  }

  @Override
  public ActionGroup getCellPopupGroup(final String place) {
    final ActionGroup original = myTabPopupActions != null ? myTabPopupActions : (ActionGroup)myActionManager.getAction(VIEW_POPUP);
    final ActionGroup focusPlaceholder = (ActionGroup)myActionManager.getAction("Runner.Focus");

    DefaultActionGroup group = new DefaultActionGroup(VIEW_POPUP, original.isPopup());

    final AnActionEvent event =
      new AnActionEvent(null, DataManager.getInstance().getDataContext(), place, new Presentation(), ActionManager.getInstance(), 0);
    final AnAction[] originalActions = original.getChildren(event);


    for (final AnAction each : originalActions) {
      if (each == focusPlaceholder) {
        final AnAction[] focusActions = ((ActionGroup)each).getChildren(event);
        for (AnAction eachFocus : focusActions) {
          group.add(eachFocus);
        }
        if (myAdditionalFocusActions != null) {
          for (AnAction action : myAdditionalFocusActions.getChildren(event)) {
            group.add(action);
          }
        }
      }
      else {
        group.add(each);
      }
    }
    return group;
  }

  @Override
  public boolean isOriginal() {
    return myOriginal == null;
  }

  @Override
  public int getWindow() {
    return myWindow;
  }

  @Override
  public void propertyChange(@Nonnull final PropertyChangeEvent evt) {
    Content content = (Content)evt.getSource();
    final GridImpl grid = getGridFor(content, false);
    if (grid == null) return;

    final GridCellImpl cell = grid.findCell(content);
    if (cell == null) return;

    final String property = evt.getPropertyName();
    if (Content.PROP_ALERT.equals(property)) {
      attract(content, true);
    }
    else if (Content.PROP_DISPLAY_NAME.equals(property) || Content.PROP_ICON.equals(property) || Content.PROP_ACTIONS.equals(property) || Content.PROP_DESCRIPTION.equals(
      property)) {
      cell.updateTabPresentation(content);
      updateTabsUI(false);
    }
  }


  public void processBounce(Content content, final boolean activate) {
    final GridImpl grid = getGridFor(content, false);
    if (grid == null) return;

    final GridCellImpl cell = grid.findCell(content);
    if (cell == null) return;


    final TabInfo tab = myTabs.findInfo(grid);
    if (tab == null) return;


    if (getSelectedGrid() != grid) {
      tab.setAlertIcon(content.getAlertIcon());
      if (activate) {
        tab.fireAlert();
      }
      else {
        tab.stopAlerting();
      }
    }
    else {
      grid.processAlert(content, activate);
    }
  }

  @Override
  public AsyncResult<Void> detachTo(int window, GridCell cell) {
    if (myOriginal != null) {
      return myOriginal.detachTo(window, cell);
    }
    RunnerContentUiImpl target = null;
    if (window > 0) {
      for (RunnerContentUiImpl child : myChildren) {
        if (child.myWindow == window) {
          target = child;
          break;
        }
      }
    }
    final GridCellImpl gridCell = (GridCellImpl)cell;
    final Content[] contents = gridCell.getContents();
    storeDefaultIndices(contents);
    for (Content content : contents) {
      content.putUserData(RunnerLayout.DROP_INDEX, getStateFor(content).getTab().getIndex());
    }
    Dimension size = TargetAWT.to(gridCell.getSize());
    if (size == null) {
      size = JBUI.size(200, 200);
    }
    final DockableGrid content = new DockableGrid(null, null, size, Arrays.asList(contents), window);
    if (target != null) {
      target.add(content, null);
    }
    else {
      Point location = TargetAWT.to(gridCell.getLocation());
      if (location == null) {
        location = getComponent().getLocationOnScreen();
      }
      location.translate(size.width / 2, size.height / 2);
      getDockManager().createNewDockContainerFor(content, new RelativePoint(location));
    }
    return AsyncResult.done(null);
  }

  private void storeDefaultIndices(@Nonnull Content[] contents) {
    //int i = 0;
    for (Content content : contents) {
      content.putUserData(RunnerLayout.DEFAULT_INDEX, getStateFor(content).getTab().getDefaultIndex());
      //content.putUserData(CONTENT_NUMBER, i++);
    }
  }

  @Override
  public RelativeRectangle getAcceptArea() {
    return new RelativeRectangle(myTabs.getComponent());
  }

  @Override
  public RelativeRectangle getAcceptAreaFallback() {
    return getAcceptArea();
  }

  @Nonnull
  @Override
  public ContentResponse getContentResponse(@Nonnull DockableContent content, RelativePoint point) {
    if (!(content instanceof DockableGrid)) {
      return ContentResponse.DENY;
    }
    final RunnerContentUiImpl ui = ((DockableGrid)content).getOriginalRunnerUi();
    return ui.getProject() == myProject && ui.mySessionName.equals(mySessionName) ? ContentResponse.ACCEPT_MOVE : ContentResponse.DENY;
  }

  @Override
  public JComponent getComponent() {
    initUi();
    return myComponent;
  }

  @Override
  public JComponent getContainerComponent() {
    initUi();
    return myManager.getComponent();
  }

  @Override
  public void add(@Nonnull DockableContent dockable, RelativePoint dropTarget) {
    final DockableGrid dockableGrid = (DockableGrid)dockable;
    final RunnerContentUiImpl prev = dockableGrid.getRunnerUi();

    saveUiState();

    final List<Content> contents = dockableGrid.getContents();
    final boolean wasRestoring = myOriginal != null && myOriginal.isStateBeingRestored();
    setStateIsBeingRestored(true, this);
    try {
      final Point point = dropTarget != null ? dropTarget.getPoint(myComponent) : null;
      boolean hadGrid = !myTabs.shouldAddToGlobal(point);

      for (Content content : contents) {
        final View view = getStateFor(content);
        if (view.isMinimizedInGrid()) continue;
        prev.myManager.removeContent(content, false);
        myManager.removeContent(content, false);
        if (hadGrid && !wasRestoring) {
          view.assignTab(getTabFor(getSelectedGrid()));
          view.setPlaceInGrid(calcPlaceInGrid(point, myComponent.getSize()));
        }
        else if (contents.size() == 1 && !wasRestoring) {
          view.assignTab(null);
          view.setPlaceInGrid(myLayoutSettings.getDefaultGridPlace(content));
        }
        view.setWindow(myWindow);
        myManager.addContent(content);
      }
    }
    finally {
      setStateIsBeingRestored(false, this);
    }

    saveUiState();

    updateTabsUI(true);
  }

  @Override
  public void closeAll() {
    final Content[] contents = myManager.getContents();
    if (myOriginal != null) {
      for (Content content : contents) {
        getStateFor(content).setWindow(0);
        myOriginal.myManager.addContent(content);
        GridCell cell = myOriginal.findCellFor(content);
        if (cell != null) {
          myOriginal.restoreContent(content.getUserData(ViewImpl.ID));
          cell.minimize(content);
        }
      }
    }
    myManager.removeAllContents(false);
  }

  @Override
  public void addListener(final Listener listener, Disposable parent) {
    myDockingListeners.add(listener);
    Disposer.register(parent, new Disposable() {
      @Override
      public void dispose() {
        myDockingListeners.remove(listener);
      }
    });
  }

  @Override
  public boolean isEmpty() {
    return myTabs.isEmptyVisible() || myDisposing;
  }

  @Override
  public Image startDropOver(@Nonnull DockableContent content, RelativePoint point) {
    return null;
  }

  @Override
  public Image processDropOver(@Nonnull DockableContent dockable, RelativePoint dropTarget) {
    JBTabs current = getTabsAt(dockable, dropTarget);

    if (myCurrentOver != null && myCurrentOver != current) {
      resetDropOver(dockable);
    }

    if (myCurrentOver == null && current != null) {
      myCurrentOver = current;
      Presentation presentation = dockable.getPresentation();
      myCurrentOverInfo = new TabInfo(new JLabel("")).setText(presentation.getText()).setIcon(presentation.getIcon());
      myCurrentOverImg = myCurrentOver.startDropOver(myCurrentOverInfo, dropTarget);
    }

    if (myCurrentOver != null) {
      myCurrentOver.processDropOver(myCurrentOverInfo, dropTarget);
    }

    if (myCurrentPainter == null) {
      myCurrentPainter = new MyDropAreaPainter();
      IdeGlassPaneUtil.find(myComponent).addPainter(myComponent, myCurrentPainter, this);
    }
    myCurrentPainter.processDropOver(this, dockable, dropTarget);

    return myCurrentOverImg;
  }

  @Nonnull
  private static PlaceInGrid calcPlaceInGrid(Point point, Dimension size) {
    // 1/3 (left) |   (center/bottom) | 1/3 (right)
    if (point.x < size.width / 3) return PlaceInGrid.left;
    if (point.x > size.width * 2 / 3) return PlaceInGrid.right;

    // 3/4 (center with tab titles) | 1/4 (bottom)
    if (point.y > size.height * 3 / 4) return PlaceInGrid.bottom;

    return PlaceInGrid.center;
  }

  @Nullable
  private JBTabs getTabsAt(DockableContent content, RelativePoint point) {
    if (content instanceof DockableGrid) {
      final Point p = point.getPoint(getComponent());
      Component c = SwingUtilities.getDeepestComponentAt(getComponent(), p.x, p.y);
      while (c != null) {
        if (c instanceof JBRunnerTabs) {
          return (JBTabs)c;
        }
        c = c.getParent();
      }
    }
    return null;
  }

  @Override
  public void resetDropOver(@Nonnull DockableContent content) {
    if (myCurrentOver != null) {
      myCurrentOver.resetDropOver(myCurrentOverInfo);
      myCurrentOver = null;
      myCurrentOverInfo = null;
      myCurrentOverImg = null;

      IdeGlassPaneUtil.find(myComponent).removePainter(myCurrentPainter);
      myCurrentPainter = null;
    }
  }

  @Override
  public boolean isDisposeWhenEmpty() {
    return myOriginal != null;
  }

  @Override
  public boolean isCycleRoot() {
    return false;
  }

  @Override
  public void setManager(@Nonnull final ContentManager manager) {
    assert myManager == null;

    myManager = manager;
    myManager.addContentManagerListener(new ContentManagerListener() {
      @Override
      public void contentAdded(final ContentManagerEvent event) {
        initUi();
        if (event.getContent().getUserData(RunContentManager.LIGHTWEIGHT_CONTENT_MARKER) == Boolean.TRUE) {
          myLayoutSettings.setLightWeight(event.getContent());
          Disposer.register(event.getContent(), new Disposable() {
            @Override
            public void dispose() {
              myLayoutSettings.clearStateFor(event.getContent());
            }
          });
        }


        GridImpl grid = getGridFor(event.getContent(), true);
        if (grid == null) {
          return;
        }

        grid.add(event.getContent());

        if (getSelectedGrid() == grid) {
          grid.processAddToUi(false);
        }

        if (myManager.getComponent().isShowing() && !isStateBeingRestored()) {
          grid.restoreLastUiState();
        }

        updateTabsUI(false);

        event.getContent().addPropertyChangeListener(RunnerContentUiImpl.this);
        fireContentOpened(event.getContent());
      }

      @Override
      public void contentRemoved(final ContentManagerEvent event) {
        event.getContent().removePropertyChangeListener(RunnerContentUiImpl.this);

        GridImpl grid = (GridImpl)findGridFor(event.getContent());
        if (grid != null) {
          grid.remove(event.getContent());
          grid.processRemoveFromUi();
          removeGridIfNeeded(grid);
        }
        updateTabsUI(false);
        fireContentClosed(event.getContent());
      }

      @Override
      public void contentRemoveQuery(final ContentManagerEvent event) {
      }

      @Override
      public void selectionChanged(final ContentManagerEvent event) {
        if (isStateBeingRestored()) return;

        if (event.getOperation() == ContentManagerEvent.ContentOperation.add) {
          select(event.getContent(), false);
        }
      }
    });
  }

  @Nullable
  private GridImpl getSelectedGrid() {
    TabInfo selection = myTabs.getSelectedInfo();
    return selection != null ? getGridFor(selection) : null;
  }

  private void removeGridIfNeeded(GridImpl grid) {
    if (grid.isEmpty()) {
      myTabs.removeTab(myTabs.findInfo(grid));
      myMinimizedButtonsPlaceholder.remove(grid);
      myCommonActionsPlaceholder.remove(grid);
      Disposer.dispose(grid);
    }
  }

  @Nullable
  private GridImpl getGridFor(@Nonnull Content content, boolean createIfMissing) {
    GridImpl grid = (GridImpl)findGridFor(content);
    if (grid != null || !createIfMissing) return grid;

    grid = new GridImpl(this, mySessionName);

    if (myCurrentOver != null || myOriginal != null) {
      Integer forcedDropIndex = content.getUserData(RunnerLayout.DROP_INDEX);
      final int index = myTabs.getDropInfoIndex() + (myOriginal != null ? myOriginal.getTabOffsetFor(this) : 0);
      final int dropIndex = forcedDropIndex != null ? forcedDropIndex : index;
      if (forcedDropIndex == null) {
        moveFollowingTabs(dropIndex);
      }
      final int defaultIndex = content.getUserData(RunnerLayout.DEFAULT_INDEX);
      final TabImpl tab = myLayoutSettings.getOrCreateTab(forcedDropIndex != null ? forcedDropIndex : -1);
      tab.setDefaultIndex(defaultIndex);
      tab.setIndex(dropIndex);
      getStateFor(content).assignTab(tab);
      content.putUserData(RunnerLayout.DROP_INDEX, null);
      content.putUserData(RunnerLayout.DEFAULT_INDEX, null);
    }

    TabInfo tab = new TabInfo(grid).setObject(getStateFor(content).getTab()).setText("Tab");


    Wrapper left = new Wrapper();
    myCommonActionsPlaceholder.put(grid, left);


    Wrapper minimizedToolbar = new Wrapper();
    myMinimizedButtonsPlaceholder.put(grid, minimizedToolbar);


    final Wrapper searchComponent = new Wrapper();
    if (content.getSearchComponent() != null) {
      searchComponent.setContent(content.getSearchComponent());
    }

    TwoSideComponent right = new TwoSideComponent(searchComponent, minimizedToolbar);


    NonOpaquePanel sideComponent = new TwoSideComponent(left, right);

    tab.setSideComponent(sideComponent);

    tab.setTabLabelActions((ActionGroup)myActionManager.getAction(VIEW_TOOLBAR), ViewContext.TAB_TOOLBAR_PLACE);

    myTabs.addTab(tab);
    myTabs.sortTabs(myTabsComparator);

    return grid;
  }

  private void moveFollowingTabs(int index) {
    if (myOriginal != null) {
      myOriginal.moveFollowingTabs(index);
      return;
    }
    moveFollowingTabs(index, myTabs);
    for (RunnerContentUiImpl child : myChildren) {
      moveFollowingTabs(index, child.myTabs);
    }
  }

  public ActionGroup getSettingsActions() {
    return (ActionGroup)myActionManager.getAction(SETTINGS);
  }

  public ContentManager getContentManager(Content content) {
    if (hasContent(myManager, content)) {
      return myManager;
    }
    for (RunnerContentUiImpl child : myChildren) {
      if (hasContent(child.myManager, content)) {
        return child.myManager;
      }
    }
    return myManager;
  }

  private static boolean hasContent(ContentManager manager, Content content) {
    for (Content c : manager.getContents()) {
      if (c == content) {
        return true;
      }
    }
    return false;
  }

  private static void moveFollowingTabs(int index, final JBRunnerTabs tabs) {
    for (TabInfo info : tabs.getTabs()) {
      TabImpl tab = getTabFor(info);
      if (tab != null) {
        int tabIndex = tab.getIndex();
        if (tabIndex >= index) {
          tab.setIndex(tabIndex + 1);
        }
      }
    }
  }

  private int getTabOffsetFor(RunnerContentUiImpl ui) {
    int offset = myTabs.getTabCount();
    for (RunnerContentUiImpl child : myChildren) {
      if (child == ui) break;
      offset += child.myTabs.getTabCount();
    }
    return offset;
  }

  @Override
  @Nullable
  public GridCell findCellFor(@Nonnull final Content content) {
    GridImpl cell = getGridFor(content, false);
    return cell != null ? cell.getCellFor(content) : null;
  }

  private boolean rebuildToolbar() {
    boolean hasToolbarContent = rebuildCommonActions();
    hasToolbarContent |= rebuildMinimizedActions();
    return hasToolbarContent;
  }

  private boolean rebuildCommonActions() {
    boolean hasToolbarContent = false;
    for (Map.Entry<GridImpl, Wrapper> entry : myCommonActionsPlaceholder.entrySet()) {
      Wrapper eachPlaceholder = entry.getValue();
      List<Content> contentList = entry.getKey().getContents();

      Set<Content> contents = new HashSet<>();
      contents.addAll(contentList);

      DefaultActionGroup groupToBuild;
      JComponent contextComponent = null;
      if (isHorizontalToolbar() && contents.size() == 1) {
        Content content = contentList.get(0);
        groupToBuild = new DefaultActionGroup();
        if (content.getActions() != null) {
          groupToBuild.addAll(content.getActions());
          groupToBuild.addSeparator();
          contextComponent = content.getActionsContextComponent();
        }
        groupToBuild.addAll(myTopActions);
      }
      else {
        final DefaultActionGroup group = new DefaultActionGroup();
        group.addAll(myTopActions);
        groupToBuild = group;
      }

      final AnAction[] actions = groupToBuild.getChildren(null);
      if (!Arrays.equals(actions, myContextActions.get(entry.getKey()))) {
        String adjustedPlace = ActionPlaces.UNKNOWN.equals(myActionsPlace) ? ActionPlaces.TOOLBAR : myActionsPlace;
        ActionToolbar tb = myActionManager.createActionToolbar(adjustedPlace, groupToBuild, true);
        tb.setTargetComponent(contextComponent);
        eachPlaceholder.setContent(tb.getComponent());
      }

      if (groupToBuild.getChildrenCount() > 0) {
        hasToolbarContent = true;
      }

      myContextActions.put(entry.getKey(), actions);
    }

    return hasToolbarContent;
  }

  private boolean rebuildMinimizedActions() {
    for (Map.Entry<GridImpl, Wrapper> entry : myMinimizedButtonsPlaceholder.entrySet()) {
      Wrapper eachPlaceholder = entry.getValue();
      ActionToolbar tb = myActionManager.createActionToolbar(ActionPlaces.DEBUGGER_TOOLBAR, myMinimizedViewActions, true);
      tb.setTargetComponent(myComponent);
      tb.getComponent().setBorder(JBUI.Borders.empty());
      tb.setReservePlaceAutoPopupIcon(false);
      JComponent minimized = tb.getComponent();
      eachPlaceholder.setContent(minimized);
    }

    myTabs.getComponent().revalidate();
    myTabs.getComponent().repaint();

    return myMinimizedViewActions.getChildrenCount() > 0;
  }

  private void updateTabsUI(final boolean validateNow) {
    boolean hasToolbarContent = rebuildToolbar();

    Set<String> usedNames = new HashSet<>();
    List<TabInfo> tabs = myTabs.getTabs();
    for (TabInfo each : tabs) {
      hasToolbarContent |= updateTabUI(each, usedNames);
    }
    int tabsCount = tabs.size() + myChildren.stream().mapToInt(child -> child.myTabs.getTabCount()).sum();
    myTabs.getPresentation().setHideTabs(!hasToolbarContent && tabsCount <= 1 && myOriginal == null);
    myTabs.updateTabActions(validateNow);

    if (validateNow) {
      myTabs.sortTabs(myTabsComparator);
    }
  }

  private boolean updateTabUI(TabInfo tab, Set<String> usedNames) {
    TabImpl t = getTabFor(tab);
    if (t == null) {
      return false;
    }

    consulo.ui.image.Image icon = t.getIcon();

    GridImpl grid = getGridFor(tab);
    boolean hasToolbarContent = grid.updateGridUI();

    List<Content> contents = grid.getContents();
    String title = contents.size() > 1 ? t.getDisplayName() : null;
    if (title == null) {
      final String name = myLayoutSettings.getDefaultDisplayName(t.getDefaultIndex());
      if (name != null && contents.size() > 1 && !usedNames.contains(name)) {
        title = name;
      }
      else {
        title = StringUtil.join(contents, (NotNullFunction<Content, String>)dom -> dom.getTabName(), " | ");
      }
    }
    usedNames.add(title);

    boolean hidden = true;
    for (Content content : contents) {
      if (!grid.isMinimized(content)) {
        hidden = false;
        break;
      }
    }
    tab.setHidden(hidden);
    if (icon == null && contents.size() == 1) {
      icon = contents.get(0).getIcon();
    }

    tab.setDragOutDelegate(myTabs.getTabs().size() > 1 || !isOriginal() ? myDragOutDelegate : null);

    Tab gridTab = grid.getTab();
    tab.setText(title).setIcon(gridTab != null && gridTab.isDefault() && contents.size() > 1 ? null : icon);

    return hasToolbarContent;
  }

  private ActionCallback restoreLastUiState() {
    if (isStateBeingRestored()) return ActionCallback.REJECTED;

    try {
      setStateIsBeingRestored(true, this);

      List<TabInfo> tabs = new ArrayList<>();
      tabs.addAll(myTabs.getTabs());

      final ActionCallback result = new ActionCallback(tabs.size());

      for (TabInfo each : tabs) {
        getGridFor(each).restoreLastUiState().notifyWhenDone(result);
      }

      return result;
    }
    finally {
      setStateIsBeingRestored(false, this);
    }
  }

  @Override
  public void saveUiState() {
    if (isStateBeingRestored()) return;

    if (myOriginal != null) {
      myOriginal.saveUiState();
      return;
    }
    int offset = updateTabsIndices(myTabs, 0);
    for (RunnerContentUiImpl child : myChildren) {
      offset = updateTabsIndices(child.myTabs, offset);
    }

    doSaveUiState();
  }

  private static int updateTabsIndices(final JBRunnerTabs tabs, int offset) {
    for (TabInfo each : tabs.getTabs()) {
      final int index = tabs.getIndexOf(each);
      final TabImpl tab = getTabFor(each);
      if (tab != null) tab.setIndex(index >= 0 ? index + offset : index);
    }
    return offset + tabs.getTabCount();
  }

  private void doSaveUiState() {
    if (isStateBeingRestored()) return;

    for (TabInfo each : myTabs.getTabs()) {
      GridImpl eachGrid = getGridFor(each);
      eachGrid.saveUiState();
    }

    for (RunnerContentUiImpl child : myChildren) {
      child.doSaveUiState();
    }
  }

  @Override
  @Nullable
  public Tab getTabFor(final Grid grid) {
    TabInfo info = myTabs.findInfo((Component)grid);
    return getTabFor(info);
  }

  @Override
  public void showNotify() {
    Window awtWindow = SwingUtilities.getWindowAncestor(myComponent);
    consulo.ui.Window uiWindow = TargetAWT.from(awtWindow);

    IdeFrame ideFrame = uiWindow.getUserData(IdeFrame.KEY);
    if (ideFrame instanceof IdeFrame.Child) {
      ideFrame.setFrameTitle(mySessionName);
    }
  }

  @Override
  public void hideNotify() {
  }

  @Nullable
  private static TabImpl getTabFor(@Nullable final TabInfo tab) {
    if (tab == null) {
      return null;
    }
    return (TabImpl)tab.getObject();
  }

  private static GridImpl getGridFor(TabInfo tab) {
    return (GridImpl)tab.getComponent();
  }

  @Override
  @Nullable
  public Grid findGridFor(@Nonnull Content content) {
    TabImpl tab = (TabImpl)getStateFor(content).getTab();
    for (TabInfo each : myTabs.getTabs()) {
      TabImpl t = getTabFor(each);
      if (t != null && t.equals(tab)) return getGridFor(each);
    }

    return null;
  }

  private ArrayList<GridImpl> getGrids() {
    return myTabs.getTabs().stream().map(RunnerContentUiImpl::getGridFor).collect(Collectors.toCollection(ArrayList::new));
  }


  public void setHorizontalToolbar(final boolean state) {
    myLayoutSettings.setToolbarHorizontal(state);
    for (GridImpl each : getGrids()) {
      each.setToolbarHorizontal(state);
    }

    myContextActions.clear();
    updateTabsUI(false);
  }

  @Override
  public boolean isSingleSelection() {
    return false;
  }

  @Override
  public boolean isToSelectAddedContent() {
    return false;
  }

  @Override
  public boolean canBeEmptySelection() {
    return true;
  }

  @Override
  public void beforeDispose() {
    if (myOriginal != null) {
      myDisposing = true;
      fireContentClosed(null);
    }
  }

  @Override
  public boolean canChangeSelectionTo(@Nonnull Content content, boolean implicit) {
    if (implicit) {
      GridImpl grid = getGridFor(content, false);
      if (grid != null) {
        return !grid.isMinimized(content);
      }
    }

    return true;
  }

  @Nonnull
  @Override
  public String getCloseActionName() {
    return UIBundle.message("tabbed.pane.close.tab.action.name");
  }

  @Nonnull
  @Override
  public String getCloseAllButThisActionName() {
    return UIBundle.message("tabbed.pane.close.all.tabs.but.this.action.name");
  }

  @Nonnull
  @Override
  public String getPreviousContentActionName() {
    return "Select Previous Tab";
  }

  @Nonnull
  @Override
  public String getNextContentActionName() {
    return "Select Next Tab";
  }

  @Override
  public void dispose() {
    if (myOriginal != null) {
      myOriginal.myChildren.remove(this);
    }
    myMinimizedButtonsPlaceholder.clear();
    myCommonActionsPlaceholder.clear();
    myContextActions.clear();

    myOriginal = null;
    myTopActions = null;
    myAdditionalFocusActions = null;
    myLeftToolbarActions = null;
  }

  @Override
  public void restoreLayout() {
    final RunnerContentUiImpl[] children = myChildren.toArray(new RunnerContentUiImpl[myChildren.size()]);
    final List<Content> contents = new ArrayList<>();
    Collections.addAll(contents, myManager.getContents());
    for (RunnerContentUiImpl child : children) {
      Collections.addAll(contents, child.myManager.getContents());
    }
    for (AnAction action : myMinimizedViewActions.getChildren(null)) {
      final Content content = ((RestoreViewAction)action).getContent();
      contents.add(content);
    }
    Content[] all = contents.toArray(new Content[contents.size()]);
    Arrays.sort(all, (content, content1) -> {
      final int i = getStateFor(content).getTab().getDefaultIndex();
      final int i1 = getStateFor(content1).getTab().getDefaultIndex();
      return i - i1;
    });

    setStateIsBeingRestored(true, this);
    try {
      for (RunnerContentUiImpl child : children) {
        child.myManager.removeAllContents(false);
      }
      myManager.removeAllContents(false);
      myMinimizedViewActions.removeAll();
    }
    finally {
      setStateIsBeingRestored(false, this);
    }

    myLayoutSettings.resetToDefault();
    for (Content each : all) {
      myManager.addContent(each);
    }

    updateTabsUI(true);
  }

  @Override
  public boolean isStateBeingRestored() {
    return !myRestoreStateRequestors.isEmpty();
  }

  @Override
  public void setStateIsBeingRestored(final boolean restoredNow, final Object requestor) {
    if (restoredNow) {
      myRestoreStateRequestors.add(requestor);
    }
    else {
      myRestoreStateRequestors.remove(requestor);
    }
  }

  public ActionGroup getLayoutActions() {
    return (ActionGroup)myActionManager.getAction(LAYOUT);
  }

  public void updateActionsImmediately() {
    if (myToolbar.getTargetComponent() instanceof ActionToolbar) {
      ((ActionToolbar)myToolbar.getTargetComponent()).updateActionsImmediately();
    }
  }

  public void setMinimizeActionEnabled(final boolean enabled) {
    myMinimizeActionEnabled = enabled;
  }

  @SuppressWarnings("SpellCheckingInspection")
  public void setMovetoGridActionEnabled(final boolean enabled) {
    myMoveToGridActionEnabled = enabled;
  }

  @Override
  public boolean isMinimizeActionEnabled() {
    return myMinimizeActionEnabled && myOriginal == null;
  }

  @Override
  public boolean isMoveToGridActionEnabled() {
    return myMoveToGridActionEnabled;
  }

  public void setPolicy(String contentId, final LayoutAttractionPolicy policy) {
    myAttractions.put(contentId, policy);
  }

  public void setConditionPolicy(final String condition, final LayoutAttractionPolicy policy) {
    myConditionAttractions.put(condition, policy);
  }

  private static LayoutAttractionPolicy getOrCreatePolicyFor(String key,
                                                             Map<String, LayoutAttractionPolicy> map,
                                                             LayoutAttractionPolicy defaultPolicy) {
    LayoutAttractionPolicy policy = map.get(key);
    if (policy == null) {
      policy = defaultPolicy;
      map.put(key, policy);
    }
    return policy;
  }

  @Nullable
  public Content findContent(final String key) {
    final ContentManager manager = getContentManager();
    if (manager == null || key == null) return null;

    Content[] contents = manager.getContents();
    for (Content content : contents) {
      String kind = content.getUserData(ViewImpl.ID);
      if (key.equals(kind)) {
        return content;
      }
    }

    return null;
  }

  public void restoreContent(final String key) {
    for (AnAction action : myMinimizedViewActions.getChildren(null)) {
      Content content = ((RestoreViewAction)action).getContent();
      if (key.equals(content.getUserData(ViewImpl.ID))) {
        action.actionPerformed(null);
        return;
      }
    }
  }

  public void setToDisposeRemovedContent(final boolean toDispose) {
    myToDisposeRemovedContent = toDispose;
  }

  @Override
  public boolean isToDisposeRemovedContent() {
    return myToDisposeRemovedContent;
  }

  private static class MyDropAreaPainter extends AbstractPainter {
    private Shape myBoundingBox;
    private final Color myColor = ColorUtil.mix(JBColor.BLUE, JBColor.WHITE, .3);

    @Override
    public boolean needsRepaint() {
      return myBoundingBox != null;
    }

    @Override
    public void executePaint(Component component, Graphics2D g) {
      if (myBoundingBox == null) return;
      GraphicsUtil.setupAAPainting(g);
      g.setColor(ColorUtil.toAlpha(myColor, 200));
      g.setStroke(new BasicStroke(2));
      g.draw(myBoundingBox);
      g.setColor(ColorUtil.toAlpha(myColor, 40));
      g.fill(myBoundingBox);
    }

    private void processDropOver(RunnerContentUiImpl ui, DockableContent dockable, RelativePoint dropTarget) {
      myBoundingBox = null;
      setNeedsRepaint(true);

      if (!(dockable instanceof DockableGrid)) return;

      JComponent component = ui.myComponent;
      Point point = dropTarget != null ? dropTarget.getPoint(component) : null;

      // do not paint anything if adding to the top
      if (ui.myTabs.shouldAddToGlobal(point)) return;

      // calc target place-in-grid
      PlaceInGrid targetPlaceInGrid = null;
      for (Content c : ((DockableGrid)dockable).getContents()) {
        View view = ui.getStateFor(c);
        if (view.isMinimizedInGrid()) continue;
        PlaceInGrid defaultGridPlace = ui.getLayoutSettings().getDefaultGridPlace(c);
        targetPlaceInGrid = point == null ? defaultGridPlace : calcPlaceInGrid(point, component.getSize());
        break;
      }
      if (targetPlaceInGrid == null) return;

      // calc the default rectangle for the targetPlaceInGrid "area"
      Dimension size = component.getSize();
      Rectangle r = new Rectangle(size);
      switch (targetPlaceInGrid) {
        case left:
          r.width /= 3;
          break;
        case center:
          r.width /= 3;
          r.x += r.width;
          break;
        case right:
          r.width /= 3;
          r.x += 2 * r.width;
          break;
        case bottom:
          r.height /= 4;
          r.y += 3 * r.height;
          break;
      }
      // adjust the rectangle if the target grid cell is already present and showing
      for (Content c : ui.getContentManager().getContents()) {
        GridCell cellFor = ui.findCellFor(c);
        PlaceInGrid placeInGrid = cellFor == null ? null : ((GridCellImpl)cellFor).getPlaceInGrid();
        if (placeInGrid != targetPlaceInGrid) continue;
        Wrapper wrapper = UIUtil.getParentOfType(Wrapper.class, c.getComponent());
        JComponent cellWrapper = wrapper == null ? null : (JComponent)wrapper.getParent();
        if (cellWrapper == null || !cellWrapper.isShowing()) continue;
        r = new RelativeRectangle(cellWrapper).getRectangleOn(component);
        break;
      }
      myBoundingBox = new RoundRectangle2D.Double(r.x, r.y, r.width, r.height, 16, 16);
    }
  }

  private class MyComponent extends NonOpaquePanel implements DataProvider, QuickActionProvider {
    private boolean myWasEverAdded;

    public MyComponent(LayoutManager layout) {
      super(layout);
      setOpaque(true);
      setFocusCycleRoot(true);
    }

    @Override
    @Nullable
    public Object getData(@Nonnull final Key<?> dataId) {
      if (KEY == dataId) {
        return RunnerContentUiImpl.this;
      }
      else if (CloseAction.CloseTarget.KEY == dataId) {
        Content content = getContentManager().getSelectedContent();
        if (content != null && content.getManager().canCloseContents() && content.isCloseable()) {
          return (CloseAction.CloseTarget)() -> content.getManager().removeContent(content, true, true, true);
        }
      }

      ContentManager originalContentManager = myOriginal == null ? null : myOriginal.getContentManager();
      JComponent originalContentComponent = originalContentManager == null ? null : originalContentManager.getComponent();
      if (originalContentComponent instanceof DataProvider) {
        return ((DataProvider)originalContentComponent).getData(dataId);
      }
      return null;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public String getName() {
      return RunnerContentUiImpl.this.getName();
    }

    @Override
    public List<AnAction> getActions(boolean originalProvider) {
      return RunnerContentUiImpl.this.getActions(originalProvider);
    }

    @Override
    public JComponent getComponent() {
      return RunnerContentUiImpl.this.getComponent();
    }

    @Override
    public void addNotify() {
      super.addNotify();

      if (!myUiLastStateWasRestored && myOriginal == null) {
        myUiLastStateWasRestored = true;

        // [kirillk] this is done later since restoreUiState doesn't work properly in the addNotify call chain
        //todo to investigate and to fix (may cause extra flickering)
        //noinspection SSBasedInspection
        SwingUtilities.invokeLater(() -> restoreLastUiState().doWhenDone(() -> {
          if (!myWasEverAdded) {
            myWasEverAdded = true;
            attractOnStartup();
            myInitialized.setDone();
          }
        }));
      }
    }

    @Override
    public void removeNotify() {
      super.removeNotify();
      if (!ScreenUtil.isStandardAddRemoveNotify(this)) return;

      if (Disposer.isDisposed(RunnerContentUiImpl.this)) return;

      saveUiState();
    }
  }

  @SuppressWarnings({"SSBasedInspection"})
  // [kirillk] this is done later since "startup" attractions should be done gently, only if no explicit calls are done
  private void attractOnStartup() {
    final int currentCount = myAttractionCount;
    SwingUtilities.invokeLater(() -> {
      if (currentCount < myAttractionCount) return;
      attractByCondition(LayoutViewOptions.STARTUP, false);
    });
  }

  public void attract(final Content content, boolean afterInitialized) {
    processAttraction(content.getUserData(ViewImpl.ID), myAttractions, new LayoutAttractionPolicy.Bounce(), afterInitialized, true);
  }

  public void attractByCondition(@Nonnull String condition, boolean afterInitialized) {
    processAttraction(myLayoutSettings.getToFocus(condition),
                      myConditionAttractions,
                      myLayoutSettings.getAttractionPolicy(condition),
                      afterInitialized,
                      true);
  }

  public void clearAttractionByCondition(String condition, boolean afterInitialized) {
    processAttraction(myLayoutSettings.getToFocus(condition),
                      myConditionAttractions,
                      new LayoutAttractionPolicy.FocusOnce(),
                      afterInitialized,
                      false);
  }

  private void processAttraction(final String contentId,
                                 final Map<String, LayoutAttractionPolicy> policyMap,
                                 final LayoutAttractionPolicy defaultPolicy,
                                 final boolean afterInitialized,
                                 final boolean activate) {
    ProjectIdeFocusManager.getInstance(getProject()).doWhenFocusSettlesDown(() -> myInitialized.processOnDone(() -> {
      Content content = findContent(contentId);
      if (content == null) return;

      final LayoutAttractionPolicy policy = getOrCreatePolicyFor(contentId, policyMap, defaultPolicy);
      if (activate) {
        // See IDEA-93683, bounce attraction should not disable further focus attraction
        if (!(policy instanceof LayoutAttractionPolicy.Bounce)) {
          myAttractionCount++;
        }
        policy.attract(content, myRunnerUi);
      }
      else {
        policy.clearAttraction(content, myRunnerUi);
      }
    }, afterInitialized));
  }


  public static boolean ensureValid(JComponent c) {
    if (c.getRootPane() == null) return false;

    Container eachParent = c.getParent();
    while (eachParent != null && eachParent.isValid()) {
      eachParent = eachParent.getParent();
    }

    if (eachParent == null) {
      eachParent = c.getRootPane();
    }

    eachParent.validate();

    return true;
  }

  public ContentUI getContentUI() {
    return this;
  }

  @Override
  public void minimize(final Content content, final CellTransform.Restore restore) {
    final Ref<AnAction> restoreAction = new Ref<>();
    myManager.removeContent(content, false);
    restoreAction.set(new RestoreViewAction(content, new CellTransform.Restore() {
      @Override
      public ActionCallback restoreInGrid() {
        myMinimizedViewActions.remove(restoreAction.get());
        final GridImpl grid = getGridFor(content, false);
        if (grid == null) {
          getStateFor(content).assignTab(myLayoutSettings.getOrCreateTab(-1));
        }
        else {
          //noinspection ConstantConditions
          ((GridCellImpl)findCellFor(content)).restore(content);
        }
        getStateFor(content).setMinimizedInGrid(false);
        myManager.addContent(content);
        saveUiState();
        select(content, true);
        updateTabsUI(false);
        return ActionCallback.DONE;
      }
    }));

    myMinimizedViewActions.add(restoreAction.get());

    saveUiState();
    updateTabsUI(false);
  }

  @Override
  public Project getProject() {
    return myProject;
  }

  @Override
  public CellTransform.Facade getCellTransform() {
    return this;
  }

  @Override
  public ContentManager getContentManager() {
    return myManager;
  }

  @Nonnull
  @Override
  public ActionManager getActionManager() {
    return myActionManager;
  }

  @Override
  public RunnerLayout getLayoutSettings() {
    return myLayoutSettings;
  }

  @Override
  public View getStateFor(@Nonnull final Content content) {
    return myLayoutSettings.getStateFor(content);
  }

  public boolean isHorizontalToolbar() {
    return myLayoutSettings.isToolbarHorizontal();
  }

  @Override
  public ActionCallback select(@Nonnull final Content content, final boolean requestFocus) {
    final GridImpl grid = (GridImpl)findGridFor(content);
    if (grid == null) return ActionCallback.DONE;


    final TabInfo info = myTabs.findInfo(grid);
    if (info == null) return ActionCallback.DONE;


    final ActionCallback result = new ActionCallback();
    myTabs.select(info, false).doWhenDone(() -> grid.select(content, requestFocus).notifyWhenDone(result));


    return result;
  }

  @Override
  public void validate(Content content, final ActiveRunnable toRestore) {
    final TabInfo current = myTabs.getSelectedInfo();
    myTabs.getPresentation().setPaintBlocked(true, true);

    select(content, false).doWhenDone(() -> {
      myTabs.getComponent().validate();
      toRestore.run().doWhenDone(() -> {
        assert current != null;
        myTabs.select(current, true);
        myTabs.getPresentation().setPaintBlocked(false, true);
      });
    });
  }

  private static class TwoSideComponent extends NonOpaquePanel {
    private TwoSideComponent(JComponent left, JComponent right) {
      setLayout(new CommonToolbarLayout(left, right));
      add(left);
      add(right);
    }
  }

  private static class CommonToolbarLayout extends AbstractLayoutManager {
    private final JComponent myLeft;
    private final JComponent myRight;

    public CommonToolbarLayout(final JComponent left, final JComponent right) {
      myLeft = left;
      myRight = right;
    }

    @Override
    public Dimension preferredLayoutSize(@Nonnull final Container parent) {

      Dimension size = new Dimension();
      Dimension leftSize = myLeft.getPreferredSize();
      Dimension rightSize = myRight.getPreferredSize();

      size.width = leftSize.width + rightSize.width;
      size.height = Math.max(leftSize.height, rightSize.height);

      return size;
    }

    @Override
    public void layoutContainer(@Nonnull final Container parent) {
      Dimension size = parent.getSize();
      Dimension prefSize = parent.getPreferredSize();
      if (prefSize.width <= size.width) {
        myLeft.setBounds(0, 0, myLeft.getPreferredSize().width, parent.getHeight());
        Dimension rightSize = myRight.getPreferredSize();
        myRight.setBounds(parent.getWidth() - rightSize.width, 0, rightSize.width, parent.getHeight());
      }
      else {
        Dimension leftMinSize = myLeft.getMinimumSize();
        Dimension rightMinSize = myRight.getMinimumSize();

        // see IDEA-140557, always shrink left component last
        int delta = 0;
        //int delta = (prefSize.width - size.width) / 2;

        myLeft.setBounds(0, 0, myLeft.getPreferredSize().width - delta, parent.getHeight());
        int rightX = (int)myLeft.getBounds().getMaxX();
        int rightWidth = size.width - rightX;
        if (rightWidth < rightMinSize.width) {
          Dimension leftSize = myLeft.getSize();
          int diffToRightMin = rightMinSize.width - rightWidth;
          if (leftSize.width - diffToRightMin >= leftMinSize.width) {
            leftSize.width -= diffToRightMin;
            myLeft.setSize(leftSize);
          }
        }

        myRight.setBounds((int)myLeft.getBounds().getMaxX(), 0, parent.getWidth() - myLeft.getWidth(), parent.getHeight());
      }

      toMakeVerticallyInCenter(myLeft, parent);
      toMakeVerticallyInCenter(myRight, parent);
    }

    private static void toMakeVerticallyInCenter(JComponent comp, Container parent) {
      final Rectangle compBounds = comp.getBounds();
      int compHeight = comp.getPreferredSize().height;
      final int parentHeight = parent.getHeight();
      if (compHeight > parentHeight) {
        compHeight = parentHeight;
      }

      int y = (int)Math.floor(parentHeight / 2.0 - compHeight / 2.0);
      comp.setBounds(compBounds.x, y, compBounds.width, compHeight);
    }
  }

  @Override
  public IdeFocusManager getFocusManager() {
    return myFocusManager;
  }

  @Override
  public RunnerLayoutUi getRunnerLayoutUi() {
    return myRunnerUi;
  }

  @Override
  public String getName() {
    return mySessionName;
  }

  @Override
  public List<AnAction> getActions(boolean originalProvider) {
    ArrayList<AnAction> result = new ArrayList<>();
    if (myLeftToolbarActions != null) {
      AnAction[] kids = myLeftToolbarActions.getChildren(null);
      ContainerUtil.addAll(result, kids);
    }
//    if (myTopLeftActions != null && UIExperiment.isNewDebuggerUIEnabled()) {
//      AnAction[] kids = myTopLeftActions.getChildren(null);
//      ContainerUtil.addAll(result, kids);
//    }
    return result;
  }

  private int findFreeWindow() {
    int i;
    for (i = 1; i < Integer.MAX_VALUE; i++) {
      if (!isUsed(i)) {
        return i;
      }
    }
    return i;
  }

  private boolean isUsed(int i) {
    return myChildren.stream().anyMatch(child -> child.getWindow() == i);
  }

  private BaseDockManager getDockManager() {
    return (BaseDockManager)DockManager.getInstance(myProject);
  }

  class MyDragOutDelegate implements TabInfo.DragOutDelegate {
    private DragSession mySession;

    @Override
    public void dragOutStarted(MouseEvent mouseEvent, TabInfo info) {
      JComponent component = info.getComponent();
      Content[] data = (Content[])((DataProvider)component).getData(ViewContext.CONTENT_KEY);
      assert data != null;
      storeDefaultIndices(data);

      final Dimension size = info.getComponent().getSize();
      final Image image = JBTabsImpl.getComponentImage(info);
      if (component instanceof Grid) {
        info.setHidden(true);
      }

      Presentation presentation = new Presentation(info.getText());
      presentation.setIcon(info.getIcon());
      mySession = getDockManager().createDragSession(mouseEvent, new DockableGrid(image, presentation, size, Arrays.asList(data), 0));
    }

    @Override
    public void processDragOut(MouseEvent event, TabInfo source) {
      mySession.process(event);
    }

    @Override
    public void dragOutFinished(MouseEvent event, TabInfo source) {
      final Component component = event.getComponent();
      final IdeFrame window = UIUtil.getParentOfType(IdeFrame.class, component);
      mySession.process(event);
      mySession = null;
    }

    @Override
    public void dragOutCancelled(TabInfo source) {
      source.setHidden(false);
      mySession.cancel();
      mySession = null;
    }
  }

  class DockableGrid implements DockableContent<List<Content>> {
    private final Image myImg;
    private final Presentation myPresentation;
    private final Dimension myPreferredSize;
    private final List<Content> myContents;
    private final int myWindow;

    public DockableGrid(Image img, Presentation presentation, final Dimension size, List<Content> contents, int window) {
      myImg = img;
      myPresentation = presentation;
      myPreferredSize = size;
      myContents = contents;
      myWindow = window;
    }

    @Nonnull
    @Override
    public List<Content> getKey() {
      return myContents;
    }

    @Override
    public Image getPreviewImage() {
      return myImg;
    }

    @Override
    public Dimension getPreferredSize() {
      return myPreferredSize;
    }

    @Override
    public String getDockContainerType() {
      return DockableGridContainerFactory.TYPE;
    }

    @Override
    public Presentation getPresentation() {
      return myPresentation;
    }

    public RunnerContentUiImpl getRunnerUi() {
      return RunnerContentUiImpl.this;
    }

    public RunnerContentUiImpl getOriginalRunnerUi() {
      return myOriginal != null ? myOriginal : RunnerContentUiImpl.this;
    }

    @Nonnull
    public List<Content> getContents() {
      return myContents;
    }

    @Override
    public void close() {
    }

    public int getWindow() {
      return myWindow;
    }
  }

  void fireContentOpened(Content content) {
    for (Listener each : myDockingListeners) {
      each.contentAdded(content);
    }
  }

  void fireContentClosed(Content content) {
    for (Listener each : myDockingListeners) {
      each.contentRemoved(content);
    }
  }
}
