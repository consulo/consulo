/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package com.intellij.openapi.roots.ui.configuration;

import com.intellij.icons.AllIcons;
import com.intellij.ide.util.treeView.AbstractTreeBuilder;
import com.intellij.ide.util.treeView.AbstractTreeStructure;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.idea.ActionsBundle;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataProvider;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ex.ToolbarLabelAction;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.fileChooser.FileSystemTree;
import com.intellij.openapi.fileChooser.actions.NewFolderAction;
import com.intellij.openapi.fileChooser.ex.FileSystemTreeImpl;
import com.intellij.openapi.fileChooser.impl.FileTreeBuilder;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectBundle;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ContentFolder;
import com.intellij.openapi.roots.ui.configuration.actions.ToggleFolderStateAction;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.IdeFocusManager;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.TreeSpeedSearch;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.containers.ComparatorUtil;
import com.intellij.util.ui.tree.TreeUtil;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.roots.ContentFolderTypeProvider;
import consulo.roots.ContentFoldersSupportUtil;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

/**
 * @author Eugene Zhuravlev
 *         Date: Oct 9, 2003
 *         Time: 1:19:47 PM
 */
public class ContentEntryTreeEditor {
  private final Project myProject;
  private final ModuleConfigurationState myState;
  protected final Tree myTree;
  private FileSystemTreeImpl myFileSystemTree;
  private final JPanel myTreePanel;
  private final DefaultMutableTreeNode EMPTY_TREE_ROOT = new DefaultMutableTreeNode(ProjectBundle.message("module.paths.empty.node"));
  protected final DefaultActionGroup myEditingActionsGroup;
  private ContentEntryEditor myContentEntryEditor;
  private final MyContentEntryEditorListener myContentEntryEditorListener = new MyContentEntryEditorListener();
  private final FileChooserDescriptor myDescriptor;

  public ContentEntryTreeEditor(Project project, ModuleConfigurationState state) {
    myProject = project;
    myState = state;
    myTree = new Tree();
    myTree.setRootVisible(true);
    myTree.setShowsRootHandles(true);

    myEditingActionsGroup = new DefaultActionGroup();

    TreeUtil.installActions(myTree);
    new TreeSpeedSearch(myTree);

    myTreePanel = new MyPanel(new BorderLayout());
    final JScrollPane scrollPane = ScrollPaneFactory.createScrollPane(myTree);
    myTreePanel.add(scrollPane, BorderLayout.CENTER);

    myTreePanel.setVisible(false);
    myDescriptor = FileChooserDescriptorFactory.createMultipleFoldersDescriptor();
    myDescriptor.setShowFileSystemRoots(false);
  }

  protected void updateMarkActions() {
    myEditingActionsGroup.removeAll();

    myEditingActionsGroup.add(new ToolbarLabelAction() {
      @RequiredUIAccess
      @Override
      public void update(@Nonnull AnActionEvent e) {
        super.update(e);
       e.getPresentation().setTextValue(LocalizeValue.localizeTODO("Mark as:"));
      }
    });
    Set<ContentFolderTypeProvider> folders = ContentFoldersSupportUtil.getSupportedFolders(myState.getRootModel());
    ContentFolderTypeProvider[] supportedFolders = folders.toArray(new ContentFolderTypeProvider[folders.size()]);
    Arrays.sort(supportedFolders, (o1, o2) -> ComparatorUtil.compareInt(o1.getWeight(), o2.getWeight()));
    for (ContentFolderTypeProvider contentFolderTypeProvider : supportedFolders) {
      ToggleFolderStateAction action = new ToggleFolderStateAction(myTree, this, contentFolderTypeProvider);
     /* CustomShortcutSet shortcutSet = editor.getMarkRootShortcutSet();
      if (shortcutSet != null) {
        action.registerCustomShortcutSet(shortcutSet, myTree);
      }     */
      myEditingActionsGroup.add(action);
    }
  }

  protected TreeCellRenderer getContentEntryCellRenderer() {
    return new ContentEntryTreeCellRenderer(this);
  }

