// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.navigationToolbar;

import consulo.application.ApplicationManager;
import consulo.application.ui.UISettings;
import consulo.application.util.Queryable;
import consulo.application.util.SystemInfo;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.IdeView;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.language.editor.refactoring.ui.CopyPasteDelegator;
import consulo.ui.ex.CopyPasteSupport;
import consulo.ide.impl.idea.ide.dnd.TransferableWrapper;
import consulo.ide.impl.idea.ide.navigationToolbar.ui.NavBarUI;
import consulo.ide.impl.idea.ide.navigationToolbar.ui.NavBarUIManager;
import consulo.ide.impl.idea.ide.ui.customization.CustomActionsSchemaImpl;
import consulo.ide.impl.idea.ide.util.DeleteHandler;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.actions.ModuleDeleteProvider;
import consulo.ide.impl.idea.openapi.util.Getter;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.impl.idea.ui.LightweightHint;
import consulo.ide.impl.idea.ui.ListenerUtil;
import consulo.ide.impl.idea.ui.PopupMenuListenerAdapter;
import consulo.ide.impl.idea.ui.popup.AbstractPopup;
import consulo.ide.impl.idea.ui.popup.PopupOwner;
import java.util.function.Consumer;
import consulo.util.lang.ObjectUtil;
import consulo.ide.navigationToolbar.NavBarModelExtension;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.editor.hint.HintManager;
import consulo.language.psi.PsiDirectory;
import consulo.language.psi.PsiDirectoryContainer;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiUtilCore;
import consulo.language.util.ModuleUtilCore;
import consulo.module.Module;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.project.ui.view.ProjectView;
import consulo.project.ui.view.ProjectViewPane;
import consulo.language.content.ProjectRootsUtil;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.project.ui.wm.WindowManager;
import consulo.ui.ex.Gray;
import consulo.ui.ex.RelativePoint;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.accessibility.AccessibleContextUtil;
import consulo.ui.ex.awt.accessibility.ScreenReader;
import consulo.ui.ex.awt.dnd.DnDDragStartBean;
import consulo.ui.ex.awt.dnd.DnDSupport;
import consulo.ui.ex.awt.util.ComponentUtil;
import consulo.ui.ex.awt.util.ScreenUtil;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.util.TextAttributesUtil;
import consulo.util.collection.JBIterable;
import consulo.util.concurrent.ActionCallback;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VFileProperty;
import consulo.virtualFileSystem.VirtualFile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.plaf.PanelUI;
import javax.swing.tree.TreeNode;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.List;
import java.util.*;
import java.util.function.Function;

/**
 * @author Konstantin Bulenkov
 * @author Anna Kozlova
 */
public class NavBarPanel extends JPanel implements DataProvider, PopupOwner, Disposable, Queryable {

  private final NavBarModel myModel;

  private final NavBarPresentation myPresentation;
  protected final Project myProject;

  private final ArrayList<NavBarItem> myList = new ArrayList<>();

  private final ModuleDeleteProvider myDeleteModuleProvider = new ModuleDeleteProvider();
  private final IdeView myIdeView;
  private FocusListener myNavBarItemFocusListener;

  private LightweightHint myHint = null;
  private NavBarPopup myNodePopup = null;
  private JComponent myHintContainer;
  private Component myContextComponent;

  private final NavBarUpdateQueue myUpdateQueue;

  private NavBarItem myContextObject;
  private boolean myDisposed = false;
  private RelativePoint myLocationCache;

  public NavBarPanel(@Nonnull Project project, boolean docked) {
    super(new FlowLayout(FlowLayout.LEFT, 0, 0));
    myProject = project;
    myModel = createModel();
    myIdeView = new NavBarIdeView(this);
    myPresentation = new NavBarPresentation(myProject);
    myUpdateQueue = new NavBarUpdateQueue(this);

    installPopupHandler(this, -1);
    setOpaque(false);
    if (!docked && UIUtil.isUnderDarcula()) {
      setBorder(new LineBorder(Gray._120, 1));
    }
    myUpdateQueue.queueModelUpdateFromFocus();
    myUpdateQueue.queueRebuildUi();
    if (!docked) {
      final ActionCallback typeAheadDone = new ActionCallback();
      ProjectIdeFocusManager.getInstance(project).typeAheadUntil(typeAheadDone, "NavBarPanel");
      myUpdateQueue.queueTypeAheadDone(typeAheadDone);
    }

    Disposer.register(project, this);
    AccessibleContextUtil.setName(this, "Navigation Bar");
  }

