/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package consulo.ide.impl.idea.openapi.vcs.update;

import consulo.application.AllIcons;
import consulo.application.dumb.DumbAware;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.content.scope.PackageSet;
import consulo.content.scope.PackageSetBase;
import consulo.disposer.Disposer;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.ide.impl.idea.openapi.ui.PanelWithActionsAndCloseButton;
import consulo.ide.impl.idea.openapi.vcs.changes.committed.CommittedChangesBrowserUseCase;
import consulo.ide.impl.idea.openapi.vcs.changes.committed.CommittedChangesCache;
import consulo.ide.impl.idea.openapi.vcs.changes.committed.CommittedChangesTreeBrowser;
import consulo.ide.impl.idea.openapi.vcs.changes.committed.RefreshIncomingChangesAction;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.ide.impl.idea.ui.SelectionSaver;
import consulo.ui.ex.awt.tree.SmartExpander;
import consulo.ui.ex.awt.tree.EditSourceOnEnterKeyHandler;
import consulo.language.editor.PlatformDataKeys;
import consulo.localHistory.Label;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.TreeExpander;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.DefaultTreeExpander;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.ex.content.ContentManager;
import consulo.util.dataholder.Key;
import consulo.util.lang.Pair;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsBundle;
import consulo.versionControlSystem.VcsConfiguration;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.ui.UpdateInfoTree;
import consulo.versionControlSystem.update.ActionInfo;
import consulo.versionControlSystem.update.FileGroup;
import consulo.versionControlSystem.update.UpdatedFiles;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.versionControlSystem.versionBrowser.CommittedChangeList;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.pointer.VirtualFilePointer;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusListener;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.*;

public class UpdateInfoTreeImpl extends PanelWithActionsAndCloseButton implements UpdateInfoTree {
  private VirtualFile mySelectedFile;
  private FilePath mySelectedUrl;
  private final Tree myTree = new Tree();
  @Nonnull
  private final Project myProject;
  private final UpdatedFiles myUpdatedFiles;
  private UpdateRootNode myRoot;
  private DefaultTreeModel myTreeModel;
  private FileStatusListener myFileStatusListener;
  private final FileStatusManager myFileStatusManager;
  private final String myRootName;
  private final ActionInfo myActionInfo;
  private boolean myCanGroupByChangeList = false;
  private boolean myGroupByChangeList = false;
  private boolean myShowOnlyFilteredItems;
  private JLabel myLoadingChangeListsLabel;
  private List<CommittedChangeList> myCommittedChangeLists;
  private final JPanel myCenterPanel = new JPanel(new CardLayout());
  @NonNls
  private static final String CARD_STATUS = "Status";
  @NonNls
  private static final String CARD_CHANGES = "Changes";
  private CommittedChangesTreeBrowser myTreeBrowser;
  private final TreeExpander myTreeExpander;
  private final MyTreeIterable myTreeIterable;

  private Label myBefore;
  private Label myAfter;

  public UpdateInfoTreeImpl(@Nonnull ContentManager contentManager,
                            @Nonnull Project project,
                            UpdatedFiles updatedFiles,
                            String rootName,
                            ActionInfo actionInfo) {
    super(contentManager, "reference.versionControl.toolwindow.update");
    myActionInfo = actionInfo;

    myFileStatusListener = new FileStatusListener() {
      @Override
      public void fileStatusesChanged() {
        myTree.repaint();
      }

      @Override
      public void fileStatusChanged(@Nonnull VirtualFile virtualFile) {
        myTree.repaint();
      }
    };

    myProject = project;
    myUpdatedFiles = updatedFiles;
    myRootName = rootName;

    myShowOnlyFilteredItems = VcsConfiguration.getInstance(myProject).UPDATE_FILTER_BY_SCOPE;

    myFileStatusManager = FileStatusManager.getInstance(myProject);
    myFileStatusManager.addFileStatusListener(myFileStatusListener);
    createTree();
    init();
    myTreeExpander = new DefaultTreeExpander(myTree);
    myTreeIterable = new MyTreeIterable();
  }

  @Override
  public JComponent getComponent() {
    return this;
  }

  @Override
  public void dispose() {
    Disposer.dispose(myRoot);
    if (myFileStatusListener != null) {
      myFileStatusManager.removeFileStatusListener(myFileStatusListener);
      myFileStatusListener = null;
    }
  }

