/*
 * Copyright 2013-2026 consulo.io
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
package consulo.versionControlSystem.impl.internal.change.ui;

import consulo.application.HelpManager;
import consulo.dataContext.DataSink;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.CheckBoxTreePolicy;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.TriStateCheckBoxTree;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.util.lang.ThreeState;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.ChangeListDecorator;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.versionControlSystem.change.LocalChangeList;
import consulo.versionControlSystem.change.LocallyDeletedChange;
import consulo.versionControlSystem.internal.ChangesListView;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public final class UnifiedChangesTree {
    private static final String THIN_SPACE = "  ";

    private final class Model implements TreeModel<ChangesTreeItem> {
        @Override
        public void buildChildren(Function<ChangesTreeItem, TreeNode<ChangesTreeItem>> nodeFactory, @Nullable ChangesTreeItem parentValue) {
            ChangesTreeSnapshot snapshot = mySnapshot;
            for (ChangesTreeItem item : snapshot.getChildren(parentValue)) {
                TreeNode<ChangesTreeItem> node = nodeFactory.apply(item);
                node.setLeaf(!snapshot.isGroup(item) || !snapshot.hasChildren(item));
                node.setRenderer((value, presentation) -> render(snapshot, value, presentation));
            }
        }

        @Override
        public boolean onDoubleClick(Tree<ChangesTreeItem> tree, TreeNode<ChangesTreeItem> node) {
            ChangesTreeItem value = node.getValue();
            if (value == null || mySnapshot.isGroup(value)) {
                return true;
            }

            Consumer<ChangesTreeItem> handler = myDoubleClickHandler;
            if (handler != null) {
                handler.accept(value);
                return false;
            }

            navigate(value, true);
            return false;
        }
    }

    private final Project myProject;
    private final Tree<ChangesTreeItem> myTree;
    private final boolean myCheckable;

    private ChangesTreeSnapshot mySnapshot = ChangesTreeSnapshot.EMPTY;
    private boolean myExpandedDefault;

    private final Set<FilePath> myIncludedKeys = new LinkedHashSet<>();
    private final Set<FilePath> myKnownKeys = new HashSet<>();
    private final List<Runnable> myInclusionListeners = new ArrayList<>();
    private boolean mySyncingChecks;
    private boolean myAutoIncludeDefaultList = true;
    private @Nullable Consumer<ChangesTreeItem> myDoubleClickHandler;
    private @Nullable BiConsumer<Change, TextItemPresentation> myChangeDecorator;
    private Predicate<Change> myIncludableFilter = change -> true;

    @RequiredUIAccess
    public UnifiedChangesTree(Project project, boolean checkable, Disposable parent) {
        myProject = project;
        myCheckable = checkable;

        Model model = new Model();
        myTree = checkable ? TriStateCheckBoxTree.create(model) : Tree.create(model);
        myTree.setAllowMultipleSelect(true);
        myTree.setSpeedSearchConverter(node -> {
            ChangesTreeItem item = node.getValue();
            return item == null ? "" : speedSearchText(item);
        });
        Disposer.register(parent, myTree.destroyHook());

        if (myTree instanceof TriStateCheckBoxTree<ChangesTreeItem> checkBoxTree) {
            checkBoxTree.setCheckPolicy(CheckBoxTreePolicy.INDEPENDENT);
            checkBoxTree.setCheckableFilter(this::isCheckable);
            checkBoxTree.addCheckListener(event -> {
                if (!mySyncingChecks) {
                    setIncluded(event.getValue(), event.isChecked());
                }
            });
            myTree.addExpandListener(event -> syncChecks());
        }
    }

    public boolean isCheckable() {
        return myCheckable;
    }

    public void setDoubleClickHandler(@Nullable Consumer<ChangesTreeItem> handler) {
        myDoubleClickHandler = handler;
    }

    /**
     * Appends to the presentation of every change, after its name.
     */
    public void setChangeDecorator(@Nullable BiConsumer<Change, TextItemPresentation> changeDecorator) {
        myChangeDecorator = changeDecorator;
    }

    /**
     * Changes the filter rejects get no check box and are never included. Takes effect with the next snapshot.
     */
    public void setIncludableFilter(Predicate<Change> includableFilter) {
        myIncludableFilter = includableFilter;
    }

    public void setAutoIncludeDefaultList(boolean autoIncludeDefaultList) {
        myAutoIncludeDefaultList = autoIncludeDefaultList;
    }

    @RequiredUIAccess
    public void setIncluded(Collection<?> changesAndFiles) {
        myIncludedKeys.clear();
        for (Object item : changesAndFiles) {
            FilePath key = toKey(item);
            if (key != null) {
                myIncludedKeys.add(key);
            }
        }
        myIncludedKeys.retainAll(myKnownKeys);
        syncChecks();
        fireInclusionChanged();
    }

    public void addInclusionListener(Runnable listener) {
        myInclusionListeners.add(listener);
    }

    private static @Nullable FilePath inclusionKey(ChangesTreeItem item) {
        if (item instanceof ChangeTreeItem changeItem) {
            return ChangesUtil.getFilePath(changeItem.change());
        }
        if (item instanceof FileTreeItem fileItem && fileItem.kind() == ChangesTreeGroupKind.UNVERSIONED) {
            return VcsUtil.getFilePath(fileItem.file());
        }
        if (item instanceof PathTreeItem pathItem) {
            return pathItem.path();
        }
        return null;
    }

    private static @Nullable FilePath toKey(Object item) {
        if (item instanceof Change change) {
            return ChangesUtil.getFilePath(change);
        }
        if (item instanceof VirtualFile file) {
            return VcsUtil.getFilePath(file);
        }
        return item instanceof FilePath path ? path : null;
    }

    private @Nullable FilePath includableKey(ChangesTreeItem item) {
        if (item instanceof ChangeTreeItem changeItem && !myIncludableFilter.test(changeItem.change())) {
            return null;
        }
        return inclusionKey(item);
    }

    private boolean isCheckable(ChangesTreeItem item) {
        if (includableKey(item) != null) {
            return true;
        }
        for (ChangesTreeItem leaf : mySnapshot.collectLeaves(item)) {
            if (includableKey(leaf) != null) {
                return true;
            }
        }
        return false;
    }

    private ThreeState inclusionState(ChangesTreeItem item) {
        boolean anyIncluded = false;
        boolean anyExcluded = false;
        for (ChangesTreeItem leaf : mySnapshot.collectLeaves(item)) {
            FilePath key = includableKey(leaf);
            if (key == null) {
                continue;
            }
            if (myIncludedKeys.contains(key)) {
                anyIncluded = true;
            }
            else {
                anyExcluded = true;
            }
        }
        if (anyIncluded && anyExcluded) {
            return ThreeState.UNSURE;
        }
        return anyIncluded ? ThreeState.YES : ThreeState.NO;
    }

    @RequiredUIAccess
    private void setIncluded(ChangesTreeItem item, boolean included) {
        for (ChangesTreeItem leaf : mySnapshot.collectLeaves(item)) {
            FilePath key = includableKey(leaf);
            if (key == null) {
                continue;
            }
            if (included) {
                myIncludedKeys.add(key);
            }
            else {
                myIncludedKeys.remove(key);
            }
        }
        syncChecks();
        fireInclusionChanged();
    }

    @RequiredUIAccess
    public void includeChanges(Collection<Change> changes) {
        for (Change change : changes) {
            if (myIncludableFilter.test(change)) {
                myIncludedKeys.add(ChangesUtil.getFilePath(change));
            }
        }
        syncChecks();
        fireInclusionChanged();
    }

    public List<Change> getIncludedChanges() {
        List<Change> result = new ArrayList<>();
        for (ChangesTreeItem root : mySnapshot.getRoots()) {
            for (ChangesTreeItem leaf : mySnapshot.collectLeaves(root)) {
                if (leaf instanceof ChangeTreeItem changeItem && myIncludedKeys.contains(inclusionKey(leaf))) {
                    result.add(changeItem.change());
                }
            }
        }
        return result;
    }

    public List<FilePath> getIncludedPaths() {
        List<FilePath> result = new ArrayList<>();
        for (ChangesTreeItem root : mySnapshot.getRoots()) {
            for (ChangesTreeItem leaf : mySnapshot.collectLeaves(root)) {
                if (leaf instanceof PathTreeItem pathItem && myIncludedKeys.contains(pathItem.path())) {
                    result.add(pathItem.path());
                }
            }
        }
        return result;
    }

    public List<VirtualFile> getIncludedUnversionedFiles() {
        List<VirtualFile> result = new ArrayList<>();
        for (ChangesTreeItem root : mySnapshot.getRoots()) {
            for (ChangesTreeItem leaf : mySnapshot.collectLeaves(root)) {
                if (leaf instanceof FileTreeItem fileItem
                    && fileItem.kind() == ChangesTreeGroupKind.UNVERSIONED
                    && myIncludedKeys.contains(inclusionKey(leaf))) {
                    result.add(fileItem.file());
                }
            }
        }
        return result;
    }

    private void fireInclusionChanged() {
        for (Runnable listener : myInclusionListeners) {
            listener.run();
        }
    }

    private void updateInclusion(ChangesTreeSnapshot snapshot) {
        Set<FilePath> present = new HashSet<>();
        for (ChangesTreeItem root : snapshot.getRoots()) {
            boolean defaultList = root instanceof ChangeListTreeItem changeList && changeList.isDefault();
            for (ChangesTreeItem leaf : snapshot.collectLeaves(root)) {
                FilePath key = includableKey(leaf);
                if (key == null) {
                    continue;
                }
                present.add(key);
                if (myAutoIncludeDefaultList && defaultList && !myKnownKeys.contains(key)) {
                    myIncludedKeys.add(key);
                }
            }
        }
        myIncludedKeys.retainAll(present);
        myKnownKeys.clear();
        myKnownKeys.addAll(present);
    }

    @RequiredUIAccess
    private void syncChecks() {
        if (!(myTree instanceof TriStateCheckBoxTree<ChangesTreeItem> checkBoxTree)) {
            return;
        }

        mySyncingChecks = true;
        try {
            syncChecks(checkBoxTree, null);
        }
        finally {
            mySyncingChecks = false;
        }
    }

    @RequiredUIAccess
    private void syncChecks(TriStateCheckBoxTree<ChangesTreeItem> tree, @Nullable ChangesTreeItem parent) {
        for (ChangesTreeItem child : mySnapshot.getChildren(parent)) {
            if (isCheckable(child)) {
                ThreeState state = inclusionState(child);
                if (tree.getOwnCheckState(child) != state) {
                    tree.setCheckState(child, state);
                }
            }
            if (mySnapshot.isGroup(child)) {
                syncChecks(tree, child);
            }
        }
    }

    public Tree<ChangesTreeItem> getComponent() {
        return myTree;
    }

    public ChangesTreeSnapshot getSnapshot() {
        return mySnapshot;
    }

    @RequiredUIAccess
    public CompletableFuture<?> setSnapshot(ChangesTreeSnapshot snapshot) {
        return setSnapshot(snapshot, collectExpandedPaths(), valuesOf(myTree.getSelectedPath()));
    }

    @RequiredUIAccess
    public CompletableFuture<?> takeOver(UnifiedChangesTree previous) {
        myExpandedDefault = previous.myExpandedDefault;
        return setSnapshot(previous.mySnapshot, previous.collectExpandedPaths(), valuesOf(previous.myTree.getSelectedPath()));
    }

    @RequiredUIAccess
    private CompletableFuture<?> setSnapshot(ChangesTreeSnapshot snapshot,
                                             List<List<ChangesTreeItem>> expandedPaths,
                                             List<ChangesTreeItem> selectedPath) {
        mySnapshot = snapshot;
        if (myCheckable) {
            updateInclusion(snapshot);
        }

        UIAccess uiAccess = UIAccess.current();
        return myTree.refreshAll().thenRun(() -> uiAccess.give(() -> {
            if (!myExpandedDefault) {
                for (ChangesTreeItem root : snapshot.getRoots()) {
                    if (root instanceof ChangeListTreeItem changeList && changeList.isDefault() && snapshot.hasChildren(root)) {
                        myExpandedDefault = true;
                        expandPath(List.of(root), false);
                    }
                }
            }

            for (List<ChangesTreeItem> path : expandedPaths) {
                expandPath(path, false);
            }

            if (!selectedPath.isEmpty()) {
                expandPath(selectedPath, true);
            }

            syncChecks();
            fireInclusionChanged();
        }));
    }

    @RequiredUIAccess
    public void selectPath(List<ChangesTreeItem> path) {
        expandPath(path, true);
    }

    @RequiredUIAccess
    private List<List<ChangesTreeItem>> collectExpandedPaths() {
        List<List<ChangesTreeItem>> result = new ArrayList<>();
        for (List<TreeNode<ChangesTreeItem>> path : myTree.getExpandedPaths()) {
            List<ChangesTreeItem> values = valuesOf(path);
            if (!values.isEmpty()) {
                result.add(values);
            }
        }
        return result;
    }

    private static List<ChangesTreeItem> valuesOf(List<TreeNode<ChangesTreeItem>> path) {
        return path.stream().map(TreeNode::getValue).filter(Objects::nonNull).toList();
    }

    @RequiredUIAccess
    private void expandPath(List<ChangesTreeItem> path, boolean select) {
        TreeNode<ChangesTreeItem> root = myTree.getRootNode();
        if (root == null || path.isEmpty()) {
            return;
        }
        UIAccess uiAccess = UIAccess.current();
        expandStep(uiAccess, root, path, 0, select);
    }

    private void expandStep(UIAccess uiAccess, TreeNode<ChangesTreeItem> parent, List<ChangesTreeItem> path, int index, boolean select) {
        ChangesTreeItem item = path.get(index);
        parent.findChild(item::equals).whenComplete((child, error) -> {
            if (child == null) {
                return;
            }

            uiAccess.give(() -> {
                if (index == path.size() - 1) {
                    if (select) {
                        myTree.select(child);
                    }
                    else {
                        myTree.expand(child).whenComplete((o, e) -> uiAccess.give(this::syncChecks));
                    }
                    return;
                }
                myTree.expand(child).whenComplete((o, e) -> expandStep(uiAccess, child, path, index + 1, select));
            });
        });
    }

    public List<ChangesTreeItem> getSelectedItems() {
        return valuesOf(myTree.getSelectedNodes());
    }

    public boolean isGroupSelected() {
        ChangesTreeSnapshot snapshot = mySnapshot;
        for (ChangesTreeItem item : getSelectedItems()) {
            if (snapshot.isGroup(item)) {
                return true;
            }
        }
        return false;
    }

    public @Nullable List<ChangesTreeItem> findPath(Change change) {
        List<ChangesTreeItem> path = new ArrayList<>();
        return findPath(mySnapshot, null, change, path) ? path : null;
    }

    private static boolean findPath(ChangesTreeSnapshot snapshot, @Nullable ChangesTreeItem parent, Change change, List<ChangesTreeItem> path) {
        for (ChangesTreeItem item : snapshot.getChildren(parent)) {
            path.add(item);
            if (item instanceof ChangeTreeItem changeItem && changeItem.change().equals(change)) {
                return true;
            }
            if (snapshot.isGroup(item) && findPath(snapshot, item, change, path)) {
                return true;
            }
            path.remove(path.size() - 1);
        }
        return false;
    }

    @RequiredUIAccess
    public void selectFirst() {
        List<ChangesTreeItem> roots = mySnapshot.getRoots();
        if (!roots.isEmpty()) {
            selectPath(List.of(roots.get(0)));
        }
    }

    public List<Change> getSelectedChanges() {
        Set<Change> result = new LinkedHashSet<>();
        for (ChangesTreeItem item : getSelectedItems()) {
            for (ChangesTreeItem leaf : mySnapshot.collectLeaves(item)) {
                if (leaf instanceof ChangeTreeItem changeItem) {
                    result.add(changeItem.change());
                }
            }
        }
        return List.copyOf(result);
    }

    public List<Change> getAllChanges() {
        List<Change> result = new ArrayList<>();
        for (ChangesTreeItem root : mySnapshot.getRoots()) {
            collectChanges(root, result);
        }
        return result;
    }

    public List<Change> getChangesInList() {
        List<ChangesTreeItem> selectedPath = valuesOf(myTree.getSelectedPath());
        if (!selectedPath.isEmpty() && selectedPath.get(0) instanceof ChangeListTreeItem changeList) {
            List<Change> result = new ArrayList<>();
            collectChanges(changeList, result);
            return result;
        }
        return getAllChanges();
    }

    private void collectChanges(ChangesTreeItem item, List<Change> result) {
        for (ChangesTreeItem leaf : mySnapshot.collectLeaves(item)) {
            if (leaf instanceof ChangeTreeItem changeItem) {
                result.add(changeItem.change());
            }
        }
    }

    public List<Change> getLeadSelectionChanges() {
        List<Change> result = new ArrayList<>();
        for (ChangesTreeItem item : getSelectedItems()) {
            if (item instanceof ChangeTreeItem changeItem) {
                result.add(changeItem.change());
            }
        }
        return result;
    }

    public List<ChangeList> getSelectedChangeLists() {
        List<ChangeList> result = new ArrayList<>();
        for (ChangesTreeItem item : getSelectedItems()) {
            if (item instanceof ChangeListTreeItem changeListItem) {
                result.add(changeListItem.changeList());
            }
        }
        return result;
    }

    public List<VirtualFile> getSelectedFiles(ChangesTreeGroupKind kind) {
        Set<VirtualFile> result = new LinkedHashSet<>();
        for (ChangesTreeItem item : getSelectedItems()) {
            for (ChangesTreeItem leaf : mySnapshot.collectLeaves(item)) {
                if (leaf instanceof FileTreeItem fileItem && fileItem.kind() == kind) {
                    result.add(fileItem.file());
                }
            }
        }
        return List.copyOf(result);
    }

    public List<LocallyDeletedChange> getSelectedLocallyDeleted() {
        Set<LocallyDeletedChange> result = new LinkedHashSet<>();
        for (ChangesTreeItem item : getSelectedItems()) {
            for (ChangesTreeItem leaf : mySnapshot.collectLeaves(item)) {
                if (leaf instanceof MissingFileTreeItem missing) {
                    result.add(missing.change());
                }
            }
        }
        return List.copyOf(result);
    }

    public List<VirtualFile> getSelectedVirtualFiles() {
        Set<VirtualFile> result = new LinkedHashSet<>();
        for (ChangesTreeItem item : getSelectedItems()) {
            for (ChangesTreeItem leaf : mySnapshot.collectLeaves(item)) {
                VirtualFile file = virtualFileOf(leaf);
                if (file != null && file.isValid()) {
                    result.add(file);
                }
            }
        }
        return List.copyOf(result);
    }

    private static @Nullable VirtualFile virtualFileOf(ChangesTreeItem item) {
        if (item instanceof ChangeTreeItem changeItem) {
            return ChangesUtil.getFilePath(changeItem.change()).getVirtualFile();
        }
        if (item instanceof FileTreeItem fileItem) {
            return fileItem.file();
        }
        if (item instanceof PathTreeItem pathItem) {
            return pathItem.path().getVirtualFile();
        }
        return null;
    }

    public void navigate(ChangesTreeItem item, boolean requestFocus) {
        Navigatable navigatable = navigatableOf(item);
        if (navigatable != null && navigatable.canNavigate()) {
            navigatable.navigate(requestFocus);
        }
    }

    private @Nullable Navigatable navigatableOf(ChangesTreeItem item) {
        VirtualFile file = virtualFileOf(item);
        if (file == null || !file.isValid() || file.isDirectory()) {
            return null;
        }
        return OpenFileDescriptorFactory.getInstance(myProject).newBuilder(file).build();
    }

    public void uiDataSnapshot(DataSink sink) {
        List<Change> selectedChanges = getSelectedChanges();
        sink.set(VcsDataKeys.CHANGES, selectedChanges.toArray(Change[]::new));
        sink.set(VcsDataKeys.CHANGE_LEAD_SELECTION, getLeadSelectionChanges().toArray(Change[]::new));
        sink.set(VcsDataKeys.CHANGE_LISTS, getSelectedChangeLists().toArray(ChangeList[]::new));
        sink.set(VcsDataKeys.HAVE_SELECTED_CHANGES, !selectedChanges.isEmpty());

        List<VirtualFile> selectedFiles = getSelectedVirtualFiles();
        sink.set(VirtualFile.KEY_OF_ARRAY, selectedFiles.toArray(VirtualFile[]::new));

        List<ChangesTreeItem> selectedItems = getSelectedItems();
        if (selectedItems.size() == 1) {
            Navigatable navigatable = navigatableOf(selectedItems.get(0));
            if (navigatable != null) {
                sink.set(Navigatable.KEY, navigatable);
            }
        }
        sink.lazy(Navigatable.KEY_OF_ARRAY, () -> ChangesUtil.getNavigatableArray(myProject, selectedFiles.stream()));

        sink.set(ChangesListView.UNVERSIONED_FILES_DATA_KEY, getSelectedFiles(ChangesTreeGroupKind.UNVERSIONED));
        sink.set(ChangesListView.IGNORED_FILES_DATA_KEY, getSelectedFiles(ChangesTreeGroupKind.IGNORED));

        List<VirtualFile> modifiedWithoutEditing = getSelectedFiles(ChangesTreeGroupKind.MODIFIED_WITHOUT_EDITING);
        sink.set(VcsDataKeys.MODIFIED_WITHOUT_EDITING_DATA_KEY, modifiedWithoutEditing);
        sink.set(VcsDataKeys.HAVE_MODIFIED_WITHOUT_EDITING, !modifiedWithoutEditing.isEmpty());

        List<LocallyDeletedChange> locallyDeleted = getSelectedLocallyDeleted();
        sink.set(ChangesListView.LOCALLY_DELETED_CHANGES, locallyDeleted);
        List<FilePath> missingFiles = locallyDeleted.stream().map(LocallyDeletedChange::getPath).toList();
        sink.set(ChangesListView.MISSING_FILES_DATA_KEY, missingFiles);
        sink.set(VcsDataKeys.HAVE_LOCALLY_DELETED, !missingFiles.isEmpty());

        sink.set(HelpManager.HELP_ID, ChangesListView.HELP_ID);

        if (!selectedItems.isEmpty()) {
            sink.set(VcsDataKeys.CHANGES_IN_LIST_KEY, getChangesInList());
        }
    }

    private void render(ChangesTreeSnapshot snapshot, ChangesTreeItem item, TextItemPresentation presentation) {
        switch (item) {
            case ChangeListTreeItem changeList -> {
                presentation.append(
                    LocalizeValue.of(changeList.changeList().getName()),
                    changeList.isDefault() ? TextAttribute.REGULAR_BOLD : TextAttribute.REGULAR
                );
                appendCount(snapshot, item, presentation);
                if (changeList.changeList() instanceof LocalChangeList localChangeList) {
                    myProject.getExtensionPoint(ChangeListDecorator.class)
                        .forEach(decorator -> decorator.decorateChangeList(localChangeList, presentation, false, false, false));
                }
            }
            case FileGroupTreeItem group -> {
                presentation.append(group.kind().getTitle());
                appendCount(snapshot, item, presentation);
            }
            case RepositoryTreeItem repository -> {
                presentation.append(LocalizeValue.of(repository.label()));
                appendCount(snapshot, item, presentation);
            }
            case ModuleTreeItem module -> {
                presentation.withIcon(PlatformIconGroup.nodesModule());
                presentation.append(module.module().getName());
                appendCount(snapshot, item, presentation);
            }
            case DirectoryTreeItem directory -> {
                presentation.withIcon(PlatformIconGroup.nodesTreeclosed());
                presentation.append(directory.label());
                appendCount(snapshot, item, presentation);
            }
            case ChangeTreeItem change -> {
                FilePath path = ChangesUtil.getFilePath(change.change());
                renderFile(snapshot, item, presentation, path, change.change().getFileStatus());
                BiConsumer<Change, TextItemPresentation> decorator = myChangeDecorator;
                if (decorator != null) {
                    decorator.accept(change.change(), presentation);
                }
            }
            case FileTreeItem file -> {
                renderFile(snapshot, item, presentation, VcsUtil.getFilePath(file.file()), file.kind().getFileStatus());
            }
            case PathTreeItem pathItem -> {
                renderFile(snapshot, item, presentation, pathItem.path(), FileStatus.NOT_CHANGED);
            }
            case MissingFileTreeItem missing -> {
                renderFile(snapshot, item, presentation, missing.change().getPath(), FileStatus.DELETED_FROM_FS);
            }
        }
    }

    private static String speedSearchText(ChangesTreeItem item) {
        return switch (item) {
            case ChangeListTreeItem changeList -> changeList.changeList().getName().trim();
            case FileGroupTreeItem group -> group.kind().getTitle().get();
            case RepositoryTreeItem repository -> repository.label();
            case ModuleTreeItem module -> module.module().getName();
            case DirectoryTreeItem directory -> directory.label();
            case ChangeTreeItem change -> ChangesUtil.getFilePath(change.change()).getName();
            case FileTreeItem file -> file.file().getName();
            case PathTreeItem pathItem -> pathItem.path().getName();
            case MissingFileTreeItem missing -> missing.change().getPath().getName();
        };
    }

    private static void renderFile(ChangesTreeSnapshot snapshot,
                                   ChangesTreeItem item,
                                   TextItemPresentation presentation,
                                   FilePath path,
                                   FileStatus status) {
        presentation.withIcon(snapshot.getIcon(item));
        presentation.append(LocalizeValue.of(path.getName()), new TextAttribute(TextAttribute.STYLE_PLAIN, status.getColor()));

        String location = snapshot.getLocation(item);
        if (location != null) {
            presentation.append(LocalizeValue.of(THIN_SPACE + location), TextAttribute.GRAYED);
        }
    }

    private static void appendCount(ChangesTreeSnapshot snapshot, ChangesTreeItem item, TextItemPresentation presentation) {
        int count = snapshot.getFileCount(item);
        if (count > 0) {
            presentation.append(LocalizeValue.of(THIN_SPACE), TextAttribute.GRAYED);
            presentation.append(VcsLocalize.changesNodeTitleChangeCount(count), TextAttribute.GRAYED);
        }
    }
}