  /**
   * Navigation bar entry point to determine if the keyboard/focus behavior should be
   * compatible with screen readers. This additional level of indirection makes it
   * easier to figure out the various locations in the various navigation bar components
   * that enable screen reader friendly behavior.
   */
  protected boolean allowNavItemsFocus() {
    return ScreenReader.isActive();
  }

  public boolean isFocused() {
    if (allowNavItemsFocus()) {
      return UIUtil.isFocusAncestor(this);
    }
    else {
      return hasFocus();
    }
  }

  public void addNavBarItemFocusListener(@Nullable FocusListener l) {
    if (l == null) {
      return;
    }
    myNavBarItemFocusListener = AWTEventMulticaster.add(myNavBarItemFocusListener, l);
  }

  public void removeNavBarItemFocusListener(@Nullable FocusListener l) {
    if (l == null) {
      return;
    }
    myNavBarItemFocusListener = AWTEventMulticaster.remove(myNavBarItemFocusListener, l);
  }

  protected void fireNavBarItemFocusGained(final FocusEvent e) {
    FocusListener listener = myNavBarItemFocusListener;
    if (listener != null) {
      listener.focusGained(e);
    }
  }

  protected void fireNavBarItemFocusLost(final FocusEvent e) {
    FocusListener listener = myNavBarItemFocusListener;
    if (listener != null) {
      listener.focusLost(e);
    }
  }

  protected NavBarModel createModel() {
    return new NavBarModel(myProject);
  }

  @Nullable
  public NavBarPopup getNodePopup() {
    return myNodePopup;
  }

  public boolean isNodePopupActive() {
    return myNodePopup != null && myNodePopup.isVisible();
  }

  public LightweightHint getHint() {
    return myHint;
  }

  public NavBarPresentation getPresentation() {
    return myPresentation;
  }

  public void setContextComponent(@Nullable Component contextComponent) {
    myContextComponent = contextComponent;
  }

  public NavBarItem getContextObject() {
    return myContextObject;
  }

  public List<NavBarItem> getItems() {
    return Collections.unmodifiableList(myList);
  }

  public void addItem(NavBarItem item) {
    myList.add(item);
  }

