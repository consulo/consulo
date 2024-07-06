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
package consulo.desktop.awt.fileEditor.impl;

import consulo.application.HelpManager;
import consulo.application.ui.UISettings;
import consulo.application.ui.event.UISettingsListener;
import consulo.application.util.Queryable;
import consulo.colorScheme.EditorColorsManager;
import consulo.dataContext.DataProvider;
import consulo.desktop.awt.ui.IdeEventQueue;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileEditor.*;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.ide.impl.idea.ide.GeneralSettings;
import consulo.ide.impl.idea.ide.actions.CloseAction;
import consulo.ide.impl.idea.ide.actions.ShowFilePathAction;
import consulo.ide.impl.idea.ide.ui.customization.CustomActionsSchemaImpl;
import consulo.fileEditor.history.IdeDocumentHistory;
import consulo.ide.impl.idea.openapi.fileEditor.impl.DockableEditorContainerFactory;
import consulo.ide.impl.idea.openapi.fileEditor.impl.FileEditorManagerImpl;
import consulo.ide.impl.idea.openapi.fileEditor.impl.IdeDocumentHistoryImpl;
import consulo.ide.impl.idea.openapi.fileEditor.impl.tabActions.CloseTab;
import consulo.ide.impl.idea.openapi.fileEditor.impl.text.FileDropHandler;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.ide.impl.idea.openapi.wm.ex.ToolWindowManagerEx;
import consulo.ide.impl.idea.ui.InplaceButton;
import consulo.ide.impl.idea.ui.tabs.TabsUtil;
import consulo.ide.impl.idea.ui.tabs.impl.JBEditorTabs;
import consulo.ide.impl.idea.ui.tabs.impl.JBTabsImpl;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.wm.ToolWindowManagerListener;
import consulo.project.ui.wm.dock.DockContainer;
import consulo.project.ui.wm.dock.DockManager;
import consulo.project.ui.wm.dock.DockableContent;
import consulo.project.ui.wm.dock.DragSession;
import consulo.ui.ex.JBColor;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.CustomLineBorder;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.tab.*;
import consulo.ui.ex.awt.util.TimedDeadzone;
import consulo.ui.ex.awtUnsafe.TargetAWT;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.ex.toolWindow.ToolWindowAnchor;
import consulo.ui.ex.toolWindow.ToolWindowType;
import consulo.undoRedo.CommandProcessor;
import consulo.util.concurrent.ActionCallback;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.Map;

/**
 * @author Anton Katilin
 * @author Vladimir Kondratyev
 */
public final class DesktopAWTEditorTabbedContainer implements FileEditorTabbedContainer, Disposable, CloseAction.CloseTarget {
  private final DesktopFileEditorWindow myWindow;
  private final Project myProject;
  private final JBEditorTabs myTabs;

  public static final String HELP_ID = "ideaInterface.editor";

  private final TabInfo.DragOutDelegate myDragOutDelegate = new MyDragOutDelegate();

