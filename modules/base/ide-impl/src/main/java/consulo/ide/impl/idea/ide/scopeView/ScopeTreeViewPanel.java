/*
 * Copyright 2000-2009 JetBrains s.r.o.
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

package consulo.ide.impl.idea.ide.scopeView;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.codeEditor.CodeInsightColors;
import consulo.codeEditor.Editor;
import consulo.colorScheme.EditorColorsManager;
import consulo.colorScheme.TextAttributes;
import consulo.component.messagebus.MessageBusConnection;
import consulo.content.scope.InvalidPackageSet;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.content.scope.PackageSet;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataManager;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.IdeView;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectViewPaneImpl;
import consulo.ide.impl.idea.ide.projectView.impl.ProjectViewTree;
import consulo.ide.impl.idea.ide.scopeView.nodes.BasePsiNode;
import consulo.ide.impl.idea.ide.ui.customization.CustomizationUtil;
import consulo.ide.impl.idea.ide.util.DeleteHandler;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.actions.ModuleDeleteProvider;
import consulo.ide.impl.idea.packageDependencies.DefaultScopesProvider;
import consulo.ide.impl.idea.packageDependencies.ui.*;
import consulo.ide.localize.IdeLocalize;
import consulo.ide.util.DirectoryChooserUtil;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.packageDependency.DependencyValidationManager;
import consulo.language.editor.refactoring.ui.CopyPasteDelegator;
import consulo.language.editor.util.EditorHelper;
import consulo.language.editor.util.PsiUtilBase;
import consulo.language.editor.wolfAnalyzer.ProblemListener;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.*;
import consulo.language.psi.event.PsiTreeChangeAdapter;
import consulo.language.psi.event.PsiTreeChangeEvent;
import consulo.localHistory.LocalHistory;
import consulo.localHistory.LocalHistoryAction;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.content.layer.event.ModuleRootAdapter;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.project.Project;
import consulo.project.ui.view.ProjectView;
import consulo.project.ui.view.ProjectViewPane;
import consulo.project.ui.view.tree.ModuleGroup;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.*;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.dnd.DnDAwareTree;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.ColoredTreeCellRenderer;
import consulo.ui.ex.awt.tree.TreeState;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.awt.update.UiNotifyConnector;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.ui.ex.awt.util.Update;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.ex.util.TextAttributesUtil;
import consulo.util.concurrent.ActionCallback;
import consulo.util.concurrent.AsyncResult;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.change.*;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatusListener;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.lang.ref.WeakReference;
import java.util.List;
import java.util.*;
import java.util.function.Function;

/**
 * User: anna
 * Date: 25-Jan-2006
 */
public class ScopeTreeViewPanel extends JPanel implements Disposable {
  private static final Logger LOG = Logger.getInstance(ScopeTreeViewPanel.class);
  private final IdeView myIdeView = new MyIdeView();
  private final MyPsiTreeChangeAdapter myPsiTreeChangeAdapter = new MyPsiTreeChangeAdapter();

  private final DnDAwareTree myTree = new DnDAwareTree() {
    @Override
    public boolean isFileColorsEnabled() {
      return ProjectViewTree.isFileColorsEnabledFor(this);
    }

    @Nullable
    @Override
    public Color getFileColorFor(DefaultMutableTreeNode node) {
      if (!(node instanceof PackageDependenciesNode)) {
        return null;
      }
      return ProjectViewTree.getColorForElement(((PackageDependenciesNode)node).getPsiElement());
    }
  };
  private final Project myProject;
  private FileTreeModelBuilder myBuilder;

  private String CURRENT_SCOPE_NAME;