  public void clearItems() {
    final NavBarItem[] toDispose = myList.toArray(new NavBarItem[0]);
    myList.clear();
    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      for (NavBarItem item : toDispose) {
        Disposer.dispose(item);
      }
    });

    getNavBarUI().clearItems();
  }

  @Override
  public void setUI(PanelUI ui) {
    getNavBarUI().clearItems();
    super.setUI(ui);
  }

  public NavBarUpdateQueue getUpdateQueue() {
    return myUpdateQueue;
  }

  public void escape() {
    myModel.setSelectedIndex(-1);
    hideHint();
    ToolWindowManager.getInstance(myProject).activateEditorComponent();
  }

  public void enter() {
    int index = myModel.getSelectedIndex();
    if (index != -1) ctrlClick(index);
  }

  public void moveHome() {
    shiftFocus(-myModel.getSelectedIndex());
  }

  public void navigate() {
    if (myModel.getSelectedIndex() != -1) {
      doubleClick(myModel.getSelectedIndex());
    }
  }

  public void moveDown() {
    final int index = myModel.getSelectedIndex();
    if (index != -1) {
      if (myModel.size() - 1 == index) {
        shiftFocus(-1);
        ctrlClick(index - 1);
      }
      else {
        ctrlClick(index);
      }
    }
  }

  public void moveEnd() {
    shiftFocus(myModel.size() - 1 - myModel.getSelectedIndex());
  }

  public Project getProject() {
    return myProject;
  }

  public NavBarModel getModel() {
    return myModel;
  }

  @Override
  public void dispose() {
    cancelPopup();
    getNavBarUI().clearItems();
    myDisposed = true;
    NavBarListener.unsubscribeFrom(this);
  }

  public boolean isDisposed() {
    return myDisposed;
  }

  boolean isSelectedInPopup(Object object) {
    return isNodePopupActive() && myNodePopup.getList().getSelectedValuesList().contains(object);
  }

  static Object expandDirsWithJustOneSubdir(Object target) {
    if (target instanceof PsiElement && !((PsiElement)target).isValid()) return target;
    if (target instanceof PsiDirectory) {
      PsiDirectory directory = (PsiDirectory)target;
      for (VirtualFile file = directory.getVirtualFile(), next; ; file = next) {
        VirtualFile[] children = file.getChildren();
        VirtualFile child = children.length == 1 ? children[0] : null;
        //noinspection AssignmentToForLoopParameter
        next = child != null && child.isDirectory() && !child.is(VFileProperty.SYMLINK) ? child : null;
        if (next == null) return ObjectUtil.notNull(directory.getManager().findDirectory(file), directory);
      }
    }
    return target;
  }

  protected void updateItems() {
    for (NavBarItem item : myList) {
      item.update();
    }
    if (UISettings.getInstance().getShowNavigationBar()) {
      NavBarRootPaneExtension.NavBarWrapperPanel wrapperPanel =
              ComponentUtil.getParentOfType((Class<? extends NavBarRootPaneExtension.NavBarWrapperPanel>)NavBarRootPaneExtension.NavBarWrapperPanel.class, (Component)this);

      if (wrapperPanel != null) {
        wrapperPanel.revalidate();
        wrapperPanel.repaint();
      }
    }
  }

  public void rebuildAndSelectItem(final Function<List<NavBarItem>, Integer> indexToSelectCallback, boolean showPopup) {
    myUpdateQueue.queueModelUpdateFromFocus();
    myUpdateQueue.queueRebuildUi();
    myUpdateQueue.queueSelect(() -> {
      if (!myList.isEmpty()) {
        int index = indexToSelectCallback.apply(myList);
        myModel.setSelectedIndex(index);
        requestSelectedItemFocus();
        if (showPopup) {
          ctrlClick(index);
        }

      }
    });

    myUpdateQueue.flush();
  }

  public void rebuildAndSelectTail(final boolean requestFocus) {
    rebuildAndSelectItem((list) -> list.size() - 1, false);
  }

  public void requestSelectedItemFocus() {
    int index = myModel.getSelectedIndex();
    if (index >= 0 && index < myModel.size() && allowNavItemsFocus()) {
      ProjectIdeFocusManager.getInstance(myProject).requestFocus(getItem(index), true);
    }
    else {
      ProjectIdeFocusManager.getInstance(myProject).requestFocus(this, true);
    }
  }

  public void moveLeft() {
    shiftFocus(-1);
  }

  public void moveRight() {
    shiftFocus(1);
  }

  void shiftFocus(int direction) {
    final int selectedIndex = myModel.getSelectedIndex();
    final int index = myModel.getIndexByModel(selectedIndex + direction);
    myModel.setSelectedIndex(index);
    if (allowNavItemsFocus()) {
      requestSelectedItemFocus();
    }
  }

  protected void scrollSelectionToVisible() {
    final int selectedIndex = myModel.getSelectedIndex();
    if (selectedIndex == -1 || selectedIndex >= myList.size()) return;
    scrollRectToVisible(myList.get(selectedIndex).getBounds());
  }

  @Nullable
  private NavBarItem getItem(int index) {
    if (index != -1 && index < myList.size()) {
      return myList.get(index);
    }
    return null;
  }

  public boolean isInFloatingMode() {
    return myHint != null && myHint.isVisible();
  }


  @Override
  public Dimension getPreferredSize() {
    if (myDisposed || !myList.isEmpty()) {
      return super.getPreferredSize();
    }
    else {
      final NavBarItem item = new NavBarItem(this, null, 0, null);
      final Dimension size = item.getPreferredSize();
      ApplicationManager.getApplication().executeOnPooledThread(() -> Disposer.dispose(item));
      return size;
    }
  }

  public boolean isRebuildUiNeeded() {
    myModel.revalidate();
    if (myList.size() == myModel.size()) {
      int index = 0;
      for (NavBarItem eachLabel : myList) {
        Object eachElement = myModel.get(index);
        if (eachLabel.getObject() == null || !eachLabel.getObject().equals(eachElement)) {
          return true;
        }

        if (!StringUtil.equals(eachLabel.getText(), getPresentation().getPresentableText(eachElement, false))) {
          return true;
        }

        SimpleTextAttributes modelAttributes1 = myPresentation.getTextAttributes(eachElement, true);
        SimpleTextAttributes modelAttributes2 = myPresentation.getTextAttributes(eachElement, false);
        SimpleTextAttributes labelAttributes = eachLabel.getAttributes();

        if (!TextAttributesUtil.toTextAttributes(modelAttributes1).equals(TextAttributesUtil.toTextAttributes(labelAttributes)) && !TextAttributesUtil.toTextAttributes(modelAttributes2)
                .equals(TextAttributesUtil.toTextAttributes(labelAttributes))) {
          return true;
        }
        index++;
      }
      return false;
    }
    else {
      return true;
    }
  }

  void installPopupHandler(@Nonnull JComponent component, int index) {
    ActionManager actionManager = ActionManager.getInstance();
    PopupHandler.installPopupHandler(component, new ActionGroup() {
      @Override
      public AnAction[] getChildren(@Nullable AnActionEvent e) {
        if (e == null) return EMPTY_ARRAY;
        String popupGroupId = null;
        for (NavBarModelExtension modelExtension : NavBarModelExtension.EP_NAME.getExtensionList()) {
          popupGroupId = modelExtension.getPopupMenuGroup(NavBarPanel.this);
          if (popupGroupId != null) break;
        }
        if (popupGroupId == null) popupGroupId = IdeActions.GROUP_NAVBAR_POPUP;
        ActionGroup group = (ActionGroup)CustomActionsSchemaImpl.getInstance().getCorrectedAction(popupGroupId);
        return group == null ? EMPTY_ARRAY : group.getChildren(e);
      }
    }, ActionPlaces.NAVIGATION_BAR_POPUP, actionManager, new PopupMenuListenerAdapter() {
      @Override
      public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
        if (index != -1) {
          myModel.setSelectedIndex(index);
        }
      }
    });
  }

  public void installActions(int index, NavBarItem component) {
    //suppress it for a while
    //installDnD(index, component);
    installPopupHandler(component, index);
    ListenerUtil.addMouseListener(component, new MouseAdapter() {
      @Override
      public void mouseReleased(final MouseEvent e) {
        if (SystemInfo.isWindows) {
          click(e);
        }
      }

      @Override
      public void mousePressed(final MouseEvent e) {
        if (!SystemInfo.isWindows) {
          click(e);
        }
      }

      private void click(final MouseEvent e) {
        if (e.isConsumed()) return;

        if (e.isPopupTrigger()) return;
        if (e.getClickCount() == 1) {
          ctrlClick(index);
          e.consume();
        }
        else if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1) {
          requestSelectedItemFocus();
          doubleClick(index);
          e.consume();
        }
      }
    });

    ListenerUtil.addKeyListener(component, new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (e.getModifiers() == 0 && e.getKeyCode() == KeyEvent.VK_SPACE) {
          ctrlClick(index);
          myModel.setSelectedIndex(index);
          e.consume();
        }
      }
    });
  }

  private void installDnD(final int index, NavBarItem component) {
    DnDSupport.createBuilder(component).setBeanProvider(dnDActionInfo -> new DnDDragStartBean(new TransferableWrapper() {
      @Override
      public List<File> asFileList() {
        Object o = myModel.get(index);
        if (o instanceof PsiElement) {
          VirtualFile vf = o instanceof PsiDirectory ? ((PsiDirectory)o).getVirtualFile() : ((PsiElement)o).getContainingFile().getVirtualFile();
          if (vf != null) {
            return Collections.singletonList(new File(vf.getPath()).getAbsoluteFile());
          }
        }
        return Collections.emptyList();
      }

      @Override
      public TreeNode[] getTreeNodes() {
        return null;
      }

      @Override
      public PsiElement[] getPsiElements() {
        return null;
      }
    })).setDisposableParent(component).install();
  }

  private void doubleClick(final int index) {
    doubleClick(myModel.getElement(index));
  }

  protected void doubleClick(final Object object) {
    if (object instanceof Navigatable) {
      Navigatable navigatable = (Navigatable)object;
      if (navigatable.canNavigate()) {
        navigatable.navigate(true);
      }
    }
    else if (object instanceof Module) {
      ProjectView projectView = ProjectView.getInstance(myProject);
      ProjectViewPane projectViewPane = projectView.getProjectViewPaneById(projectView.getCurrentViewId());
      if (projectViewPane != null) {
        projectViewPane.selectModule((Module)object, true);
      }
    }
    else if (object instanceof Project) {
      return;
    }
    hideHint(true);
  }

  private void ctrlClick(final int index) {
    if (isNodePopupActive()) {
      cancelPopup();
      if (myModel.getSelectedIndex() == index) {
        return;
      }
    }

    final Object object = myModel.getElement(index);
    final List<Object> objects = myModel.getChildren(object);

    if (!objects.isEmpty()) {
      final Object[] siblings = new Object[objects.size()];
      //final Icon[] icons = new Icon[objects.size()];
      for (int i = 0; i < objects.size(); i++) {
        siblings[i] = objects.get(i);
        //icons[i] = NavBarPresentation.getIcon(siblings[i], false);
      }
      final NavBarItem item = getItem(index);

      final int selectedIndex = index < myModel.size() - 1 ? objects.indexOf(myModel.getElement(index + 1)) : 0;
      myNodePopup = new NavBarPopup(this, index, siblings, selectedIndex);
      // if (item != null && item.isShowing()) {
      myNodePopup.show(item);
      item.update();
      // }
    }
  }

  protected void navigateInsideBar(int sourceItemIndex, final Object object) {
    //UIEventLogger.logUIEvent(UIEventId.NavBarNavigate);

    boolean restorePopup = shouldRestorePopupOnSelect(object, sourceItemIndex);
    Object obj = expandDirsWithJustOneSubdir(object);
    myContextObject = null;

    myUpdateQueue.cancelAllUpdates();
    if (myNodePopup != null && myNodePopup.isVisible()) {
      myUpdateQueue.queueModelUpdateForObject(obj);
    }
    myUpdateQueue.queueRebuildUi();

    myUpdateQueue.queueAfterAll(() -> {
      int index = myModel.indexOf(obj);
      if (index >= 0) {
        myModel.setSelectedIndex(index);
      }

      if (myModel.hasChildren(obj) && restorePopup) {
        restorePopup();
      }
      else {
        doubleClick(obj);
      }
    }, NavBarUpdateQueue.ID.NAVIGATE_INSIDE);
  }

  private boolean shouldRestorePopupOnSelect(Object obj, int sourceItemIndex) {
    if (sourceItemIndex < myModel.size() - 1 && myModel.get(sourceItemIndex + 1) == obj) return true;
    if (!(obj instanceof PsiElement)) return true;
    PsiElement psiElement = (PsiElement)obj;
    return psiElement instanceof PsiDirectory || psiElement instanceof PsiDirectoryContainer;
  }

  void restorePopup() {
    cancelPopup();
    ctrlClick(myModel.getSelectedIndex());
  }

  void cancelPopup() {
    cancelPopup(false);
  }


  void cancelPopup(boolean ok) {
    if (myNodePopup != null) {
      myNodePopup.hide(ok);
      myNodePopup = null;
      if (allowNavItemsFocus()) {
        requestSelectedItemFocus();
      }
    }
  }

  void hideHint() {
    hideHint(false);
  }

  protected void hideHint(boolean ok) {
    cancelPopup(ok);
    if (myHint != null) {
      myHint.hide(ok);
      myHint = null;
    }
  }

  @Override
  @Nullable
  public Object getData(@Nonnull Key<?> dataId) {
    for (NavBarModelExtension modelExtension : NavBarModelExtension.EP_NAME.getExtensionList()) {
      Object data = modelExtension.getData(dataId, this::getDataInner);
      if (data != null) return data;
    }
    return getDataInner(dataId);
  }

  @Nullable
  private Object getDataInner(Key<?> dataId) {
    return getDataImpl(dataId, this, () -> getSelection());
  }

  @Nonnull
  JBIterable<?> getSelection() {
    Object value = myModel.getSelectedValue();
    if (value != null) return JBIterable.of(value);
    int size = myModel.size();
    return JBIterable.of(size > 0 ? myModel.getElement(size - 1) : null);
  }

  Object getDataImpl(Key<?> dataId, @Nonnull JComponent source, @Nonnull Getter<? extends JBIterable<?>> selection) {
    if (CommonDataKeys.PROJECT == dataId) {
      return !myProject.isDisposed() ? myProject : null;
    }
    if (LangDataKeys.MODULE == dataId) {
      Module module = selection.get().filter(Module.class).first();
      if (module != null && !module.isDisposed()) return module;
      PsiElement element = selection.get().filter(PsiElement.class).first();
      if (element != null) {
        return ModuleUtilCore.findModuleForPsiElement(element);
      }
      return null;
    }
    if (LangDataKeys.MODULE_CONTEXT == dataId) {
      PsiDirectory directory = selection.get().filter(PsiDirectory.class).first();
      if (directory != null) {
        VirtualFile dir = directory.getVirtualFile();
        if (ProjectRootsUtil.isModuleContentRoot(dir, myProject)) {
          return ModuleUtilCore.findModuleForPsiElement(directory);
        }
      }
      return null;
    }
    if (CommonDataKeys.PSI_ELEMENT == dataId) {
      PsiElement element = selection.get().filter(PsiElement.class).first();
      return element != null && element.isValid() ? element : null;
    }
    if (LangDataKeys.PSI_ELEMENT_ARRAY == dataId) {
      List<PsiElement> result = selection.get().filter(PsiElement.class).filter(e -> e != null && e.isValid()).toList();
      return result.isEmpty() ? null : result.toArray(PsiElement.EMPTY_ARRAY);
    }

    if (CommonDataKeys.VIRTUAL_FILE_ARRAY == dataId) {
      Set<VirtualFile> files = selection.get().filter(PsiElement.class).filter(e -> e != null && e.isValid()).filterMap(e -> PsiUtilCore.getVirtualFile(e)).toSet();
      return !files.isEmpty() ? VfsUtilCore.toVirtualFileArray(files) : null;
    }

    if (CommonDataKeys.NAVIGATABLE_ARRAY == dataId) {
      List<Navigatable> elements = selection.get().filter(Navigatable.class).toList();
      return elements.isEmpty() ? null : elements.toArray(new Navigatable[0]);
    }

    if (UIExAWTDataKey.CONTEXT_COMPONENT == dataId) {
      return this;
    }
    if (PlatformDataKeys.CUT_PROVIDER == dataId) {
      return getCopyPasteDelegator(source).getCutProvider();
    }
    if (PlatformDataKeys.COPY_PROVIDER == dataId) {
      return getCopyPasteDelegator(source).getCopyProvider();
    }
    if (PlatformDataKeys.PASTE_PROVIDER == dataId) {
      return getCopyPasteDelegator(source).getPasteProvider();
    }
    if (PlatformDataKeys.DELETE_ELEMENT_PROVIDER == dataId) {
      return selection.get().filter(Module.class).isNotEmpty() ? myDeleteModuleProvider : new DeleteHandler.DefaultDeleteProvider();
    }

    if (IdeView.KEY== dataId) {
      return myIdeView;
    }

    return null;
  }

  @Nonnull
  private CopyPasteSupport getCopyPasteDelegator(@Nonnull JComponent source) {
    String key = "NavBarPanel.copyPasteDelegator";
    Object result = source.getClientProperty(key);
    if (!(result instanceof CopyPasteSupport)) {
      source.putClientProperty(key, result = new CopyPasteDelegator(myProject, source));
    }
    return (CopyPasteSupport)result;
  }

  @Override
  public Point getBestPopupPosition() {
    int index = myModel.getSelectedIndex();
    final int modelSize = myModel.size();
    if (index == -1) {
      index = modelSize - 1;
    }
    if (index > -1 && index < modelSize) {
      final NavBarItem item = getItem(index);
      if (item != null) {
        return new Point(item.getX(), item.getY() + item.getHeight());
      }
    }
    return null;
  }

  @Override
  public void addNotify() {
    super.addNotify();
    NavBarListener.subscribeTo(this);
  }

  @Override
  public void removeNotify() {
    super.removeNotify();
    if (isDisposeOnRemove() && ScreenUtil.isStandardAddRemoveNotify(this)) {
      Disposer.dispose(this);
    }
  }

  protected boolean isDisposeOnRemove() {
    return true;
  }

  public void updateState(final boolean show) {
    if (show) {
      myUpdateQueue.queueModelUpdateFromFocus();
      myUpdateQueue.queueRebuildUi();
    }
  }

  // ------ popup NavBar ----------
  public void showHint(@Nullable final Editor editor, final DataContext dataContext) {
    myModel.updateModel(dataContext);
    if (myModel.isEmpty()) return;
    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(this);
    panel.setOpaque(true);
    panel.setBackground(UIUtil.getListBackground());

    myHint = new LightweightHint(panel) {
      @Override
      public void hide() {
        super.hide();
        cancelPopup();
        Disposer.dispose(NavBarPanel.this);
      }
    };
    myHint.setForceShowAsPopup(true);
    myHint.setFocusRequestor(this);
    final KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    myUpdateQueue.rebuildUi();
    if (editor == null) {
      myContextComponent = dataContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
      getHintContainerShowPoint().doWhenDone((Consumer<RelativePoint>)relativePoint -> {
        final Component owner = focusManager.getFocusOwner();
        final Component cmp = relativePoint.getComponent();
        if (cmp instanceof JComponent && cmp.isShowing()) {
          myHint.show((JComponent)cmp, relativePoint.getPoint().x, relativePoint.getPoint().y, owner instanceof JComponent ? (JComponent)owner : null,
                      new HintHint(relativePoint.getComponent(), relativePoint.getPoint()));
        }
      });
    }
    else {
      myHintContainer = editor.getContentComponent();
      getHintContainerShowPoint().doWhenDone(rp -> {
        Point p = rp.getPointOn(myHintContainer).getPoint();
        final HintHint hintInfo = new HintHint(editor.getContentComponent(), p);
        HintManagerImpl.getInstanceImpl().showEditorHint(myHint, editor, p, HintManager.HIDE_BY_ESCAPE, 0, true, hintInfo);
      });
    }

    rebuildAndSelectTail(true);
  }

  AsyncResult<RelativePoint> getHintContainerShowPoint() {
    AsyncResult<RelativePoint> result = new AsyncResult<>();
    if (myLocationCache == null) {
      if (myHintContainer != null) {
        final Point p = AbstractPopup.getCenterOf(myHintContainer, this);
        p.y -= myHintContainer.getVisibleRect().height / 4;
        myLocationCache = RelativePoint.fromScreen(p);
      }
      else {
        DataManager dataManager = DataManager.getInstance();
        if (myContextComponent != null) {
          DataContext ctx = dataManager.getDataContext(myContextComponent);
          myLocationCache = JBPopupFactory.getInstance().guessBestPopupLocation(ctx);
        }
        else {
          dataManager.getDataContextFromFocus().doWhenDone((Consumer<DataContext>)dataContext -> {
            myContextComponent = dataContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT);
            DataContext ctx = dataManager.getDataContext(myContextComponent);
            myLocationCache = JBPopupFactory.getInstance().guessBestPopupLocation(ctx);
          });
        }
      }
    }
    final Component c = myLocationCache.getComponent();
    if (!(c instanceof JComponent && c.isShowing())) {
      //Yes. It happens sometimes.
      // 1. Empty frame. call nav bar, select some package and open it in Project View
      // 2. Call nav bar, then Esc
      // 3. Hide all tool windows (Ctrl+Shift+F12), so we've got empty frame again
      // 4. Call nav bar. NPE. ta da
      final JComponent ideFrame = WindowManager.getInstance().getIdeFrame(getProject()).getComponent();
      final JRootPane rootPane = UIUtil.getRootPane(ideFrame);
      myLocationCache = JBPopupFactory.getInstance().guessBestPopupLocation(rootPane);
    }
    result.setDone(myLocationCache);
    return result;
  }

  @Override
  public void putInfo(@Nonnull Map<String, String> info) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < myList.size(); i++) {
      NavBarItem each = myList.get(i);
      if (each.isSelected()) {
        result.append("[").append(each.getText()).append("]");
      }
      else {
        result.append(each.getText());
      }
      if (i < myList.size() - 1) {
        result.append(">");
      }
    }
    info.put("navBar", result.toString());

    if (isNodePopupActive()) {
      StringBuilder popupText = new StringBuilder();
      JBList list = myNodePopup.getList();
      for (int i = 0; i < list.getModel().getSize(); i++) {
        Object eachElement = list.getModel().getElementAt(i);
        String text = new NavBarItem(this, eachElement, myNodePopup, true).getText();
        int selectedIndex = list.getSelectedIndex();
        if (selectedIndex != -1 && eachElement.equals(list.getSelectedValue())) {
          popupText.append("[").append(text).append("]");
        }
        else {
          popupText.append(text);
        }
        if (i < list.getModel().getSize() - 1) {
          popupText.append(">");
        }
      }
      info.put("navBarPopup", popupText.toString());
    }
  }

  @Nonnull
  public NavBarUI getNavBarUI() {
    return NavBarUIManager.getUI();
  }

  boolean isUpdating() {
    return myUpdateQueue.isUpdating();
  }
}