  DesktopAWTEditorTabbedContainer(final DesktopFileEditorWindow window, Project project) {
    myWindow = window;
    myProject = project;
    final ActionManager actionManager = ActionManager.getInstance();
    myTabs = new JBEditorTabs(project, actionManager, ProjectIdeFocusManager.getInstance(project), this);
    myTabs.setBorder(new CustomLineBorder(JBColor.border(), 1, 0, 0, 0) {
      @Override
      public Insets getBorderInsets(Component c) {
        if (myTabs.getPresentation().isHideTabs()) {
          return super.getBorderInsets(c);
        }
        return JBUI.emptyInsets();
      }
    });
    myTabs.setTransferHandler(new MyTransferHandler());
    myTabs.setDataProvider(new MyDataProvider())
          .setPopupGroup(() -> (ActionGroup)CustomActionsSchemaImpl.getInstance().getCorrectedAction(IdeActions.GROUP_EDITOR_TAB_POPUP), ActionPlaces.EDITOR_TAB_POPUP, false)
          .addTabMouseListener(new TabMouseListener()).getPresentation().setTabDraggingEnabled(true)
          .setUiDecorator(() -> new UiDecorator.UiDecoration(null, JBUI.insets(TabsUtil.TAB_VERTICAL_PADDING, 8))).setTabLabelActionsMouseDeadzone(TimedDeadzone.NULL)
          .setTabLabelActionsAutoHide(false).setActiveTabFillIn(TargetAWT.to(EditorColorsManager.getInstance().getGlobalScheme().getDefaultBackground())).setPaintFocus(true).getJBTabs()
          .addListener(new TabsListener.Adapter() {
              @Override
              public void selectionChanged(final TabInfo oldSelection, final TabInfo newSelection) {
                final FileEditorManager editorManager = FileEditorManager.getInstance(myProject);
                final FileEditor oldEditor = oldSelection != null ? editorManager.getSelectedEditor((VirtualFile)oldSelection.getObject()) : null;
                if (oldEditor != null) {
                  oldEditor.deselectNotify();
                }

                VirtualFile newFile = (VirtualFile)newSelection.getObject();
                final FileEditor newEditor = editorManager.getSelectedEditor(newFile);
                if (newEditor != null) {
                  newEditor.selectNotify();
                }

                if (GeneralSettings.getInstance().isSyncOnFrameActivation()) {
                  VfsUtil.markDirtyAndRefresh(true, false, false, newFile);
                }
              }
            }).setSelectionChangeHandler((info, requestFocus, doChangeSelection) -> {
      final ActionCallback result = new ActionCallback();
      CommandProcessor.getInstance().executeCommand(myProject, () -> {
        ((IdeDocumentHistoryImpl)IdeDocumentHistory.getInstance(myProject)).onSelectionChanged();
        result.notify(doChangeSelection.run());
      }, "EditorChange", null);
      return result;
    }).getPresentation().setRequestFocusOnLastFocusedComponent(true);
    myTabs.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (myTabs.findInfo(e) != null || isFloating()) return;
        if (!e.isPopupTrigger() && SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2) {
          final ActionManager mgr = ActionManager.getInstance();
          mgr.tryToExecute(mgr.getAction("HideAllWindows"), e, null, ActionPlaces.UNKNOWN, true);
        }
      }
    });

    setTabPlacement(UISettings.getInstance().EDITOR_TAB_PLACEMENT);

    updateTabBorder();

    project.getMessageBus().connect().subscribe(ToolWindowManagerListener.class, new ToolWindowManagerListener() {
      @Override
      public void stateChanged(ToolWindowManager toolWindowManager) {
        updateTabBorder();
      }

      @Override
      public void toolWindowRegistered(@Nonnull final String id) {
        updateTabBorder();
      }
    });
    project.getMessageBus().connect().subscribe(UISettingsListener.class, uiSettings -> updateTabBorder());

    Disposer.register(project, this);
  }

  @Override
  public int getTabCount() {
    return myTabs.getTabCount();
  }

  @Override
  @Nonnull
  public ActionCallback setSelectedIndex(final int indexToSelect) {
    return setSelectedIndex(indexToSelect, true);
  }

  @Override
  @Nonnull
  public ActionCallback setSelectedIndex(final int indexToSelect, boolean focusEditor) {
    if (indexToSelect >= myTabs.getTabCount()) return ActionCallback.REJECTED;
    return myTabs.select(myTabs.getTabAt(indexToSelect), focusEditor);
  }

  @Nonnull
  public static DockableEditor createDockableEditor(Project project, Image image, VirtualFile file, Presentation presentation, FileEditorWindow window) {
    return new DockableEditor(project, image, file, presentation, ((DesktopFileEditorWindow)window).getSize(), window.isFilePinned(file));
  }

  private void updateTabBorder() {
    if (!myProject.isOpen()) return;

    ToolWindowManagerEx mgr = (ToolWindowManagerEx)ToolWindowManager.getInstance(myProject);

    String[] ids = mgr.getToolWindowIds();

    Insets border = JBUI.emptyInsets();

    UISettings uiSettings = UISettings.getInstance();

    List<String> topIds = mgr.getIdsOn(ToolWindowAnchor.TOP);
    List<String> bottom = mgr.getIdsOn(ToolWindowAnchor.BOTTOM);
    List<String> rightIds = mgr.getIdsOn(ToolWindowAnchor.RIGHT);
    List<String> leftIds = mgr.getIdsOn(ToolWindowAnchor.LEFT);

    if (!uiSettings.getHideToolStripes() && !uiSettings.getPresentationMode()) {
      border.top = !topIds.isEmpty() ? 1 : 0;
      border.bottom = !bottom.isEmpty() ? 1 : 0;
      border.left = !leftIds.isEmpty() ? 1 : 0;
      border.right = !rightIds.isEmpty() ? 1 : 0;
    }

    for (String each : ids) {
      ToolWindow eachWnd = mgr.getToolWindow(each);
      if (eachWnd == null || !eachWnd.isAvailable()) continue;

      if (eachWnd.isVisible() && eachWnd.getType() == ToolWindowType.DOCKED) {
        ToolWindowAnchor eachAnchor = eachWnd.getAnchor();
        if (eachAnchor == ToolWindowAnchor.TOP) {
          border.top = 0;
        }
        else if (eachAnchor == ToolWindowAnchor.BOTTOM) {
          border.bottom = 0;
        }
        else if (eachAnchor == ToolWindowAnchor.LEFT) {
          border.left = 0;
        }
        else if (eachAnchor == ToolWindowAnchor.RIGHT) {
          border.right = 0;
        }
      }
    }

    myTabs.getPresentation().setPaintBorder(border.top, border.left, border.right, border.bottom).setTabSidePaintBorder(5);
  }

  @Nonnull
  public JComponent getComponent() {
    return myTabs.getComponent();
  }

  @Override
  public ActionCallback removeTabAt(final int componentIndex, int indexToSelect, boolean transferFocus) {
    TabInfo toSelect = indexToSelect >= 0 && indexToSelect < myTabs.getTabCount() ? myTabs.getTabAt(indexToSelect) : null;
    final TabInfo info = myTabs.getTabAt(componentIndex);
    // removing hidden tab happens on end of drag-out, we've already selected the correct tab for this case in dragOutStarted
    if (info.isHidden() || !myProject.isOpen()) {
      toSelect = null;
    }
    final ActionCallback callback = myTabs.removeTab(info, toSelect, transferFocus);
    return myProject.isOpen() ? callback : ActionCallback.DONE;
  }

  @Override
  public int getSelectedIndex() {
    return myTabs.getIndexOf(myTabs.getSelectedInfo());
  }

  void setForegroundAt(final int index, final Color color) {
    myTabs.getTabAt(index).setDefaultForeground(color);
  }

  void setWaveColor(final int index, @Nullable final Color color) {
    final TabInfo tab = myTabs.getTabAt(index);
    tab.setDefaultStyle(color == null ? SimpleTextAttributes.STYLE_PLAIN : SimpleTextAttributes.STYLE_WAVED);
    tab.setDefaultWaveColor(color);
  }

  void setIconAt(final int index, final consulo.ui.image.Image icon) {
    myTabs.getTabAt(index).setIcon(icon);
  }

  public void setTitleAt(final int index, final String text) {
    myTabs.getTabAt(index).setText(text);
  }

  void setToolTipTextAt(final int index, final String text) {
    myTabs.getTabAt(index).setTooltipText(text);
  }

  void setBackgroundColorAt(final int index, final Color color) {
    myTabs.getTabAt(index).setTabColor(color);
  }

  void setTabLayoutPolicy(final int policy) {
    switch (policy) {
      case JTabbedPane.SCROLL_TAB_LAYOUT:
        myTabs.getPresentation().setSingleRow(true);
        break;
      case JTabbedPane.WRAP_TAB_LAYOUT:
        myTabs.getPresentation().setSingleRow(false);
        break;
      default:
        throw new IllegalArgumentException("Unsupported tab layout policy: " + policy);
    }
  }

  public void setTabPlacement(final int tabPlacement) {
    myTabs.getPresentation().setHideTabs(false);
    switch (tabPlacement) {
      case UISettings.PLACEMENT_EDITOR_TAB_TOP:
        myTabs.getPresentation().setTabsPosition(JBTabsPosition.top);
        break;
      case UISettings.PLACEMENT_EDITOR_TAB_BOTTOM:
        myTabs.getPresentation().setTabsPosition(JBTabsPosition.bottom);
        break;
      case UISettings.PLACEMENT_EDITOR_TAB_LEFT:
        myTabs.getPresentation().setTabsPosition(JBTabsPosition.left);
        break;
      case UISettings.PLACEMENT_EDITOR_TAB_RIGHT:
        myTabs.getPresentation().setTabsPosition(JBTabsPosition.right);
        break;
      case UISettings.PLACEMENT_EDITOR_TAB_NONE:
        myTabs.getPresentation().setHideTabs(true);
        break;
      default:
        throw new IllegalArgumentException("Unknown tab placement code=" + tabPlacement);
    }
  }

  @Nullable
  public Object getSelectedComponent() {
    final TabInfo info = myTabs.getTargetInfo();
    return info != null ? info.getComponent() : null;
  }

  public void insertTab(final VirtualFile file, final consulo.ui.image.Image icon, final JComponent comp, final String tooltip, final int indexToInsert, FileEditorWindow window) {
    TabInfo tab = myTabs.findInfo(file);
    if (tab != null) return;

    tab = new TabInfo(comp).setText(EditorTabPresentationUtil.getEditorTabTitle(myProject, file)).setIcon(icon).setTooltipText(tooltip).setObject(file)
                           .setTabColor(EditorTabPresentationUtil.getEditorTabBackgroundColor(myProject, file, window)).setDragOutDelegate(myDragOutDelegate);
    tab.setTestableUi(new MyQueryable(tab));

    final ActionGroup.Builder tabActions = ActionGroup.newImmutableBuilder();
    tabActions.add(new CloseTab(comp, myProject, file, myWindow));

    tab.setTabLabelActions(tabActions.build(), ActionPlaces.EDITOR_TAB);
    myTabs.addTabSilently(tab, indexToInsert);
  }

  boolean isEmptyVisible() {
    return myTabs.isEmptyVisible();
  }

  public JBTabs getTabs() {
    return myTabs;
  }

  public void requestFocus(boolean forced) {
    if (myTabs != null) {
      ProjectIdeFocusManager.getInstance(myProject).requestFocus(myTabs.getComponent(), forced);
    }
  }

  void setPaintBlocked(boolean blocked) {
    myTabs.setPaintBlocked(blocked, true);
  }

  private static class MyQueryable implements Queryable {

    private final TabInfo myTab;

    MyQueryable(TabInfo tab) {
      myTab = tab;
    }

    @Override
    public void putInfo(@Nonnull Map<String, String> info) {
      info.put("editorTab", myTab.getText());
    }
  }

  /**
   * @deprecated Use {@link EditorTabPresentationUtil#getEditorTabTitle(Project, VirtualFile)}
   */
  @Deprecated
  @Nonnull
  public static String calcTabTitle(@Nonnull Project project, @Nonnull VirtualFile file) {
    return EditorTabPresentationUtil.getEditorTabTitle(project, file);
  }

  /**
   * @deprecated Use {@link EditorTabPresentationUtil#getUniqueEditorTabTitle(Project, VirtualFile)}
   */
  @Deprecated
  @Nonnull
  public static String calcFileName(@Nonnull Project project, @Nonnull VirtualFile file) {
    return EditorTabPresentationUtil.getUniqueEditorTabTitle(project, file);
  }

  /**
   * @deprecated Use {@link EditorTabPresentationUtil#getEditorTabBackgroundColor(Project, VirtualFile, FileEditorWindow)}
   */
  @Deprecated
  @Nullable
  public static Color calcTabColor(@Nonnull Project project, @Nonnull VirtualFile file) {
    return EditorTabPresentationUtil.getEditorTabBackgroundColor(project, file, null);
  }

  public Component getComponentAt(final int i) {
    final TabInfo tab = myTabs.getTabAt(i);
    return tab.getComponent();
  }

  @Override
  public void dispose() {

  }

  private class MyDataProvider implements DataProvider {
    @Override
    public Object getData(@Nonnull Key<?> dataId) {
      if (Project.KEY == dataId) {
        return myProject;
      }
      if (VirtualFile.KEY == dataId) {
        final VirtualFile selectedFile = myWindow.getSelectedFile();
        return selectedFile != null && selectedFile.isValid() ? selectedFile : null;
      }
      if (DesktopFileEditorWindow.DATA_KEY == dataId) {
        return myWindow;
      }
      if (HelpManager.HELP_ID == dataId) {
        return HELP_ID;
      }

      if (CloseAction.CloseTarget.KEY == dataId) {
        TabInfo selected = myTabs.getSelectedInfo();
        if (selected != null) {
          return DesktopAWTEditorTabbedContainer.this;
        }
      }

      if (DesktopFileEditorWindow.DATA_KEY == dataId) {
        return myWindow;
      }

      return null;
    }
  }

  @Override
  public void close() {
    TabInfo selected = myTabs.getTargetInfo();
    if (selected == null) return;

    final VirtualFile file = (VirtualFile)selected.getObject();
    final FileEditorManagerEx mgr = FileEditorManagerEx.getInstanceEx(myProject);

    AsyncResult<FileEditorWindow> window = mgr.getActiveWindow();
    window.doWhenDone(wnd -> {
      if (wnd != null) {
        if (wnd.findFileComposite(file) != null) {
          mgr.closeFile(file, wnd);
        }
      }
    });
  }

  private boolean isFloating() {
    return myWindow.getOwner().isFloating();
  }

  private class TabMouseListener extends MouseAdapter {
    private int myActionClickCount;

    @Override
    public void mouseReleased(MouseEvent e) {
      if (UIUtil.isCloseClick(e, MouseEvent.MOUSE_RELEASED)) {
        final TabInfo info = myTabs.findInfo(e);
        if (info != null) {
          IdeEventQueue.getInstance().blockNextEvents(e);
          if (e.isAltDown() && e.getButton() == MouseEvent.BUTTON1) {//close others
            List<TabInfo> allTabInfos = myTabs.getTabs();
            for (TabInfo tabInfo : allTabInfos) {
              if (tabInfo == info) continue;
              FileEditorManagerEx.getInstanceEx(myProject).closeFile((VirtualFile)tabInfo.getObject(), myWindow);
            }
          }
          else {
            FileEditorManagerEx.getInstanceEx(myProject).closeFile((VirtualFile)info.getObject(), myWindow);
          }
        }
      }
    }

    @Override
    public void mousePressed(final MouseEvent e) {
      if (UIUtil.isActionClick(e)) {
        if (e.getClickCount() == 1) {
          myActionClickCount = 0;
        }
        // clicks on the close window button don't count in determining whether we have a double-click on tab (IDEA-70403)
        final Component deepestComponent = SwingUtilities.getDeepestComponentAt(e.getComponent(), e.getX(), e.getY());
        if (!(deepestComponent instanceof InplaceButton)) {
          myActionClickCount++;
        }
        if (myActionClickCount > 1 && !isFloating()) {
          final ActionManager mgr = ActionManager.getInstance();
          mgr.tryToExecute(mgr.getAction("HideAllWindows"), e, null, ActionPlaces.UNKNOWN, true);
        }
      }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if (UIUtil.isActionClick(e, MouseEvent.MOUSE_CLICKED) && (e.isMetaDown() || !Platform.current().os().isMac() && e.isControlDown())) {
        final TabInfo info = myTabs.findInfo(e);
        if (info != null && info.getObject() != null) {
          final VirtualFile vFile = (VirtualFile)info.getObject();
          if (vFile != null) {
            ShowFilePathAction.show(vFile, e);
          }
        }
      }
    }
  }

  class MyDragOutDelegate implements TabInfo.DragOutDelegate {

    private VirtualFile myFile;
    private DragSession mySession;

    @Override
    public void dragOutStarted(MouseEvent mouseEvent, TabInfo info) {
      final TabInfo previousSelection = info.getPreviousSelection();
      final Image img = JBTabsImpl.getComponentImage(info);
      info.setHidden(true);
      if (previousSelection != null) {
        myTabs.select(previousSelection, true);
      }

      myFile = (VirtualFile)info.getObject();
      Presentation presentation = new Presentation(info.getText());
      presentation.setIcon(info.getIcon());
      mySession = getDockManager().createDragSession(mouseEvent, createDockableEditor(myProject, img, myFile, presentation, myWindow));
    }

    private DockManager getDockManager() {
      return DockManager.getInstance(myProject);
    }

    @Override
    public void processDragOut(MouseEvent event, TabInfo source) {
      mySession.process(event);
    }

    @Override
    public void dragOutFinished(MouseEvent event, TabInfo source) {
      boolean copy = UIUtil.isControlKeyDown(event) || mySession.getResponse(event) == DockContainer.ContentResponse.ACCEPT_COPY;
      if (!copy) {
        myFile.putUserData(FileEditorManagerImpl.CLOSING_TO_REOPEN, Boolean.TRUE);
        FileEditorManagerEx.getInstanceEx(myProject).closeFile(myFile, myWindow);
      }
      else {
        source.setHidden(false);
      }

      mySession.process(event);
      if (!copy) {
        myFile.putUserData(FileEditorManagerImpl.CLOSING_TO_REOPEN, null);
      }

      myFile = null;
      mySession = null;
    }

    @Override
    public void dragOutCancelled(TabInfo source) {
      source.setHidden(false);
      if (mySession != null) {
        mySession.cancel();
      }

      myFile = null;
      mySession = null;
    }

  }

  public static class DockableEditor implements DockableContent<VirtualFile> {
    final Image myImg;
    private final DesktopDockableEditorTabbedContainer myContainer;
    private final Presentation myPresentation;
    private final Dimension myPreferredSize;
    private final boolean myPinned;
    private final VirtualFile myFile;

    public DockableEditor(Project project, Image img, VirtualFile file, Presentation presentation, Dimension preferredSize, boolean isFilePinned) {
      myImg = img;
      myFile = file;
      myPresentation = presentation;
      myContainer = new DesktopDockableEditorTabbedContainer(project);
      myPreferredSize = preferredSize;
      myPinned = isFilePinned;
    }

    @Nonnull
    @Override
    public VirtualFile getKey() {
      return myFile;
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
      return DockableEditorContainerFactory.TYPE;
    }

    @Override
    public Presentation getPresentation() {
      return myPresentation;
    }

    @Override
    public void close() {
      myContainer.close(myFile);
    }

    public VirtualFile getFile() {
      return myFile;
    }

    public boolean isPinned() {
      return myPinned;
    }
  }

  private final class MyTransferHandler extends TransferHandler {
    private final FileDropHandler myFileDropHandler = new FileDropHandler(null);

    @Override
    public boolean importData(JComponent comp, Transferable t) {
      if (myFileDropHandler.canHandleDrop(t.getTransferDataFlavors())) {
        myFileDropHandler.handleDrop(t, myProject, myWindow);
        return true;
      }
      return false;
    }

    @Override
    public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
      return myFileDropHandler.canHandleDrop(transferFlavors);
    }
  }
}