  private TreeExpansionMonitor<PackageDependenciesNode> myTreeExpansionMonitor;
  private CopyPasteDelegator myCopyPasteDelegator;
  private final MyDeletePSIElementProvider myDeletePSIElementProvider = new MyDeletePSIElementProvider();
  private final ModuleDeleteProvider myDeleteModuleProvider = new ModuleDeleteProvider();
  private final DependencyValidationManager myDependencyValidationManager;
  private final ProblemListener myProblemListener = new MyProblemListener();
  private final FileStatusListener myFileStatusListener = new FileStatusListener() {
    @Override
    public void fileStatusesChanged() {
      final List<TreePath> treePaths = TreeUtil.collectExpandedPaths(myTree);
      for (TreePath treePath : treePaths) {
        final Object component = treePath.getLastPathComponent();
        if (component instanceof PackageDependenciesNode) {
          ((PackageDependenciesNode)component).updateColor();
          for (int i = 0; i < ((PackageDependenciesNode)component).getChildCount(); i++) {
            ((PackageDependenciesNode)((PackageDependenciesNode)component).getChildAt(i)).updateColor();
          }
        }
      }
    }

    @Override
    @RequiredReadAction
    public void fileStatusChanged(@Nonnull VirtualFile virtualFile) {
      if (!virtualFile.isValid()) return;
      final PsiFile file = PsiManager.getInstance(myProject).findFile(virtualFile);
      if (file != null) {
        final NamedScope currentScope = getCurrentScope();
        final PackageSet value = currentScope.getValue();
        if (value != null && value.contains(virtualFile, myProject, NamedScopesHolder.getHolder(myProject, currentScope.getName(), myDependencyValidationManager))) {
          if (!myBuilder.hasFileNode(virtualFile)) return;
          final PackageDependenciesNode node = myBuilder.getFileParentNode(virtualFile);
          final PackageDependenciesNode[] nodes = FileTreeModelBuilder.findNodeForPsiElement(node, file);
          if (nodes != null) {
            for (PackageDependenciesNode dependenciesNode : nodes) {
              dependenciesNode.updateColor();
            }
          }
        }
      }
    }
  };

  private final MergingUpdateQueue myUpdateQueue = new MergingUpdateQueue("ScopeViewUpdate", 300, isTreeShowing(), myTree);
  private ScopeTreeViewPanel.MyChangesListListener myChangesListListener = new MyChangesListListener();
  protected AsyncResult<Void> myActionCallback;

  public ScopeTreeViewPanel(final Project project) {
    super(new BorderLayout());
    myUpdateQueue.setPassThrough(false);  // we don't want passthrough mode, even in unit tests
    myProject = project;
    initTree();

    add(ScrollPaneFactory.createScrollPane(myTree), BorderLayout.CENTER);
    myDependencyValidationManager = DependencyValidationManager.getInstance(myProject);

    final UiNotifyConnector uiNotifyConnector = new UiNotifyConnector(myTree, myUpdateQueue);
    Disposer.register(this, myUpdateQueue);
    Disposer.register(this, uiNotifyConnector);

    if (isTreeShowing()) {
      myUpdateQueue.showNotify();
    }
  }

  public void initListeners() {
    final MessageBusConnection connection = myProject.getMessageBus().connect(this);
    connection.subscribe(ModuleRootListener.class, new MyModuleRootListener());
    connection.subscribe(ProblemListener.class, myProblemListener);
    PsiManager.getInstance(myProject).addPsiTreeChangeListener(myPsiTreeChangeAdapter);
    ChangeListManager.getInstance(myProject).addChangeListListener(myChangesListListener);
    FileStatusManager.getInstance(myProject).addFileStatusListener(myFileStatusListener, myProject);
  }

  @Override
  public void dispose() {
    FileTreeModelBuilder.clearCaches(myProject);
    PsiManager.getInstance(myProject).removePsiTreeChangeListener(myPsiTreeChangeAdapter);
    ChangeListManager.getInstance(myProject).removeChangeListListener(myChangesListListener);
  }

  public void selectNode(final PsiElement element, final PsiFileSystemItem file, final boolean requestFocus) {
    final Runnable runnable = () -> myUpdateQueue.queue(new Update("Select") {
      @Override
      public void run() {
        if (myProject.isDisposed()) return;
        PackageDependenciesNode node = myBuilder.findNode(file, element);
        if (node != null && node.getPsiElement() != element) {
          final TreePath path = new TreePath(node.getPath());
          if (myTree.isCollapsed(path)) {
            myTree.expandPath(path);
            myTree.makeVisible(path);
          }
        }
        node = myBuilder.findNode(file, element);
        if (node != null) {
          TreeUtil.selectPath(myTree, new TreePath(node.getPath()));
          if (requestFocus) {
            IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(myTree);
          }
        }
      }
    });
    doWhenDone(runnable);
  }

  private void doWhenDone(Runnable runnable) {
    if (myActionCallback == null || myProject.getApplication().isUnitTestMode()) {
      runnable.run();
    }
    else {
      myActionCallback.doWhenDone(runnable);
    }
  }

  public void selectScope(final NamedScope scope) {
    refreshScope(scope);
    if (scope != DefaultScopesProvider.getAllScope() && scope != null) {
      CURRENT_SCOPE_NAME = scope.getName();
    }
  }

  public JPanel getPanel() {
    return this;
  }

