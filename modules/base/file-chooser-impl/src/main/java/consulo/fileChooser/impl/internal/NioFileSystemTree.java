// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileChooser.impl.internal;

import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.ui.Tree;
import consulo.ui.TreeNode;
import consulo.util.lang.StringUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

class NioFileSystemTree implements Disposable {
    interface Listener {
        void selectionChanged(List<Path> selection);
    }

    private final FileChooserDescriptor myDescriptor;
    private final UniversalFileChooserContributor myContributor;
    private final NioFileTreeModel myModel;
    private final Tree<NioFileNode> myTree;

    private final List<Listener> myListeners = new CopyOnWriteArrayList<>();
    private final List<Runnable> myOkActions = new CopyOnWriteArrayList<>();

    private BooleanSupplier myOkEnabledSupplier = () -> false;

    NioFileSystemTree(FileChooserDescriptor descriptor, UniversalFileChooserContributor contributor, Disposable disposable) {
        myDescriptor = descriptor;
        myContributor = contributor;
        myModel = new NioFileTreeModel(descriptor);
        myTree = Tree.create(myModel, disposable);

        myTree.setSpeedSearchConverter(node -> {
            NioFileNode value = node.getValue();
            String name = value == null ? null : value.getName();
            return name == null ? "" : name;
        });

        myTree.addSelectListener(event -> fireSelectionChanged());
    }

    Tree<NioFileNode> getTree() {
        return myTree;
    }

    UniversalFileChooserContributor getContributor() {
        return myContributor;
    }

    void addListener(Listener listener) {
        myListeners.add(listener);
    }

    void addOkAction(Runnable okAction) {
        myOkActions.add(okAction);
    }

    void setOkEnabledSupplier(BooleanSupplier okEnabledSupplier) {
        myOkEnabledSupplier = okEnabledSupplier;
    }

    void setRoots(List<UniversalFileChooserContributor.Root> roots) {
        myModel.setContributorRoots(roots);
    }

    CompletableFuture<?> updateTree() {
        return myTree.refreshAll();
    }

    boolean areHiddensShown() {
        return myDescriptor.isShowHiddenFiles();
    }

    void showHiddens(boolean showHidden) {
        myDescriptor.withShowHiddenFiles(showHidden);
    }

    UniversalFileChooserContributor.@Nullable Root getVirtualRoot(NioFileNode node) {
        return myModel.getVirtualRoot(node);
    }

    @Nullable
    Path getSelectedFile() {
        TreeNode<NioFileNode> node = myTree.getSelectedNode();
        NioFileNode value = node == null ? null : node.getValue();
        return value == null ? null : value.getPath();
    }

    List<Path> getSelectedFiles() {
        Path selected = getSelectedFile();
        return selected == null ? List.of() : List.of(selected);
    }

    @Nullable
    Path getNewFileParent() {
        Path selected = getSelectedFile();
        if (selected != null) {
            return selected;
        }
        List<VirtualFile> descriptorRoots = myDescriptor.getRoots();
        if (descriptorRoots.size() == 1) {
            return NioFileChooserUtil.toNioPathSafe(descriptorRoots.get(0));
        }
        return null;
    }

    @Nullable
    Exception createNewFolder(Path parentPath, String newFolderName) {
        try {
            for (String name : StringUtil.tokenize(newFolderName, "\\/")) {
                Path folderPath = parentPath.resolve(name);
                Files.createDirectories(folderPath);
                updateTree();
                select(folderPath);
            }
            return null;
        }
        catch (IOException e) {
            return e;
        }
    }

    CompletableFuture<@Nullable TreeNode<NioFileNode>> select(Path path) {
        TreeNode<NioFileNode> root = myTree.getRootNode();
        if (root == null) {
            return CompletableFuture.completedFuture(null);
        }
        return descend(root, path.toAbsolutePath().normalize()).thenApply(node -> {
            if (node != null) {
                myTree.select(node);
            }
            return node;
        });
    }

    private CompletableFuture<@Nullable TreeNode<NioFileNode>> descend(TreeNode<NioFileNode> node, Path target) {
        NioFileNode value = node.getValue();
        Path current = value == null ? null : value.getPath();
        if (current != null && current.equals(target)) {
            return CompletableFuture.completedFuture(node);
        }
        return node.findChild(child -> {
            Path childPath = child.getPath();
            return childPath != null && target.startsWith(childPath);
        }).thenCompose(child -> {
            if (child == null) {
                return CompletableFuture.completedFuture(current == null ? null : node);
            }
            return descend(child, target);
        });
    }

    private void fireSelectionChanged() {
        List<Path> selection = new ArrayList<>(getSelectedFiles());
        for (Listener listener : myListeners) {
            listener.selectionChanged(selection);
        }
    }

    @Override
    public void dispose() {
        myListeners.clear();
        myOkActions.clear();
    }
}
