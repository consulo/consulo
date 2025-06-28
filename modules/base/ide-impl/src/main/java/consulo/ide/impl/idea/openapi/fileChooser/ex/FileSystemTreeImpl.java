// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.fileChooser.ex;

import consulo.application.impl.internal.IdeaModalityState;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ide.impl.idea.openapi.fileChooser.FileSystemTree;
import consulo.ide.impl.idea.openapi.fileChooser.impl.FileTreeStructure;
import consulo.ide.impl.idea.openapi.fileChooser.tree.*;
import consulo.ide.impl.idea.openapi.vfs.VfsUtil;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.SimpleTextAttributes;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.awt.PopupHandler;
import consulo.ui.ex.awt.event.DoubleClickListener;
import consulo.ui.ex.awt.speedSearch.TreeSpeedSearch;
import consulo.ui.ex.awt.tree.*;
import consulo.ui.ex.localize.UILocalize;
import consulo.undoRedo.CommandProcessor;
import consulo.util.collection.Lists;
import consulo.util.dataholder.Key;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

public class FileSystemTreeImpl implements FileSystemTree {
    private final Tree myTree;
    private final AbstractTreeBuilder myTreeBuilder;
    private final Project myProject;
    private final ArrayList<Runnable> myOkActions = new ArrayList<>(2);
    private final FileChooserDescriptor myDescriptor;

    @Nonnull
    private final AsyncTreeModel myAsyncTreeModel;

    private final List<Listener> myListeners = Lists.newLockFreeCopyOnWriteList();
    private AbstractTreeModel myFileTreeModel;

    public FileSystemTreeImpl(@Nullable Project project, FileChooserDescriptor descriptor) {
        this(project, descriptor, new Tree(), null, null, null);
        myTree.setRootVisible(descriptor.isTreeRootVisible());
        myTree.setShowsRootHandles(true);
    }

    public FileSystemTreeImpl(
        @Nullable Project project,
        FileChooserDescriptor descriptor,
        Tree tree,
        @Nullable TreeCellRenderer renderer,
        @Nullable Runnable onInitialized,
        @Nullable Function<? super TreePath, String> speedSearchConverter
    ) {
        myProject = project;

        if (renderer == null) {
            renderer = createFileRender();
            myFileTreeModel = new FileTreeModel(
                descriptor,
                new FileRefresher(true, 3, () -> IdeaModalityState.stateForComponent(tree))
            );
        }
        else {
            FileTreeStructure treeStructure = new FileTreeStructure(project, descriptor);
            myFileTreeModel = new StructureTreeModel<>(treeStructure, this);
        }

        myDescriptor = descriptor;
        myTree = tree;
        myAsyncTreeModel = new AsyncTreeModel(myFileTreeModel, false, this);
        myTree.setModel(myAsyncTreeModel);
        myTreeBuilder = null;

        myTree.getSelectionModel().addTreeSelectionListener(e -> processSelectionChange());

        if (speedSearchConverter != null) {
            new TreeSpeedSearch(myTree, speedSearchConverter);
        }
        else {
            new TreeSpeedSearch(myTree);
        }
        TreeUtil.installActions(myTree);

        myTree.getSelectionModel()
            .setSelectionMode(descriptor.isChooseMultiple() ? TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION : TreeSelectionModel.SINGLE_TREE_SELECTION);
        registerTreeActions();

        if (renderer == null) {
            renderer = new NodeRenderer() {
                @RequiredUIAccess
                @Override
                public void customizeCellRenderer(
                    @Nonnull JTree tree,
                    Object value,
                    boolean selected,
                    boolean expanded,
                    boolean leaf,
                    int row,
                    boolean hasFocus
                ) {
                    super.customizeCellRenderer(tree, value, selected, expanded, leaf, row, hasFocus);
                    Object userObject = ((DefaultMutableTreeNode) value).getUserObject();
                    if (userObject instanceof FileNodeDescriptor fileNodeDescriptor) {
                        String comment = fileNodeDescriptor.getComment();
                        if (comment != null) {
                            append(comment, SimpleTextAttributes.REGULAR_ATTRIBUTES);
                        }
                    }
                }
            };
        }
        myTree.setCellRenderer(renderer);
    }

