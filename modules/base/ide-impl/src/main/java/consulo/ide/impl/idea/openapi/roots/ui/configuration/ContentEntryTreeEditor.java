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

package consulo.ide.impl.idea.openapi.roots.ui.configuration;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.AllIcons;
import consulo.application.ui.wm.IdeFocusManager;
import consulo.content.ContentFolderTypeProvider;
import consulo.dataContext.DataProvider;
import consulo.disposer.Disposer;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.FileChooserDescriptorFactory;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ToolbarLabelAction;
import consulo.ide.impl.idea.openapi.fileChooser.FileSystemTree;
import consulo.ide.impl.idea.openapi.fileChooser.actions.NewFolderAction;
import consulo.ide.impl.idea.openapi.fileChooser.ex.FileSystemTreeImpl;
import consulo.ide.impl.idea.openapi.fileChooser.tree.FileNode;
import consulo.ide.impl.idea.openapi.fileChooser.tree.FileRenderer;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.actions.ToggleFolderStateAction;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.setting.module.ModuleConfigurationState;
import consulo.language.content.ContentFoldersSupportUtil;
import consulo.language.content.LanguageContentFolderScopes;
import consulo.language.psi.PsiPackageSupportProvider;
import consulo.localize.LocalizeValue;
import consulo.module.content.layer.ContentEntry;
import consulo.module.content.layer.ContentFolder;
import consulo.module.extension.ModuleExtension;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.ex.awt.ScrollPaneFactory;
import consulo.ui.ex.awt.SimpleColoredComponent;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.Tree;
import consulo.ui.ex.awt.tree.TreeUtil;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import consulo.util.io.FileUtil;
import consulo.util.lang.ComparatorUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.TreeCellRenderer;
import java.awt.*;
import java.util.Arrays;
import java.util.Set;

/**
 * @author Eugene Zhuravlev
 * Date: Oct 9, 2003
 * Time: 1:19:47 PM
 */
public class ContentEntryTreeEditor {
    private final Project myProject;
    private final ModuleConfigurationState myState;
    protected final Tree myTree;
    private FileSystemTreeImpl myFileSystemTree;
    private final JPanel myTreePanel;
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

        myFileSystemTree = new FileSystemTreeImpl(myProject, myDescriptor, myTree, null, init, null) {
            @Override
            protected TreeCellRenderer createFileRender() {
                return new FileRenderer() {
                    @Override
                    @RequiredUIAccess
                    protected void customize(SimpleColoredComponent renderer, Object value, boolean selected, boolean focused) {
                        super.customize(renderer, value, selected, focused);

                        ContentEntryEditor contentEntryEditor = getContentEntryEditor();
                        if (contentEntryEditor == null) {
                            return;
                        }
                        
                        if (value instanceof FileNode fileNode) {
                            VirtualFile treeFile = fileNode.getFile();
                            if (treeFile != null && treeFile.isDirectory()) {
                                final ContentEntry contentEntry = contentEntryEditor.getContentEntry();
                                renderer.setIcon(updateIcon(contentEntry, treeFile, renderer.getIcon()));
                            }
                        }
                        else if (value instanceof VirtualFile virtualFile) {
                            if (virtualFile.isDirectory()) {
                                final ContentEntry contentEntry = contentEntryEditor.getContentEntry();
                                renderer.setIcon(updateIcon(contentEntry, virtualFile, renderer.getIcon()));
                            }
                        }
                    }
                }.forTree();
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

    @RequiredReadAction
    private Image updateIcon(final ContentEntry entry, final VirtualFile file, Image originalIcon) {
        Image icon = originalIcon;
        VirtualFile currentRoot = null;
        for (ContentFolder contentFolder : entry.getFolders(LanguageContentFolderScopes.all())) {
            final VirtualFile contentPath = contentFolder.getFile();
            if (file.equals(contentPath)) {
                icon = ContentFoldersSupportUtil.getContentFolderIcon(contentFolder.getType(), contentFolder.getProperties());
            }
            else if (contentPath != null && VfsUtilCore.isAncestor(contentPath, file, true)) {
                if (currentRoot != null && VfsUtilCore.isAncestor(contentPath, currentRoot, false)) {
                    continue;
                }

                boolean hasSupport = false;
                for (ModuleExtension moduleExtension : getContentEntryEditor().getModel().getExtensions()) {
                    for (PsiPackageSupportProvider supportProvider : PsiPackageSupportProvider.EP_NAME.getExtensionList()) {
                        if (supportProvider.isSupported(moduleExtension)) {
                            hasSupport = true;
                            break;
                        }
                    }
                }
                icon = hasSupport ? contentFolder.getType().getChildDirectoryIcon(null, null) : AllIcons.Nodes.TreeOpen;
                currentRoot = contentPath;
            }
        }
        return icon;
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
//    if (myFileSystemTree != null) {
//      myFileSystemTree.updateTree();
//      final AsyncTreeModel model = (AsyncTreeModel)myTree.getModel();
//      final int visibleRowCount = myTree.getVisibleRowCount();
//      for (int row = 0; row < visibleRowCount; row++) {
//        final TreePath pathForRow = myTree.getPathForRow(row);
//        if (pathForRow != null) {
//          final TreeNode node = (TreeNode)pathForRow.getLastPathComponent();
//          if (node != null) {
//            model.nodeChanged(node);
//          }
//        }
//      }
//    }
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
            super(
                ActionLocalize.actionFilechooserNewfolderText(),
                ActionLocalize.actionFilechooserNewfolderDescription(),
                AllIcons.Actions.NewFolder
            );
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