  public void setCanGroupByChangeList(final boolean canGroupByChangeList) {
    myCanGroupByChangeList = canGroupByChangeList;
    if (myCanGroupByChangeList) {
      myLoadingChangeListsLabel = new JLabel(VcsBundle.message("update.info.loading.changelists"));
      add(myLoadingChangeListsLabel, BorderLayout.SOUTH);
      myGroupByChangeList = VcsConfiguration.getInstance(myProject).UPDATE_GROUP_BY_CHANGELIST;
      if (myGroupByChangeList) {
        final CardLayout cardLayout = (CardLayout)myCenterPanel.getLayout();
        cardLayout.show(myCenterPanel, CARD_CHANGES);
      }
    }
  }

  @Override
  protected void addActionsTo(DefaultActionGroup group) {
    group.add(new MyGroupByPackagesAction());
    group.add(new GroupByChangeListAction());
    group.add(new FilterAction());
    group.add(ActionManager.getInstance().getAction(IdeActions.ACTION_EXPAND_ALL));
    group.add(ActionManager.getInstance().getAction(IdeActions.ACTION_COLLAPSE_ALL));
    group.add(ActionManager.getInstance().getAction(IdeActions.ACTION_SHOW_DIFF_COMMON));
  }

  @Override
  protected JComponent createCenterPanel() {
    JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTree);
    scrollPane.setBorder(IdeBorderFactory.createBorder(SideBorder.LEFT));
    myCenterPanel.add(CARD_STATUS, scrollPane);
    myTreeBrowser = new CommittedChangesTreeBrowser(myProject, Collections.emptyList());
    Disposer.register(this, myTreeBrowser);
    myTreeBrowser.setHelpId(getHelpId());
    myCenterPanel.add(CARD_CHANGES, myTreeBrowser);
    return myCenterPanel;
  }

  private void createTree() {
    SmartExpander.installOn(myTree);
    SelectionSaver.installOn(myTree);
    createTreeModel();

    myTree.addTreeSelectionListener(e -> {
      AbstractTreeNode treeNode = (AbstractTreeNode)e.getPath().getLastPathComponent();
      VirtualFilePointer pointer = null;
      if (treeNode instanceof FileTreeNode) {
        pointer = ((FileTreeNode)treeNode).getFilePointer();
        if (!pointer.isValid()) pointer = null;
      }
      if (pointer != null) {
        mySelectedUrl = getFilePath(pointer);
        mySelectedFile = pointer.getFile();
      }
      else {
        mySelectedUrl = null;
        mySelectedFile = null;
      }
    });
    myTree.setCellRenderer(new UpdateTreeCellRenderer());
    TreeUtil.installActions(myTree);
    new TreeSpeedSearch(myTree, path -> {
      Object last = path.getLastPathComponent();
      if (last instanceof AbstractTreeNode) {
        return ((AbstractTreeNode)last).getText();
      }
      return TreeSpeedSearch.NODE_DESCRIPTOR_TOSTRING.apply(path);
    }, true);

    myTree.addMouseListener(new PopupHandler() {
      @Override
      public void invokePopup(Component comp, int x, int y) {
        final DefaultActionGroup group = (DefaultActionGroup)ActionManager.getInstance().getAction("UpdateActionGroup");
        if (group != null) { //if no UpdateActionGroup was configured
          ActionPopupMenu popupMenu = ActionManager.getInstance().createActionPopupMenu(ActionPlaces.UPDATE_POPUP, group);
          popupMenu.getComponent().show(comp, x, y);
        }
      }
    });
    EditSourceOnDoubleClickHandler.install(myTree);
    EditSourceOnEnterKeyHandler.install(myTree);

    myTree.setSelectionRow(0);
  }

  private void createTreeModel() {
    myRoot = new UpdateRootNode(myUpdatedFiles, myProject, myRootName, myActionInfo);
    updateTreeModel();
    myTreeModel = new DefaultTreeModel(myRoot);
    myRoot.setTreeModel(myTreeModel);
    myTree.setModel(myTreeModel);
    myRoot.setTree(myTree);
  }

  private void updateTreeModel() {
    myRoot.rebuild(VcsConfiguration.getInstance(myProject).UPDATE_GROUP_BY_PACKAGES, getScopeFilter(), myShowOnlyFilteredItems);
    if (myTreeModel != null) {
      myTreeModel.reload();
    }
  }

  @Override
  public Object getData(@Nonnull Key<?> dataId) {
    if (myTreeBrowser != null && myTreeBrowser.isVisible()) {
      return null;
    }
    if (Navigatable.KEY == dataId) {
      if (mySelectedFile == null || !mySelectedFile.isValid()) return null;
      return new OpenFileDescriptorImpl(myProject, mySelectedFile);
    }
    else if (VirtualFile.KEY_OF_ARRAY == dataId) {
      return getVirtualFileArray();
    }
    else if (VcsDataKeys.IO_FILE_ARRAY == dataId) {
      return getFileArray();
    }
    else if (PlatformDataKeys.TREE_EXPANDER == dataId) {
      if (myGroupByChangeList) {
        return myTreeBrowser != null ? myTreeBrowser.getTreeExpander() : null;
      }
      else {
        return myTreeExpander;
      }
    }
    else if (VcsDataKeys.UPDATE_VIEW_SELECTED_PATH == dataId) {
      return mySelectedUrl;
    }
    else if (VcsDataKeys.UPDATE_VIEW_FILES_ITERABLE == dataId) {
      return myTreeIterable;
    }
    else if (VcsDataKeys.LABEL_BEFORE == dataId) {
      return myBefore;
    }
    else if (VcsDataKeys.LABEL_AFTER == dataId) {
      return myAfter;
    }

    return super.getData(dataId);
  }

  private class MyTreeIterator implements Iterator<Pair<FilePath, FileStatus>> {
    private final Enumeration myEnum;
    private FilePath myNext;
    private FileStatus myStatus;

    private MyTreeIterator() {
      myEnum = myRoot.depthFirstEnumeration();
      step();
    }

    @Override
    public boolean hasNext() {
      return myNext != null;
    }

    @Override
    public Pair<FilePath, FileStatus> next() {
      final FilePath result = myNext;
      final FileStatus status = myStatus;
      step();
      return Pair.create(result, status);
    }

    private void step() {
      myNext = null;
      while (myEnum.hasMoreElements()) {
        final Object o = myEnum.nextElement();
        if (o instanceof FileTreeNode) {
          final FileTreeNode treeNode = (FileTreeNode)o;
          VirtualFilePointer filePointer = treeNode.getFilePointer();
          if (!filePointer.isValid()) continue;

          myNext = getFilePath(filePointer);
          myStatus = FileStatus.MODIFIED;

          final GroupTreeNode parent = findParentGroupTreeNode(treeNode.getParent());
          if (parent != null) {
            final String id = parent.getFileGroupId();
            if (FileGroup.CREATED_ID.equals(id)) {
              myStatus = FileStatus.ADDED;
            }
            else if (FileGroup.REMOVED_FROM_REPOSITORY_ID.equals(id)) {
              myStatus = FileStatus.DELETED;
            }
          }
          break;
        }
      }
    }

    @Nullable
    private GroupTreeNode findParentGroupTreeNode(@Nonnull TreeNode treeNode) {
      TreeNode currentNode = treeNode;
      while (currentNode != null && !(currentNode instanceof GroupTreeNode)) {
        currentNode = currentNode.getParent();
      }
      return (GroupTreeNode)currentNode;
    }

    @Override
    public void remove() {
      throw new UnsupportedOperationException();
    }
  }

  private class MyTreeIterable implements Iterable<Pair<FilePath, FileStatus>> {
    @Override
    public Iterator<Pair<FilePath, FileStatus>> iterator() {
      return new MyTreeIterator();
    }
  }

  private VirtualFile[] getVirtualFileArray() {
    ArrayList<VirtualFile> result = new ArrayList<>();
    TreePath[] selectionPaths = myTree.getSelectionPaths();
    if (selectionPaths != null) {
      for (TreePath selectionPath : selectionPaths) {
        AbstractTreeNode treeNode = (AbstractTreeNode)selectionPath.getLastPathComponent();
        result.addAll(treeNode.getVirtualFiles());
      }
    }
    return VfsUtil.toVirtualFileArray(result);
  }

  @Nullable
  private File[] getFileArray() {
    ArrayList<File> result = new ArrayList<>();
    TreePath[] selectionPaths = myTree.getSelectionPaths();
    if (selectionPaths != null) {
      for (TreePath selectionPath : selectionPaths) {
        AbstractTreeNode treeNode = (AbstractTreeNode)selectionPath.getLastPathComponent();
        result.addAll(treeNode.getFiles());
      }
    }
    if (result.isEmpty()) return null;
    return result.toArray(new File[result.size()]);
  }

  int getFilteredFilesCount() {
    Pair<PackageSetBase, NamedScopesHolder> scopeFilter = getScopeFilter();
    int[] result = new int[1];
    TreeUtil.traverse(myRoot, node -> {
      if (node instanceof FileTreeNode) {
        if (((FileTreeNode)node).acceptFilter(scopeFilter, true)) {
          result[0]++;
        }
      }
      return true;
    });
    return result[0];
  }

  public void expandRootChildren() {
    TreeNode root = (TreeNode)myTreeModel.getRoot();

    if (root.getChildCount() == 1) {
      myTree.expandPath(new TreePath(new Object[]{root, root.getChildAt(0)}));
    }
  }

  public void setChangeLists(final List<CommittedChangeList> receivedChanges) {
    final boolean hasEmptyCaches = CommittedChangesCache.getInstance(myProject).hasEmptyCaches();

    myProject.getApplication().invokeLater(() -> {
      if (myLoadingChangeListsLabel != null) {
        remove(myLoadingChangeListsLabel);
        myLoadingChangeListsLabel = null;
      }
      myCommittedChangeLists = receivedChanges;
      myTreeBrowser.setItems(myCommittedChangeLists, CommittedChangesBrowserUseCase.UPDATE);
      if (hasEmptyCaches) {
        final StatusText statusText = myTreeBrowser.getEmptyText();
        statusText.clear();
        statusText.appendText("Click ").appendText(
          "Refresh",
          SimpleTextAttributes.LINK_ATTRIBUTES,
          e -> RefreshIncomingChangesAction.doRefresh(myProject)
        ).appendText(" to initialize repository changes cache");
      }
    }, myProject.getDisposed());
  }

  private class MyGroupByPackagesAction extends ToggleAction implements DumbAware {
    public MyGroupByPackagesAction() {
      super(VcsLocalize.actionNameGroupByPackages(), LocalizeValue.empty(), AllIcons.Actions.GroupByPackage);
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
      return !myProject.isDisposed() && VcsConfiguration.getInstance(myProject).UPDATE_GROUP_BY_PACKAGES;
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
      if (!myProject.isDisposed()) {
        VcsConfiguration.getInstance(myProject).UPDATE_GROUP_BY_PACKAGES = state;
        updateTreeModel();
      }
    }

    @Override
    public void update(@Nonnull final AnActionEvent e) {
      super.update(e);
      e.getPresentation().setEnabled(!myGroupByChangeList);
    }
  }

  private class GroupByChangeListAction extends ToggleAction implements DumbAware {
    public GroupByChangeListAction() {
      super(VcsLocalize.updateInfoGroupByChangelist(), LocalizeValue.empty(), AllIcons.Actions.ShowAsTree);
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
      return myGroupByChangeList;
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
      myGroupByChangeList = state;
      VcsConfiguration.getInstance(myProject).UPDATE_GROUP_BY_CHANGELIST = myGroupByChangeList;
      final CardLayout cardLayout = (CardLayout)myCenterPanel.getLayout();
      if (!myGroupByChangeList) {
        cardLayout.show(myCenterPanel, CARD_STATUS);
      }
      else {
        cardLayout.show(myCenterPanel, CARD_CHANGES);
      }
    }

    @Override
    public void update(@Nonnull final AnActionEvent e) {
      super.update(e);
      e.getPresentation().setVisible(myCanGroupByChangeList);
    }
  }

  public void setBefore(Label before) {
    myBefore = before;
  }

  public void setAfter(Label after) {
    myAfter = after;
  }

  @Nullable
  private Pair<PackageSetBase, NamedScopesHolder> getScopeFilter() {
    String scopeName = getFilterScopeName();
    if (scopeName != null) {
      for (NamedScopesHolder holder : NamedScopesHolder.getAllNamedScopeHolders(myProject)) {
        NamedScope scope = holder.getScope(scopeName);
        if (scope != null) {
          PackageSet packageSet = scope.getValue();
          if (packageSet instanceof PackageSetBase) {
            return Pair.create((PackageSetBase)packageSet, holder);
          }
        }
      }
    }
    return null;
  }

  @Nullable
  private String getFilterScopeName() {
    return VcsConfiguration.getInstance(myProject).UPDATE_FILTER_SCOPE_NAME;
  }

  @Nullable
  NamedScope getFilterScope() {
    Pair<PackageSetBase, NamedScopesHolder> filter = getScopeFilter();
    return filter == null ? null : filter.second.getScope(getFilterScopeName());
  }

  private class FilterAction extends ToggleAction implements DumbAware {
    public FilterAction() {
      super(LocalizeValue.localizeTODO("Scope Filter"), VcsLocalize.settingsFilterUpdateProjectInfoByScope(), AllIcons.General.Filter);
    }

    @Override
    public boolean isSelected(@Nonnull AnActionEvent e) {
      return myShowOnlyFilteredItems;
    }

    @Override
    public void setSelected(@Nonnull AnActionEvent e, boolean state) {
      myShowOnlyFilteredItems = state;
      VcsConfiguration.getInstance(myProject).UPDATE_FILTER_BY_SCOPE = myShowOnlyFilteredItems;
      updateTreeModel();
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
      super.update(e);
      e.getPresentation().setEnabled(!myGroupByChangeList && getFilterScopeName() != null);
    }
  }

  @Nonnull
  private static FilePath getFilePath(@Nonnull VirtualFilePointer filePointer) {
    return VcsUtil.getFilePath(filePointer.getPresentableUrl(), false);
  }
}
