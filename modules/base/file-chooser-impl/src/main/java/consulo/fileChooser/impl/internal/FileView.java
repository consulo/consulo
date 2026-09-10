// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.fileChooser.impl.internal;

import consulo.disposer.Disposable;
import consulo.fileChooser.FileChooserDescriptor;
import consulo.fileChooser.localize.FileChooserLocalize;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.MessageBoxes;
import consulo.ui.Component;
import consulo.ui.UIAccess;
import consulo.ui.ex.tree.ApplicationTreeExecutorFactory;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.LoadingLayout;
import consulo.ui.layout.ScrollableLayout;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

class FileView {
    private static final Logger LOG = Logger.getInstance(FileView.class);

    private final UniversalFileChooserContributor myContributor;
    private final FileChooserDescriptor myDescriptor;
    private final Project myProject;
    private final Runnable myOkEnabledUpdater;
    private final boolean myEnvironmentRestricted;
    private final boolean myHasExtensionFilter;
    private final boolean myChooseFiles;
    private final boolean myChooseFolders;
    private final List<String> myRoots = new ArrayList<>();

    private final NioFileSystemTree myFileTree;
    private final NioPathTextField myPathTextField;
    private final Component myTreeComponent;
    private final LoadingLayout<DockLayout> myContent;
    private final DockLayout myTopComponent;

    private @Nullable Path myFileToSelect;

    FileView(
        UniversalFileChooserContributor contributor,
        FileChooserDescriptor descriptor,
        Project project,
        Runnable okAction,
        Runnable okEnabledUpdater,
        boolean restrictRootsToProjectEnvironment,
        ApplicationTreeExecutorFactory treeExecutorFactory,
        Disposable disposable
    ) {
        myContributor = contributor;
        myDescriptor = descriptor;
        myProject = project;
        myOkEnabledUpdater = okEnabledUpdater;
        myEnvironmentRestricted = restrictRootsToProjectEnvironment;
        myHasExtensionFilter = descriptor.getExtensionFilter() != null;
        myChooseFiles = descriptor.isChooseFiles() || descriptor.isChooseJarContents();
        myChooseFolders = descriptor.isChooseFolders();

        myFileTree = new NioFileSystemTree(descriptor, contributor, treeExecutorFactory, disposable);
        myFileTree.addOkAction(okAction);
        myFileTree.addListener(this::onSelectionChanged);
        myFileTree.setOkEnabledSupplier(this::isOkEnabled);

        myPathTextField = new NioPathTextField(descriptor.isChooseFiles(), descriptor.isChooseJarContents());
        myPathTextField.setShowHiddenSupplier(myFileTree::areHiddensShown);
        myPathTextField.setPathParser(contributor::parsePresentablePath);

        myTreeComponent = ScrollableLayout.create(myFileTree.getTree());
        myContent = LoadingLayout.create(DockLayout.create(), disposable);

        DockLayout pathHolder = DockLayout.create();
        pathHolder.center(myPathTextField.getComponent());

        myTopComponent = DockLayout.create();
        myTopComponent.top(pathHolder);
        myTopComponent.center(myContent);
    }

    UniversalFileChooserContributor getContributor() {
        return myContributor;
    }

    Component getTopComponent() {
        return myTopComponent;
    }

    NioFileSystemTree getFileTree() {
        return myFileTree;
    }

    void setFileToSelect(@Nullable Path fileToSelect) {
        myFileToSelect = fileToSelect;
    }

    List<Path> getSelectedFiles() {
        return myFileTree.getSelectedFiles();
    }

    boolean isOkEnabled() {
        List<Path> selected = getSelectedFiles();
        if (selected.isEmpty()) {
            return false;
        }
        for (Path file : selected) {
            if (file.getParent() == null) {
                return false;
            }
            if (Files.isDirectory(file)) {
                if (!myChooseFolders) {
                    return false;
                }
                if (myHasExtensionFilter) {
                    return false;
                }
            }
            else if (!myChooseFiles) {
                return false;
            }
            if (!myDescriptor.isPathSelectable(file)) {
                return false;
            }
        }
        return true;
    }