  /**
   * @param contentEntryEditor : null means to clear the editor
   */
  public void setContentEntryEditor(@Nullable final ContentEntryEditor contentEntryEditor) {
    if (myContentEntryEditor != null && myContentEntryEditor.equals(contentEntryEditor)) {
      return;
    }
    if (myFileSystemTree != null) {
      Disposer.dispose(myFileSystemTree);
      myFileSystemTree = null;
    }
    if (myContentEntryEditor != null) {
      myContentEntryEditor.removeContentEntryEditorListener(myContentEntryEditorListener);
      myContentEntryEditor = null;
    }
    if (contentEntryEditor == null) {
      ((DefaultTreeModel)myTree.getModel()).setRoot(EMPTY_TREE_ROOT);
      myTreePanel.setVisible(false);
      return;
    }
    myTreePanel.setVisible(true);
    myContentEntryEditor = contentEntryEditor;
    myContentEntryEditor.addContentEntryEditorListener(myContentEntryEditorListener);

    final ContentEntry entry = contentEntryEditor.getContentEntry();
    assert entry != null : contentEntryEditor;
    final VirtualFile file = entry.getFile();
    myDescriptor.setRoots(file);
    if (file == null) {
      final String path = VfsUtilCore.urlToPath(entry.getUrl());
      myDescriptor.setTitle(FileUtil.toSystemDependentName(path));
    }

    final Runnable init = () -> {
      myFileSystemTree.updateTree();
      myFileSystemTree.select(file, null);
    };

    myFileSystemTree = new FileSystemTreeImpl(myProject, myDescriptor, myTree, getContentEntryCellRenderer(), init, null) {
      @Override
      protected AbstractTreeBuilder createTreeBuilder(JTree tree,
                                                      DefaultTreeModel treeModel,
                                                      AbstractTreeStructure treeStructure,
                                                      Comparator<NodeDescriptor> comparator,
                                                      FileChooserDescriptor descriptor,
                                                      final Runnable onInitialized) {
        return new MyFileTreeBuilder(tree, treeModel, treeStructure, comparator, descriptor, onInitialized);
      }
    };
    myFileSystemTree.showHiddens(true);
    Disposer.register(myProject, myFileSystemTree);

    final NewFolderAction newFolderAction = new MyNewFolderAction();
    final DefaultActionGroup mousePopupGroup = new DefaultActionGroup();
    mousePopupGroup.add(myEditingActionsGroup);
    mousePopupGroup.addSeparator();
    mousePopupGroup.add(newFolderAction);
    myFileSystemTree.registerMouseListener(mousePopupGroup);
  }

  public ContentEntryEditor getContentEntryEditor() {
    return myContentEntryEditor;
  }

  public JComponent createComponent() {
    updateMarkActions();
    return myTreePanel;
  }

  public void select(VirtualFile file) {
    if (myFileSystemTree != null) {
      myFileSystemTree.select(file, null);
    }
  }

  public void requestFocus() {
    IdeFocusManager.getGlobalInstance().doForceFocusWhenFocusSettlesDown(myTree);
  }

  public void update() {
    updateMarkActions();
    if (myFileSystemTree != null) {
      myFileSystemTree.updateTree();
      final DefaultTreeModel model = (DefaultTreeModel)myTree.getModel();
      final int visibleRowCount = myTree.getVisibleRowCount();
      for (int row = 0; row < visibleRowCount; row++) {
        final TreePath pathForRow = myTree.getPathForRow(row);
        if (pathForRow != null) {
          final TreeNode node = (TreeNode)pathForRow.getLastPathComponent();
          if (node != null) {
            model.nodeChanged(node);
          }
        }
      }
    }
  }

  private class MyContentEntryEditorListener implements ContentEntryEditor.ContentEntryEditorListener {
    @Override
    public void folderAdded(@Nonnull ContentEntryEditor editor, ContentFolder folder) {
      update();
    }

    @Override
    public void folderRemoved(@Nonnull ContentEntryEditor editor, ContentFolder file) {
      update();
    }
  }

  private static class MyNewFolderAction extends NewFolderAction {
    private MyNewFolderAction() {
      super(ActionsBundle.message("action.FileChooser.NewFolder.text"), ActionsBundle.message("action.FileChooser.NewFolder.description"), AllIcons.Actions.NewFolder);
    }
  }

  private static class MyFileTreeBuilder extends FileTreeBuilder {
    public MyFileTreeBuilder(JTree tree,
                             DefaultTreeModel treeModel,
                             AbstractTreeStructure treeStructure,
                             Comparator<NodeDescriptor> comparator,
                             FileChooserDescriptor descriptor,
                             @Nullable Runnable onInitialized) {
      super(tree, treeModel, treeStructure, comparator, descriptor, onInitialized);
    }

    @Override
    protected boolean isAlwaysShowPlus(NodeDescriptor nodeDescriptor) {
      return false; // need this in order to not show plus for empty directories
    }
  }

  private class MyPanel extends JPanel implements DataProvider {
    private MyPanel(final LayoutManager layout) {
      super(layout);
    }

    @Override
    @Nullable
    public Object getData(@Nonnull final Key<?> dataId) {
      if (FileSystemTree.DATA_KEY == dataId) {
        return myFileSystemTree;
      }
      return null;
    }
  }

  public DefaultActionGroup getEditingActionsGroup() {
    return myEditingActionsGroup;
  }
}