  private void initTree() {
    myTree.setCellRenderer(new MyTreeCellRenderer());
    myTree.setRootVisible(false);
    myTree.setShowsRootHandles(true);
    UIUtil.setLineStyleAngled(myTree);
    TreeUtil.installActions(myTree);
    EditSourceOnDoubleClickHandler.install(myTree);
    new TreeSpeedSearch(myTree);
    myCopyPasteDelegator = new CopyPasteDelegator(myProject, this) {
      @Override
      @Nonnull
      protected PsiElement[] getSelectedElements() {
        return getSelectedPsiElements();
      }
    };
    myTreeExpansionMonitor = PackageTreeExpansionMonitor.install(myTree, myProject);
    List<ScopeTreeStructureExpander> extensionList = myProject.getExtensionList(ScopeTreeStructureExpander.class);
    for (ScopeTreeStructureExpander expander : extensionList) {
      myTree.addTreeWillExpandListener(expander);
    }
    if (extensionList.isEmpty()) {
      myTree.addTreeWillExpandListener(new SortingExpandListener());
    }
    myTree.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (KeyEvent.VK_ENTER == e.getKeyCode()) {
          final Object component = myTree.getLastSelectedPathComponent();
          if (component instanceof DefaultMutableTreeNode) {
            final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode)component;
            if (selectedNode.isLeaf()) {
              OpenSourceUtil.openSourcesFrom(DataManager.getInstance().getDataContext(myTree), false);
            }
          }
        }
      }
    });
    CustomizationUtil.installPopupHandler(myTree, IdeActions.GROUP_PROJECT_VIEW_POPUP, ActionPlaces.PROJECT_VIEW_POPUP);
  }

  @Nonnull
  private PsiElement[] getSelectedPsiElements() {
    final TreePath[] treePaths = myTree.getSelectionPaths();
    if (treePaths != null) {
      Set<PsiElement> result = new HashSet<>();
      for (TreePath path : treePaths) {
        PackageDependenciesNode node = (PackageDependenciesNode)path.getLastPathComponent();
        final PsiElement psiElement = node.getPsiElement();
        if (psiElement != null && psiElement.isValid()) {
          result.add(psiElement);
        }
      }
      return PsiUtilBase.toPsiElementArray(result);
    }
    return PsiElement.EMPTY_ARRAY;
  }

  public void refreshScope(@Nullable NamedScope scope) {
    FileTreeModelBuilder.clearCaches(myProject);
    if (scope == null) { //was deleted
      scope = DefaultScopesProvider.getAllScope();
    }
    LOG.assertTrue(scope != null);
    final NamedScopesHolder holder = NamedScopesHolder.getHolder(myProject, scope.getName(), myDependencyValidationManager);
    final PackageSet packageSet = scope.getValue() != null ? scope.getValue() : new InvalidPackageSet("");
    final DependenciesPanel.DependencyPanelSettings settings = new DependenciesPanel.DependencyPanelSettings();
    settings.UI_FILTER_LEGALS = true;
    settings.UI_GROUP_BY_SCOPE_TYPE = false;
    settings.UI_SHOW_FILES = true;
    final ProjectView projectView = ProjectView.getInstance(myProject);
    settings.UI_FLATTEN_PACKAGES = projectView.isFlattenPackages(ScopeViewPane.ID);
    settings.UI_COMPACT_EMPTY_MIDDLE_PACKAGES = projectView.isHideEmptyMiddlePackages(ScopeViewPane.ID);
    settings.UI_SHOW_MODULES = projectView.isShowModules(ScopeViewPane.ID);
    settings.UI_SHOW_MODULE_GROUPS = projectView.isShowModules(ScopeViewPane.ID);
    myBuilder = new FileTreeModelBuilder(myProject, file -> packageSet != null && packageSet.contains(file, myProject, holder), settings);
    myTree.setPaintBusy(true);
    myBuilder.setTree(myTree);
    myTree.getEmptyText().setText("Loading...");
    myActionCallback = new AsyncResult<>();
    myTree.putClientProperty(TreeState.CALLBACK, new WeakReference<ActionCallback>(myActionCallback));
    myTree.setModel(myBuilder.build(myProject, true, () -> {
      myTree.setPaintBusy(false);
      myTree.getEmptyText().setText(UILocalize.messageNothingtoshow().get());
      myActionCallback.setDone();
    }));
    ((PackageDependenciesNode)myTree.getModel().getRoot()).sortChildren();
    ((DefaultTreeModel)myTree.getModel()).reload();
    FileTreeModelBuilder.clearCaches(myProject);
  }

  protected NamedScope getCurrentScope() {
    NamedScope scope = NamedScopesHolder.getScope(myProject, CURRENT_SCOPE_NAME);
    if (scope == null) {
      scope = DefaultScopesProvider.getAllScope();
    }
    LOG.assertTrue(scope != null);
    return scope;
  }

  @Nullable
  public Object getData(Key<?> dataId) {
    if (LangDataKeys.MODULE_CONTEXT == dataId) {
      final TreePath selectionPath = myTree.getSelectionPath();
      if (selectionPath != null) {
        PackageDependenciesNode node = (PackageDependenciesNode)selectionPath.getLastPathComponent();
        if (node instanceof ModuleNode) {
          return ((ModuleNode)node).getModule();
        }
      }
    }
    if (PsiElement.KEY == dataId) {
      final TreePath selectionPath = myTree.getSelectionPath();
      if (selectionPath != null) {
        PackageDependenciesNode node = (PackageDependenciesNode)selectionPath.getLastPathComponent();
        return node != null && node.isValid() ? node.getPsiElement() : null;
      }
    }
    final TreePath[] treePaths = myTree.getSelectionPaths();
    if (treePaths != null) {
      if (PsiElement.KEY_OF_ARRAY == dataId) {
        Set<PsiElement> psiElements = new HashSet<>();
        for (TreePath treePath : treePaths) {
          final PackageDependenciesNode node = (PackageDependenciesNode)treePath.getLastPathComponent();
          if (node.isValid()) {
            final PsiElement psiElement = node.getPsiElement();
            if (psiElement != null) {
              psiElements.add(psiElement);
            }
          }
        }
        return psiElements.isEmpty() ? null : PsiUtilBase.toPsiElementArray(psiElements);
      }
    }
    if (IdeView.KEY == dataId) {
      return myIdeView;
    }
    if (CutProvider.KEY == dataId) {
      return myCopyPasteDelegator.getCutProvider();
    }
    if (CopyProvider.KEY == dataId) {
      return myCopyPasteDelegator.getCopyProvider();
    }
    if (PasteProvider.KEY == dataId) {
      return myCopyPasteDelegator.getPasteProvider();
    }
    if (DeleteProvider.KEY == dataId) {
      if (getSelectedModules() != null) {
        return myDeleteModuleProvider;
      }
      return myDeletePSIElementProvider;
    }
    if (LangDataKeys.PASTE_TARGET_PSI_ELEMENT == dataId) {
      final TreePath selectionPath = myTree.getSelectionPath();
      if (selectionPath != null) {
        final Object pathComponent = selectionPath.getLastPathComponent();
        if (pathComponent instanceof DirectoryNode) {
          return ((DirectoryNode)pathComponent).getTargetDirectory();
        }
      }
    }
    return null;
  }

  @Nullable
  private Module[] getSelectedModules() {
    final TreePath[] treePaths = myTree.getSelectionPaths();
    if (treePaths != null) {
      Set<Module> result = new HashSet<>();
      for (TreePath path : treePaths) {
        PackageDependenciesNode node = (PackageDependenciesNode)path.getLastPathComponent();
        if (node instanceof ModuleNode) {
          result.add(((ModuleNode)node).getModule());
        }
        else if (node instanceof ModuleGroupNode) {
          final ModuleGroupNode groupNode = (ModuleGroupNode)node;
          final ModuleGroup moduleGroup = groupNode.getModuleGroup();
          result.addAll(moduleGroup.modulesInGroup(myProject, true));
        }
      }
      return result.isEmpty() ? null : result.toArray(new Module[result.size()]);
    }
    return null;
  }

  private void reload(@Nullable final DefaultMutableTreeNode rootToReload) {
    final DefaultTreeModel treeModel = (DefaultTreeModel)myTree.getModel();
    if (rootToReload != null && rootToReload != treeModel.getRoot()) {
      final List<TreePath> treePaths = TreeUtil.collectExpandedPaths(myTree, new TreePath(rootToReload.getPath()));
      final List<TreePath> selectionPaths = TreeUtil.collectSelectedPaths(myTree, new TreePath(rootToReload.getPath()));
      final TreePath path = new TreePath(rootToReload.getPath());
      final boolean wasCollapsed = myTree.isCollapsed(path);
      final Runnable runnable = () -> {
        if (!isTreeShowing() || rootToReload.getParent() == null) return;
        TreeUtil.sort(rootToReload, getNodeComparator());
        treeModel.reload(rootToReload);
        if (!wasCollapsed) {
          myTree.collapsePath(path);
          for (TreePath treePath : treePaths) {
            myTree.expandPath(treePath);
          }
          for (TreePath selectionPath : selectionPaths) {
            TreeUtil.selectPath(myTree, selectionPath);
          }
        }
      };
      if (myProject.getApplication().isUnitTestMode()) {
        runnable.run();
      }
      else {
        SwingUtilities.invokeLater(runnable);
      }
    }
    else {
      TreeUtil.sort(treeModel, getNodeComparator());
      treeModel.reload();
    }
  }

  private DependencyNodeComparator getNodeComparator() {
    return new DependencyNodeComparator(ProjectView.getInstance(myProject).isSortByType(ScopeViewPane.ID));
  }

  public void setSortByType() {
    myTreeExpansionMonitor.freeze();
    reload(null);
    myTreeExpansionMonitor.restore();
  }

  AsyncResult<Void> getActionCallback() {
    return myActionCallback;
  }

  private class MyTreeCellRenderer extends ColoredTreeCellRenderer {
    @RequiredUIAccess
    @Override
    public void customizeCellRenderer(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
      Font font = UIUtil.getTreeFont();
      setFont(font.deriveFont(font.getSize() + JBUI.scale(1f)));

      if (value instanceof PackageDependenciesNode) {
        PackageDependenciesNode node = (PackageDependenciesNode)value;
        try {
          setIcon(node.getIcon());
        }
        catch (IndexNotReadyException ignore) {
        }
        final SimpleTextAttributes regularAttributes = SimpleTextAttributes.REGULAR_ATTRIBUTES;
        TextAttributes textAttributes = TextAttributesUtil.toTextAttributes(regularAttributes);
        if (node instanceof BasePsiNode && ((BasePsiNode)node).isDeprecated()) {
          textAttributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.DEPRECATED_ATTRIBUTES).clone();
        }
        final PsiElement psiElement = node.getPsiElement();
        textAttributes.setForegroundColor(CopyPasteManager.getInstance().isCutElement(psiElement) ? CopyPasteManager.CUT_COLOR : node.getColor());
        append(node.toString(), TextAttributesUtil.fromTextAttributes(textAttributes));

        String oldToString = toString();
        if (toString().equals(oldToString)) {   // nothing was decorated
          final String locationString = node.getComment();
          if (locationString != null && locationString.length() > 0) {
            append(" (" + locationString + ")", SimpleTextAttributes.GRAY_ATTRIBUTES);
          }
        }
      }
    }
  }

  private class MyPsiTreeChangeAdapter extends PsiTreeChangeAdapter {
    @Override
    public void childAdded(@Nonnull final PsiTreeChangeEvent event) {
      final PsiElement element = event.getParent();
      final PsiElement child = event.getChild();
      if (child == null) return;
      if (element.getContainingFile() == null) {
        queueUpdateAsync(() -> {
          if (!child.isValid()) return;
          processNodeCreation(child);
        });
      }
    }

    @RequiredReadAction
    private void processNodeCreation(final PsiElement psiElement) {
      if (psiElement instanceof PsiFile && !isInjected((PsiFile)psiElement)) {
        final PackageDependenciesNode rootToReload = myBuilder.addFileNode((PsiFile)psiElement);
        if (rootToReload != null) {
          reload(rootToReload);
        }
      }
      else if (psiElement instanceof PsiDirectory) {
        final PsiElement[] children = psiElement.getChildren();
        if (children.length > 0) {
          for (PsiElement child : children) {
            processNodeCreation(child);
          }
        }
        else {
          final PackageDependenciesNode node = myBuilder.addDirNode((PsiDirectory)psiElement);
          if (node != null) {
            reload((DefaultMutableTreeNode)node.getParent());
          }
        }
      }
    }

    @Override
    public void beforeChildRemoval(@Nonnull final PsiTreeChangeEvent event) {
      final PsiElement child = event.getChild();
      final PsiElement parent = event.getParent();
      if (parent instanceof PsiDirectory directory && (child instanceof PsiFile file && !isInjected(file) || child instanceof PsiDirectory)) {
        queueUpdateImmediately(() -> {
          final DefaultMutableTreeNode rootToReload = myBuilder.removeNode(child, directory);
          if (rootToReload != null) {
            reload(rootToReload);
          }
        });
      }
    }

    @Override
    public void beforeChildMovement(@Nonnull PsiTreeChangeEvent event) {
      final PsiElement oldParent = event.getOldParent();
      final PsiElement child = event.getChild();
      if (oldParent instanceof PsiDirectory oldParentDirectory) {
        if (child instanceof PsiFileSystemItem && !(child instanceof PsiFile file && isInjected(file))) {
          queueUpdateImmediately(() -> {
            final DefaultMutableTreeNode rootToReload =
              myBuilder.removeNode(child, child instanceof PsiDirectory directory ? directory : oldParentDirectory);
            if (rootToReload != null) {
              reload(rootToReload);
            }
          });
        }
      }
    }

    @Override
    public void childMoved(@Nonnull PsiTreeChangeEvent event) {
      final PsiElement newParent = event.getNewParent();
      final PsiElement child = event.getChild();
      if (newParent instanceof PsiDirectory) {
        if (child instanceof PsiFileSystemItem && (!(child instanceof PsiFile) || !isInjected((PsiFile)child))) {
          final PsiFileSystemItem file = (PsiFileSystemItem)child;
          queueUpdateImmediately(() -> {
            final VirtualFile virtualFile = file.getVirtualFile();
            if (virtualFile != null && virtualFile.isValid()) {
              final PsiFileSystemItem newFile =
                file.isValid() ? file : (
                  file.isDirectory()
                    ? PsiManager.getInstance(myProject).findDirectory(virtualFile)
                    : PsiManager.getInstance(myProject).findFile(virtualFile)
                );
              if (newFile != null) {
                final PackageDependenciesNode rootToReload =
                  newFile.isDirectory() ? myBuilder.addDirNode((PsiDirectory)newFile) : myBuilder.addFileNode((PsiFile)newFile);
                if (rootToReload != null) {
                  reload(rootToReload);
                }
              }
            }
          });
        }
      }
    }

    @Override
    public void childrenChanged(@Nonnull PsiTreeChangeEvent event) {
      final PsiElement parent = event.getParent();
      final PsiFile file = parent.getContainingFile();
      if (file != null) {
        if (!file.getViewProvider().isPhysical() && !isInjected(file)) return;
        queueUpdateAsync(() -> {
          if (file.isValid() && file.getViewProvider().isPhysical()) {
            final NamedScope scope = getCurrentScope();
            final PackageSet packageSet = scope.getValue();
            if (packageSet == null) return; //invalid scope selected
            if (packageSet.contains(
              file.getVirtualFile(),
              file.getProject(),
              NamedScopesHolder.getHolder(myProject, scope.getName(), myDependencyValidationManager)
            )) {
              reload(myBuilder.getFileParentNode(file.getVirtualFile()));
            }
          }
        });
      }
    }

    @Override
    public final void propertyChanged(@Nonnull PsiTreeChangeEvent event) {
      String propertyName = event.getPropertyName();
      final PsiElement element = event.getElement();
      if (element != null) {
        final NamedScope scope = getCurrentScope();
        if (propertyName.equals(PsiTreeChangeEvent.PROP_FILE_NAME) || propertyName.equals(PsiTreeChangeEvent.PROP_FILE_TYPES)) {
          queueUpdateAsync(() -> {
            if (element.isValid()) {
              processRenamed(scope, element.getContainingFile());
            }
          });
        }
        else if (propertyName.equals(PsiTreeChangeEvent.PROP_DIRECTORY_NAME)) {
          queueRefreshScope(scope, (PsiDirectory)element);
        }
      }
    }

    @Override
    public void childReplaced(@Nonnull final PsiTreeChangeEvent event) {
      final NamedScope scope = getCurrentScope();
      final PsiElement element = event.getNewChild();
      final PsiFile psiFile = event.getFile();
      if (psiFile != null && !isInjected(psiFile)) {
        if (psiFile.getLanguage() == psiFile.getViewProvider().getBaseLanguage()) {
          queueUpdateAsync(() -> processRenamed(scope, psiFile));
        }
      }
      else if (element instanceof PsiDirectory && element.isValid()) {
        queueRefreshScope(scope, (PsiDirectory)element);
      }
    }

    private boolean isInjected(PsiFile psiFile) {
      return InjectedLanguageManager.getInstance(myProject).isInjectedFragment(psiFile);
    }

    private void queueRefreshScope(final NamedScope scope, final PsiDirectory dir) {
      myUpdateQueue.cancelAllUpdates();
      queueUpdateAsync(() -> {
        myTreeExpansionMonitor.freeze();
        refreshScope(scope);
        doWhenDone(() -> {
          myTreeExpansionMonitor.restore();
          final PackageDependenciesNode dirNode = myBuilder.findNode(dir, dir);
          if (dirNode != null) {
            TreeUtil.selectPath(myTree, new TreePath(dirNode.getPath()));
          }
        });
      });
    }

    private void processRenamed(final NamedScope scope, final PsiFile file) {
      if (!file.isValid() || !file.getViewProvider().isPhysical()) return;
      final PackageSet packageSet = scope.getValue();
      if (packageSet == null) return; //invalid scope selected
      if (packageSet.contains(file.getVirtualFile(), file.getProject(), NamedScopesHolder.getHolder(myProject, scope.getName(), myDependencyValidationManager))) {
        reload(myBuilder.addFileNode(file));
      }
      else {
        final DefaultMutableTreeNode rootToReload = myBuilder.removeNode(file, file.getParent());
        if (rootToReload != null) {
          reload(rootToReload);
        }
      }
    }

    private void queueUpdateImmediately(final Runnable request) {
      queueUpdate(request, true);
    }

    private void queueUpdateAsync(final Runnable request) {
      queueUpdate(request, false);
    }

    //expand/collapse state should be restored in actual request if needed
    private void queueUpdate(final Runnable request, boolean updateImmediately) {
      final Runnable wrapped = () -> {
        if (myProject.isDisposed()) return;
        request.run();
      };
      if (updateImmediately && isTreeShowing()) {
        myUpdateQueue.run(new Update(request) {
          @Override
          public void run() {
            wrapped.run();
          }
        });
      }
      else {
        myUpdateQueue.queue(new Update(request) {
          @Override
          public void run() {
            wrapped.run();
          }

          @Override
          public boolean isExpired() {
            return !isTreeShowing();
          }
        });
      }
    }
  }

  private class MyModuleRootListener extends ModuleRootAdapter {
    @Override
    public void rootsChanged(ModuleRootEvent event) {
      myUpdateQueue.cancelAllUpdates();
      myUpdateQueue.queue(new Update("RootsChanged") {
        @Override
        public void run() {
          refreshScope(getCurrentScope());
        }

        @Override
        public boolean isExpired() {
          return !isTreeShowing();
        }
      });
    }
  }

  private class MyIdeView implements IdeView {
    @Override
    public void selectElement(final PsiElement element) {
      if (element != null) {
        final PackageSet packageSet = getCurrentScope().getValue();
        final PsiFile psiFile = element.getContainingFile();
        if (packageSet == null) return;
        final VirtualFile virtualFile = psiFile != null ? psiFile.getVirtualFile() : (element instanceof PsiDirectory ? ((PsiDirectory)element).getVirtualFile() : null);
        if (virtualFile != null) {
          final ProjectView projectView = ProjectView.getInstance(myProject);
          final NamedScopesHolder holder = NamedScopesHolder.getHolder(myProject, CURRENT_SCOPE_NAME, myDependencyValidationManager);
          if (!packageSet.contains(virtualFile, myProject, holder)) {
            projectView.changeView(ProjectViewPaneImpl.ID);
          }
          projectView.select(element, virtualFile, false);
        }
        Editor editor = EditorHelper.openInEditor(element);
        if (editor != null) {
          ToolWindowManager.getInstance(myProject).activateEditorComponent();
        }
      }
    }

    @Nullable
    private PsiDirectory getDirectory() {
      final TreePath[] selectedPaths = myTree.getSelectionPaths();
      if (selectedPaths != null) {
        if (selectedPaths.length != 1) return null;
        TreePath path = selectedPaths[0];
        final PackageDependenciesNode node = (PackageDependenciesNode)path.getLastPathComponent();
        if (!node.isValid()) return null;
        if (node instanceof DirectoryNode) {
          return (PsiDirectory)node.getPsiElement();
        }
        else if (node instanceof BasePsiNode) {
          final PsiElement psiElement = node.getPsiElement();
          LOG.assertTrue(psiElement != null);
          final PsiFile psiFile = psiElement.getContainingFile();
          LOG.assertTrue(psiFile != null);
          return psiFile.getContainingDirectory();
        }
        else if (node instanceof FileNode) {
          final PsiFile psiFile = (PsiFile)node.getPsiElement();
          LOG.assertTrue(psiFile != null);
          return psiFile.getContainingDirectory();
        }
      }
      return null;
    }

    @Override
    public PsiDirectory[] getDirectories() {
      PsiDirectory directory = getDirectory();
      return directory == null ? PsiDirectory.EMPTY_ARRAY : new PsiDirectory[]{directory};
    }

    @Override
    @Nullable
    public PsiDirectory getOrChooseDirectory() {
      return DirectoryChooserUtil.getOrChooseDirectory(this);
    }
  }

  private final class MyDeletePSIElementProvider implements DeleteProvider {
    @Override
    public boolean canDeleteElement(@Nonnull DataContext dataContext) {
      final PsiElement[] elements = getSelectedPsiElements();
      return DeleteHandler.shouldEnableDeleteAction(elements);
    }

    @Override
    public void deleteElement(@Nonnull DataContext dataContext) {
      List<PsiElement> allElements = Arrays.asList(getSelectedPsiElements());
      ArrayList<PsiElement> validElements = new ArrayList<>();
      for (PsiElement psiElement : allElements) {
        if (psiElement != null && psiElement.isValid()) validElements.add(psiElement);
      }
      final PsiElement[] elements = PsiUtilBase.toPsiElementArray(validElements);

      LocalHistoryAction a = LocalHistory.getInstance().startAction(IdeLocalize.progressDeleting().get());
      try {
        DeleteHandler.deletePsiElement(elements, myProject);
      }
      finally {
        a.finish();
      }
    }
  }

  public DnDAwareTree getTree() {
    return myTree;
  }

  private class MyProblemListener implements ProblemListener {
    @Override
    public void problemsAppeared(@Nonnull VirtualFile file) {
      addNode(file, DefaultScopesProvider.getInstance(myProject).getProblemsScope().getName());
    }

    @Override
    public void problemsDisappeared(@Nonnull VirtualFile file) {
      removeNode(file, DefaultScopesProvider.getInstance(myProject).getProblemsScope().getName());
    }
  }

  private void addNode(VirtualFile file, final String scopeName) {
    queueUpdate(file, new Function<>() {
      @Override
      @Nullable
      public DefaultMutableTreeNode apply(final PsiFile psiFile) {
        return myBuilder.addFileNode(psiFile);
      }
    }, scopeName);
  }

  private void removeNode(VirtualFile file, final String scopeName) {
    queueUpdate(file, new Function<>() {
      @Override
      @Nullable
      public DefaultMutableTreeNode apply(final PsiFile psiFile) {
        return myBuilder.removeNode(psiFile, psiFile.getContainingDirectory());
      }
    }, scopeName);
  }

  private void queueUpdate(final VirtualFile fileToRefresh, final Function<PsiFile, DefaultMutableTreeNode> rootToReloadGetter, final String scopeName) {
    if (myProject.isDisposed()) return;
    ProjectViewPane pane = ProjectView.getInstance(myProject).getCurrentProjectViewPane();
    if (pane == null || !ScopeViewPane.ID.equals(pane.getId()) || !scopeName.equals(pane.getSubId())) {
      return;
    }
    myUpdateQueue.queue(new Update(fileToRefresh) {
      @Override
      @RequiredReadAction
      public void run() {
        if (myProject.isDisposed() || !fileToRefresh.isValid()) return;
        final PsiFile psiFile = PsiManager.getInstance(myProject).findFile(fileToRefresh);
        if (psiFile != null) {
          reload(rootToReloadGetter.apply(psiFile));
        }
      }

      @Override
      public boolean isExpired() {
        return !isTreeShowing();
      }
    });
  }

  private boolean isTreeShowing() {
    return myTree.isShowing() || Application.get().isUnitTestMode();
  }

  private class MyChangesListListener extends ChangeListAdapter {
    @Override
    public void changeListAdded(ChangeList list) {
      fireListeners(list, null);
    }

    @Override
    public void changeListRemoved(ChangeList list) {
      fireListeners(list, null);
    }

    @Override
    public void changeListRenamed(ChangeList list, String oldName) {
      fireListeners(list, oldName);
    }

    private void fireListeners(ChangeList list, @Nullable String oldName) {
      ProjectViewPane pane = ProjectView.getInstance(myProject).getCurrentProjectViewPane();
      if (pane == null || !ScopeViewPane.ID.equals(pane.getId())) {
        return;
      }
      final String subId = pane.getSubId();
      if (!list.getName().equals(subId) && (oldName == null || !oldName.equals(subId))) {
        return;
      }
      Application.get().invokeLater(() -> myDependencyValidationManager.fireScopeListeners(), myProject.getDisposed());
    }

    @Override
    public void changesRemoved(Collection<Change> changes, ChangeList fromList) {
      final String name = fromList.getName();
      final Set<VirtualFile> files = new HashSet<>();
      collectFiles(changes, files);
      for (VirtualFile file : files) {
        removeNode(file, name);
      }
    }

    @Override
    public void changesAdded(Collection<Change> changes, ChangeList toList) {
      final String name = toList.getName();
      final Set<VirtualFile> files = new HashSet<>();
      collectFiles(changes, files);
      for (VirtualFile file : files) {
        addNode(file, name);
      }
    }

    private void collectFiles(Collection<Change> changes, Set<VirtualFile> files) {
      for (Change change : changes) {
        final ContentRevision afterRevision = change.getAfterRevision();
        if (afterRevision != null) {
          final VirtualFile virtualFile = afterRevision.getFile().getVirtualFile();
          if (virtualFile != null) {
            files.add(virtualFile);
          }
        }
      }
    }
  }

  private class SortingExpandListener implements TreeWillExpandListener {
    @Override
    public void treeWillExpand(TreeExpansionEvent event) throws ExpandVetoException {
      final TreePath path = event.getPath();
      if (path == null) return;
      final PackageDependenciesNode node = (PackageDependenciesNode)path.getLastPathComponent();
      node.sortChildren();
      ((DefaultTreeModel)myTree.getModel()).reload(node);
    }

    @Override
    public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException {
    }
  }
}