    boolean canDeleteSelectedFile() {
        Path selected = myFileTree.getSelectedFile();
        if (selected == null) {
            return false;
        }
        if (myRoots.contains(selected.toString())) {
            return false;
        }
        return Files.isWritable(selected);
    }

    @Nullable
    Path getNewFileParent() {
        return myFileTree.getNewFileParent();
    }

    void deleteSelectedFile() {
        Path selected = myFileTree.getSelectedFile();
        if (selected == null) {
            return;
        }

        Path fileName = selected.getFileName();
        String presentableName = fileName != null ? fileName.toString() : selected.toString();
        LocalizeValue confirmMessage = Files.isDirectory(selected) && !isEmptyDirectory(selected)
            ? FileChooserLocalize.universalFileChooserActionDeleteConfirmDirectory(presentableName)
            : FileChooserLocalize.universalFileChooserActionDeleteConfirm(presentableName);

        MessageBoxes.yesNo()
            .asWarning()
            .title(FileChooserLocalize.universalFileChooserActionDeleteText())
            .text(confirmMessage)
            .showAsync()
            .whenComplete((confirmed, t) -> {
                if (Boolean.TRUE.equals(confirmed)) {
                    deleteRecursively(selected);
                    myFileTree.updateTree();
                }
            });
    }

    private static boolean isEmptyDirectory(Path directory) {
        try (Stream<Path> children = Files.list(directory)) {
            return children.findAny().isEmpty();
        }
        catch (IOException e) {
            return false;
        }
    }

    private static void deleteRecursively(Path path) {
        try {
            if (Files.isDirectory(path) && !Files.isSymbolicLink(path)) {
                try (Stream<Path> children = Files.list(path)) {
                    children.forEach(FileView::deleteRecursively);
                }
            }
            Files.deleteIfExists(path);
        }
        catch (IOException e) {
            LOG.warn("cannot delete: " + path, e);
        }
    }

    void loadRoots() {
        LocalizeValue customLoadingText = myContributor.getCustomLoadingText();
        myContent.setLoadingText(customLoadingText == LocalizeValue.empty()
            ? FileChooserLocalize.universalFileChooserLabelLoading()
            : customLoadingText);

        UIAccess uiAccess = UIAccess.current();
        myContent.startLoading(
            () -> {
                try {
                    return rootsFuture().join();
                }
                catch (RuntimeException e) {
                    LOG.warn("cannot load roots", e);
                    return List.<UniversalFileChooserContributor.Root>of();
                }
            },
            (inner, roots) -> {
                List<UniversalFileChooserContributor.Root> resolvedRoots = roots == null ? List.of() : roots;
                myRoots.clear();
                for (UniversalFileChooserContributor.Root root : resolvedRoots) {
                    if (root.path() != null) {
                        myRoots.add(root.path().toString());
                    }
                }
                myFileTree.setRoots(resolvedRoots);
                inner.center(myTreeComponent);
                myFileTree.updateTree().whenComplete((ignored, error) -> uiAccess.give(this::selectPendingFile));
            });
    }

    void navigateToTextFieldPath() {
        String text = myPathTextField.getTextBox().getValueOrError();
        Path path = myContributor.parsePresentablePath(text);
        if (path == null) {
            return;
        }
        myFileTree.select(path);
    }

    void navigateToFile(Path file) {
        myFileTree.select(file);
    }

    private java.util.concurrent.CompletableFuture<List<UniversalFileChooserContributor.Root>> rootsFuture() {
        if (myEnvironmentRestricted && !myProject.isDefault()) {
            String basePath = myProject.getBasePath();
            if (basePath != null) {
                return myContributor.getFilteredRoots(Path.of(basePath));
            }
        }
        return myContributor.getRoots();
    }

    private void selectPendingFile() {
        if (myFileToSelect != null) {
            myFileTree.select(myFileToSelect);
        }
    }

    private void onSelectionChanged(List<Path> selection) {
        updatePathField(selection);
        myOkEnabledUpdater.run();
    }

    private void updatePathField(List<Path> selection) {
        if (selection.isEmpty()) {
            return;
        }
        myPathTextField.getTextBox().setValue(myContributor.getPresentablePath(selection.get(0)));
    }
}