    protected TreeCellRenderer createFileRender() {
        return new FileRenderer().forTree();
    }

    private void registerTreeActions() {
        myTree.registerKeyboardAction(
            e -> performEnterAction(true),
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0),
            JComponent.WHEN_FOCUSED
        );

        new DoubleClickListener() {
            @Override
            protected boolean onDoubleClick(MouseEvent e) {
                performEnterAction(false);
                return true;
            }
        }.installOn(myTree);
    }

    private void performEnterAction(boolean toggleNodeState) {
        TreePath path = myTree.getSelectionPath();
        if (path != null) {
            if (isLeaf(path)) {
                fireOkAction();
            }
            else if (toggleNodeState) {
                if (myTree.isExpanded(path)) {
                    myTree.collapsePath(path);
                }
                else {
                    myTree.expandPath(path);
                }
            }
        }
    }

    public void addOkAction(Runnable action) {
        myOkActions.add(action);
    }

    private void fireOkAction() {
        for (Runnable action : myOkActions) {
            action.run();
        }
    }

    public void registerMouseListener(ActionGroup group) {
        PopupHandler.installUnknownPopupHandler(myTree, group, ActionManager.getInstance());
    }

    @Override
    public boolean areHiddensShown() {
        return myDescriptor.isShowHiddenFiles();
    }

    @Override
    public void showHiddens(boolean showHidden) {
        myDescriptor.withShowHiddenFiles(showHidden);

        UIAccess uiAccess = UIAccess.current();

        if (myFileTreeModel instanceof FileTreeModel fileTreeModel) {
            VirtualFile[] selectedFiles = getSelectedFiles();
            fileTreeModel.invalidate().onSuccess(o -> uiAccess.give(() -> select(selectedFiles, null)));
        }
    }

    @Override
    public void updateTree() {
        if (myTreeBuilder != null) {
            myTreeBuilder.queueUpdate();
        } else if (myFileTreeModel instanceof FileTreeModel fileTreeModel) {
            fileTreeModel.invalidate();
        }
    }

    @Override
    public void dispose() {
        if (myTreeBuilder != null) {
            Disposer.dispose(myTreeBuilder);
        }
    }

    public AbstractTreeBuilder getTreeBuilder() {
        return myTreeBuilder;
    }

    @Override
    public void select(VirtualFile file, @Nullable Runnable onDone) {
        select(new VirtualFile[]{file}, onDone);
    }

    @Override
    public void select(VirtualFile[] file, @Nullable Runnable onDone) {
        switch (file.length) {
            case 0:
                myTree.clearSelection();
                if (onDone != null) {
                    onDone.run();
                }
                break;
            case 1:
                myTree.clearSelection();
                TreeUtil.promiseSelect(myTree, new FileNodeVisitor(file[0])).onProcessed(path -> {
                    if (onDone != null) {
                        onDone.run();
                    }
                });
                break;
            default:
                myTree.clearSelection();
                TreeUtil.promiseSelect(myTree, Stream.of(file).map(FileNodeVisitor::new)).onProcessed(paths -> {
                    if (onDone != null) {
                        onDone.run();
                    }
                });
                break;
        }
    }

    @Override
    public void expand(VirtualFile file, @Nullable Runnable onDone) {
        TreeUtil.promiseExpand(myTree, new FileNodeVisitor(file)).onSuccess(path -> {
            if (path != null && onDone != null) {
                onDone.run();
            }
        });
    }

    @RequiredUIAccess
    public Exception createNewFolder(VirtualFile parentDirectory, String newFolderName) {
        return CommandProcessor.getInstance().<Exception>newCommand()
            .project(myProject)
            .name(UILocalize.fileChooserCreateNewFolderCommandName())
            .inWriteAction()
            .compute(() -> {
                try {
                    VirtualFile parent = parentDirectory;
                    for (String name : StringUtil.tokenize(newFolderName, "\\/")) {
                        VirtualFile folder = parent.createChildDirectory(this, name);
                        updateTree();
                        select(folder, null);
                        parent = folder;
                    }
                    return null;
                }
                catch (IOException e) {
                    return e;
                }
            });
    }

    @RequiredUIAccess
    public Exception createNewFile(
        VirtualFile parentDirectory,
        String newFileName,
        FileType fileType,
        String initialContent
    ) {
        return CommandProcessor.getInstance().<Exception>newCommand()
            .project(myProject)
            .name(UILocalize.fileChooserCreateNewFileCommandName())
            .inWriteAction()
            .compute(() -> {
                try {
                    String newFileNameWithExtension = newFileName.endsWith('.' + fileType.getDefaultExtension())
                        ? newFileName
                        : newFileName + '.' + fileType.getDefaultExtension();
                    VirtualFile file = parentDirectory.createChildData(this, newFileNameWithExtension);
                    VfsUtil.saveText(file, initialContent != null ? initialContent : "");
                    updateTree();
                    select(file, null);
                    return null;
                }
                catch (IOException e) {
                    return e;
                }
            });
    }

    @Override
    public JTree getTree() {
        return myTree;
    }

    @Override
    @Nullable
    public VirtualFile getSelectedFile() {
        TreePath path = myTree.getSelectionPath();
        if (path == null) {
            return null;
        }
        return getVirtualFile(path);
    }

    @Override
    @Nullable
    public VirtualFile getNewFileParent() {
        VirtualFile selected = getSelectedFile();
        if (selected != null) {
            return selected;
        }

        List<VirtualFile> roots = myDescriptor.getRoots();
        return roots.size() == 1 ? roots.get(0) : null;
    }

    @Override
    public <T> T getData(@Nonnull Key<T> key) {
        return myDescriptor.getUserData(key);
    }

    @Override
    @Nonnull
    public VirtualFile[] getSelectedFiles() {
        TreePath[] paths = myTree.getSelectionPaths();
        if (paths == null) {
            return VirtualFile.EMPTY_ARRAY;
        }

        List<VirtualFile> files = new ArrayList<>();
        for (TreePath path : paths) {
            VirtualFile file = getVirtualFile(path);
            if (file != null && file.isValid()) {
                files.add(file);
            }
        }
        return VfsUtilCore.toVirtualFileArray(files);
    }

    private boolean isLeaf(TreePath path) {
        Object component = path.getLastPathComponent();
        return component instanceof DefaultMutableTreeNode node ? node.isLeaf() : myAsyncTreeModel.isLeaf(component);
    }

    public static VirtualFile getVirtualFile(TreePath path) {
        Object component = path.getLastPathComponent();
        if (component instanceof DefaultMutableTreeNode node
            && node.getUserObject() instanceof FileNodeDescriptor descriptor) {
            return descriptor.getElement().getFile();
        }
        return component instanceof FileNode node ? node.getFile() : null;
    }

    @Override
    public boolean selectionExists() {
        TreePath[] selectedPaths = myTree.getSelectionPaths();
        return selectedPaths != null && selectedPaths.length != 0;
    }

    @Override
    public boolean isUnderRoots(@Nonnull VirtualFile file) {
        List<VirtualFile> roots = myDescriptor.getRoots();
        if (roots.size() == 0) {
            return true;
        }

        for (VirtualFile root : roots) {
            if (root != null && VfsUtilCore.isAncestor(root, file, false)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void addListener(Listener listener, Disposable parent) {
        myListeners.add(listener);
        Disposer.register(parent, () -> myListeners.remove(listener));
    }

    private void fireSelection(@Nonnull List<? extends VirtualFile> selection) {
        for (Listener each : myListeners) {
            each.selectionChanged(selection);
        }
    }

    private void processSelectionChange() {
        if (myListeners.size() == 0) {
            return;
        }
        List<VirtualFile> selection = new ArrayList<>();

        TreePath[] paths = myTree.getSelectionPaths();
        if (paths != null) {
            for (TreePath each : paths) {
                VirtualFile file = getVirtualFile(each);
                if (file != null) {
                    selection.add(file);
                }
            }
        }

        fireSelection(selection);
    }
}
