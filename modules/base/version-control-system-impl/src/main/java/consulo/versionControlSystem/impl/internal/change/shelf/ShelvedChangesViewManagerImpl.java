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
package consulo.versionControlSystem.impl.internal.change.shelf;

import consulo.annotation.component.ServiceImpl;
import consulo.application.util.DateFormatUtil;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.file.FileTypeManager;
import consulo.localize.LocalizeValue;
import consulo.navigation.Navigatable;
import consulo.navigation.OpenFileDescriptorFactory;
import consulo.project.Project;
import consulo.project.ui.wm.ToolWindowManager;
import consulo.ui.TextAttribute;
import consulo.ui.TextItemPresentation;
import consulo.ui.Tree;
import consulo.ui.TreeModel;
import consulo.ui.TreeNode;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionPopupMenu;
import consulo.ui.ex.content.Content;
import consulo.ui.ex.content.ContentFactory;
import consulo.ui.ex.toolWindow.ToolWindow;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.ScrollableLayout;
import consulo.ui.style.ComponentColors;
import consulo.util.dataholder.Key;
import consulo.versionControlSystem.VcsDataKeys;
import consulo.versionControlSystem.VcsToolWindow;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.shelf.ShelveChangesListener;
import consulo.versionControlSystem.change.shelf.ShelveChangesManager;
import consulo.versionControlSystem.change.shelf.ShelvedChangeList;
import consulo.versionControlSystem.change.shelf.ShelvedChangesViewManager;
import consulo.versionControlSystem.impl.internal.patch.PatchFileType;
import consulo.versionControlSystem.internal.ChangesViewContentI;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.status.FileStatus;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

/**
 * The Shelf tab of the Version Control tool window: the shelved change lists, and the recycled ones on request.
 *
 * @author yole
 * @since 2006-11-23
 */
@Singleton
@ServiceImpl
public class ShelvedChangesViewManagerImpl implements ShelvedChangesViewManager, Disposable {
    public static final Key<ShelvedChangeListImpl[]> SHELVED_CHANGELIST_KEY = Key.create("ShelveChangesManager.ShelvedChangeListData");
    public static final Key<ShelvedChangeListImpl[]> SHELVED_RECYCLED_CHANGELIST_KEY =
        Key.create("ShelveChangesManager.ShelvedRecycledChangeListData");
    public static final Key<List<ShelvedChangeImpl>> SHELVED_CHANGE_KEY = Key.create("ShelveChangesManager.ShelvedChange");
    public static final Key<List<ShelvedBinaryFileImpl>> SHELVED_BINARY_FILE_KEY = Key.create("ShelveChangesManager.ShelvedBinaryFile");

    private static final String THIN_SPACE = "  ";

    private final Project myProject;
    private final ChangesViewContentI myContentManager;
    private final ShelveChangesManagerImpl myShelveChangesManager;
    private final DeleteProvider myDeleteProvider;
    private final AtomicBoolean myRefreshQueued = new AtomicBoolean();

    private ShelfTreeSnapshot mySnapshot = ShelfTreeSnapshot.EMPTY;
    private @Nullable Tree<Object> myTree;
    private @Nullable DockLayout myRoot;
    private @Nullable Content myContent;
    private @Nullable Disposable myContentDisposable;
    private volatile boolean myUpdatePending;
    private @Nullable Runnable myPostUpdateRunnable;
    private volatile boolean myDisposed;

    @Inject
    public ShelvedChangesViewManagerImpl(Project project, ChangesViewContentI contentManager, ShelveChangesManager shelveChangesManager) {
        myProject = project;
        myContentManager = contentManager;
        myShelveChangesManager = (ShelveChangesManagerImpl) shelveChangesManager;
        myDeleteProvider = new ShelvedChangeDeleteProvider(project, myShelveChangesManager);
        myProject.getMessageBus().connect(this).subscribe(ShelveChangesListener.class, manager -> scheduleUpdate());
    }

    /**
     * Reloads the shelf off the UI thread and shows it.
     */
    public void scheduleUpdate() {
        myUpdatePending = true;
        if (myDisposed || !myRefreshQueued.compareAndSet(false, true)) {
            return;
        }

        AppExecutorUtil.getAppExecutorService().execute(() -> {
            myRefreshQueued.set(false);
            if (myDisposed || myProject.isDisposed()) {
                return;
            }
            ShelfTreeSnapshot snapshot = ShelfTreeSnapshot.build(myProject, myShelveChangesManager);
            myProject.getApplication().invokeLater(() -> applySnapshot(snapshot), myProject.getDisposed());
        });
    }

    @RequiredUIAccess
    private void applySnapshot(ShelfTreeSnapshot snapshot) {
        if (myDisposed) {
            return;
        }
        myUpdatePending = false;
        if (!snapshot.hasLists()) {
            mySnapshot = snapshot;
            removeContent();
            myPostUpdateRunnable = null;
            return;
        }

        Tree<Object> tree = ensureContent();
        List<List<Object>> expandedPaths = collectExpandedPaths(tree);
        List<Object> selectedPath = valuesOf(tree.getSelectedPath());
        mySnapshot = snapshot;

        UIAccess uiAccess = UIAccess.current();
        tree.refreshAll().whenCompleteAsync((result, error) -> {
            for (List<Object> path : expandedPaths) {
                expandPath(tree, path, false);
            }
            if (!selectedPath.isEmpty()) {
                expandPath(tree, selectedPath, true);
            }
            Runnable postUpdate = myPostUpdateRunnable;
            myPostUpdateRunnable = null;
            if (postUpdate != null) {
                postUpdate.run();
            }
        }, uiAccess);
    }

    @RequiredUIAccess
    private Tree<Object> ensureContent() {
        Tree<Object> existing = myTree;
        if (existing != null && myContent != null) {
            return existing;
        }

        Disposable contentDisposable = Disposable.newDisposable("Shelf content");
        Disposer.register(this, contentDisposable);
        myContentDisposable = contentDisposable;

        Tree<Object> tree = Tree.create(new ShelfTreeModel());
        tree.setAllowMultipleSelect(true);
        tree.setSpeedSearchConverter(node -> speedSearchText(node.getValue()));
        Disposer.register(contentDisposable, tree.destroyHook());
        myTree = tree;

        DockLayout root = DockLayout.create();
        root.putUserData(UiDataProvider.KEY, this::uiDataSnapshot);
        root.center(ScrollableLayout.create(tree));
        myRoot = root;

        tree.addContextMenuListener(event -> {
            ActionManager actionManager = ActionManager.getInstance();
            if (!(actionManager.getAction("ShelvedChangesPopupMenu") instanceof ActionGroup group)) {
                return;
            }
            ActionPopupMenu menu = actionManager.createActionPopupMenu(ActionPlaces.UNKNOWN, group);
            menu.setTargetComponent(root);
            menu.show(event.getComponent(), event.getInputDetails().getX(), event.getInputDetails().getY());
        });

        Content content = ContentFactory.getInstance().createUIContent(root, VcsLocalize.shelfTab().get(), false);
        content.setCloseable(false);
        content.putUserData(ChangesViewContentI.CONTENT_ID_KEY, ChangesViewContentI.SHELF);
        myContentManager.addContent(content);
        myContent = content;
        return tree;
    }

    @RequiredUIAccess
    private void removeContent() {
        Content content = myContent;
        if (content != null) {
            myContentManager.removeContent(content);
            myContentManager.selectContent(ChangesViewContentI.LOCAL_CHANGES);
        }
        Disposable contentDisposable = myContentDisposable;
        if (contentDisposable != null) {
            Disposer.dispose(contentDisposable);
        }
        myContent = null;
        myContentDisposable = null;
        myTree = null;
        myRoot = null;
    }

    @Override
    public void activateView(@Nullable ShelvedChangeList list) {
        myProject.getApplication().invokeLater(() -> {
            Runnable runnable = () -> showList(list);
            if (myUpdatePending) {
                myPostUpdateRunnable = runnable;
            }
            else {
                runnable.run();
            }
        }, myProject.getDisposed());
    }

    @RequiredUIAccess
    private void showList(@Nullable ShelvedChangeList list) {
        Tree<Object> tree = myTree;
        Content content = myContent;
        if (tree == null || content == null) {
            return;
        }
        if (list != null) {
            expandPath(tree, List.of(list), true);
        }
        myContentManager.setSelectedContent(content);
        ToolWindow window = ToolWindowManager.getInstance(myProject).getToolWindow(VcsToolWindow.ID);
        if (window != null && !window.isVisible()) {
            window.activate(null);
        }
    }

    @Override
    public void dispose() {
        myDisposed = true;
    }

    private final class ShelfTreeModel implements TreeModel<Object> {
        @Override
        public void buildChildren(Function<Object, TreeNode<Object>> nodeFactory, @Nullable Object parentValue) {
            ShelfTreeSnapshot snapshot = mySnapshot;
            if (parentValue == null) {
                for (ShelvedChangeListImpl list : snapshot.getLists()) {
                    TreeNode<Object> node = nodeFactory.apply(list);
                    node.setLeaf(snapshot.getFiles(list).isEmpty());
                    node.setRenderer((value, presentation) -> render(snapshot, value, presentation));
                }
            }
            else if (parentValue instanceof ShelvedChangeListImpl list) {
                for (Object file : snapshot.getFiles(list)) {
                    TreeNode<Object> node = nodeFactory.apply(file);
                    node.setLeaf(true);
                    node.setRenderer((value, presentation) -> render(snapshot, value, presentation));
                }
            }
        }

        @Override
        @RequiredUIAccess
        public boolean onDoubleClick(Tree<Object> tree, TreeNode<Object> node) {
            DockLayout root = myRoot;
            if (node.getValue() instanceof ShelvedChangeListImpl || root == null) {
                return true;
            }
            DiffShelvedChangesAction.showShelvedChangesDiff(DataManager.getInstance().getDataContext(root));
            return false;
        }
    }

    private static void render(ShelfTreeSnapshot snapshot, Object value, TextItemPresentation presentation) {
        if (value instanceof ShelvedChangeListImpl list) {
            presentation.withIcon(PatchFileType.INSTANCE.getIcon());
            presentation.append(
                LocalizeValue.of(list.DESCRIPTION),
                list.isRecycled() ? new TextAttribute(TextAttribute.STYLE_BOLD, ComponentColors.DISABLED_TEXT) : TextAttribute.REGULAR
            );
            presentation.append(LocalizeValue.of(THIN_SPACE), TextAttribute.GRAYED);
            presentation.append(VcsLocalize.changesNodeTitleChangeCount(snapshot.getFiles(list).size()), TextAttribute.GRAYED);
            presentation.append(LocalizeValue.of(THIN_SPACE + DateFormatUtil.formatPrettyDateTime(list.DATE)), TextAttribute.GRAYED);
        }
        else if (value instanceof ShelvedChangeImpl change) {
            renderFile(snapshot, value, change.getBeforePath(), change.getAfterPath(), change.getFileStatus(), presentation);
        }
        else if (value instanceof ShelvedBinaryFileImpl binaryFile) {
            renderFile(snapshot, value, binaryFile.BEFORE_PATH, binaryFile.AFTER_PATH, binaryFile.getFileStatus(), presentation);
        }
    }

    private static void renderFile(ShelfTreeSnapshot snapshot,
                                   Object file,
                                   @Nullable String beforePath,
                                   @Nullable String afterPath,
                                   FileStatus fileStatus,
                                   TextItemPresentation presentation) {
        String fileName = ShelfTreeSnapshot.fileName(file);
        String path = ShelfTreeSnapshot.path(file);
        String systemPath = path == null ? "" : path.replace('/', File.separatorChar);
        int index = systemPath.lastIndexOf(File.separatorChar);

        presentation.withIcon(FileTypeManager.getInstance().getFileTypeByFileName(fileName).getIcon());
        presentation.append(LocalizeValue.of(fileName), new TextAttribute(TextAttribute.STYLE_PLAIN, fileStatus.getColor()));
        String movedMessage = snapshot.getMovedMessage(beforePath, afterPath);
        if (movedMessage != null) {
            presentation.append(LocalizeValue.of(movedMessage));
        }
        LocalizeValue directory = index >= 0 ? LocalizeValue.of(systemPath.substring(0, index)) : VcsLocalize.shelfNodeProjectRoot();
        presentation.append(LocalizeValue.of(THIN_SPACE), TextAttribute.GRAYED);
        presentation.append(directory, TextAttribute.GRAYED);
    }

    private static @Nullable String speedSearchText(@Nullable Object value) {
        if (value instanceof ShelvedChangeListImpl list) {
            return list.DESCRIPTION;
        }
        return value == null ? null : ShelfTreeSnapshot.fileName(value);
    }

    @RequiredUIAccess
    private void uiDataSnapshot(DataSink sink) {
        Tree<Object> tree = myTree;
        if (tree == null) {
            return;
        }

        ShelfTreeSnapshot snapshot = mySnapshot;
        List<Object> selected = valuesOf(tree.getSelectedNodes());
        Set<ShelvedChangeListImpl> shelvedLists = new LinkedHashSet<>();
        Set<ShelvedChangeListImpl> recycledLists = new LinkedHashSet<>();
        List<ShelvedChangeListImpl> selectedLists = new ArrayList<>();
        List<ShelvedChangeImpl> selectedChanges = new ArrayList<>();
        List<ShelvedBinaryFileImpl> selectedBinaryFiles = new ArrayList<>();
        for (Object value : selected) {
            ShelvedChangeListImpl list = value instanceof ShelvedChangeListImpl selectedList ? selectedList : snapshot.getOwner(value);
            if (list != null) {
                (list.isRecycled() ? recycledLists : shelvedLists).add(list);
            }
            if (value instanceof ShelvedChangeListImpl selectedList) {
                selectedLists.add(selectedList);
            }
            else if (value instanceof ShelvedChangeImpl change) {
                selectedChanges.add(change);
            }
            else if (value instanceof ShelvedBinaryFileImpl binaryFile) {
                selectedBinaryFiles.add(binaryFile);
            }
        }

        if (!shelvedLists.isEmpty()) {
            sink.set(SHELVED_CHANGELIST_KEY, shelvedLists.toArray(ShelvedChangeListImpl[]::new));
        }
        if (!recycledLists.isEmpty()) {
            sink.set(SHELVED_RECYCLED_CHANGELIST_KEY, recycledLists.toArray(ShelvedChangeListImpl[]::new));
        }
        sink.set(SHELVED_CHANGE_KEY, selectedChanges);
        sink.set(SHELVED_BINARY_FILE_KEY, selectedBinaryFiles);
        sink.set(VcsDataKeys.HAVE_SELECTED_CHANGES, !selected.isEmpty());
        sink.set(DeleteProvider.KEY, myDeleteProvider);

        List<ShelvedChangeImpl> textChanges = new ArrayList<>(selectedChanges);
        List<ShelvedBinaryFileImpl> binaryFiles = new ArrayList<>(selectedBinaryFiles);
        if (textChanges.isEmpty() && binaryFiles.isEmpty()) {
            for (ShelvedChangeListImpl list : selectedLists) {
                for (Object file : snapshot.getFiles(list)) {
                    if (file instanceof ShelvedChangeImpl change) {
                        textChanges.add(change);
                    }
                    else if (file instanceof ShelvedBinaryFileImpl binaryFile) {
                        binaryFiles.add(binaryFile);
                    }
                }
            }
        }
        List<Change> changes = new ArrayList<>(textChanges.size() + binaryFiles.size());
        for (ShelvedChangeImpl change : textChanges) {
            changes.add(change.getChange(myProject));
        }
        for (ShelvedBinaryFileImpl binaryFile : binaryFiles) {
            changes.add(binaryFile.createChange(myProject));
        }
        sink.set(VcsDataKeys.CHANGES, changes.toArray(Change[]::new));

        List<ShelvedChangeImpl> navigatableChanges = new ArrayList<>(selectedChanges);
        for (ShelvedChangeListImpl list : selectedLists) {
            for (Object file : snapshot.getFiles(list)) {
                if (file instanceof ShelvedChangeImpl change) {
                    navigatableChanges.add(change);
                }
            }
        }
        sink.lazy(Navigatable.KEY_OF_ARRAY, () -> navigatablesOf(navigatableChanges));
    }

    private Navigatable[] navigatablesOf(List<ShelvedChangeImpl> changes) {
        List<Navigatable> navigatables = new ArrayList<>(changes.size());
        for (ShelvedChangeImpl change : changes) {
            if (change.getBeforePath() == null || FileStatus.ADDED.equals(change.getFileStatus())) {
                continue;
            }
            navigatables.add(requestFocus -> {
                VirtualFile file = change.getBeforeVFUnderProject(myProject);
                if (file != null) {
                    OpenFileDescriptorFactory.getInstance(myProject).newBuilder(file).build().navigate(requestFocus);
                }
            });
        }
        return navigatables.toArray(Navigatable[]::new);
    }

    @RequiredUIAccess
    private static List<List<Object>> collectExpandedPaths(Tree<Object> tree) {
        List<List<Object>> result = new ArrayList<>();
        for (List<TreeNode<Object>> path : tree.getExpandedPaths()) {
            List<Object> values = valuesOf(path);
            if (!values.isEmpty()) {
                result.add(values);
            }
        }
        return result;
    }

    private static List<Object> valuesOf(List<TreeNode<Object>> nodes) {
        List<Object> values = new ArrayList<>(nodes.size());
        for (TreeNode<Object> node : nodes) {
            Object value = node.getValue();
            if (value != null) {
                values.add(value);
            }
        }
        return values;
    }

    private static boolean sameValue(@Nullable Object value, Object expected) {
        if (value instanceof ShelvedChangeListImpl list && expected instanceof ShelvedChangeListImpl expectedList) {
            return list == expectedList || Objects.equals(list.PATH, expectedList.PATH);
        }
        return Objects.equals(value, expected);
    }

    @RequiredUIAccess
    private static void expandPath(Tree<Object> tree, List<Object> path, boolean select) {
        TreeNode<Object> root = tree.getRootNode();
        if (root == null || path.isEmpty()) {
            return;
        }
        expandStep(UIAccess.current(), tree, root, path, 0, select);
    }

    private static void expandStep(UIAccess uiAccess, Tree<Object> tree, TreeNode<Object> parent, List<Object> path, int index, boolean select) {
        Object expected = path.get(index);
        parent.findChild(value -> sameValue(value, expected)).whenComplete((child, error) -> {
            if (child == null) {
                return;
            }
            uiAccess.give(() -> {
                if (index == path.size() - 1) {
                    if (select) {
                        tree.select(child);
                    }
                    else {
                        tree.expand(child);
                    }
                    return;
                }
                tree.expand(child).whenComplete((result, expandError) -> expandStep(uiAccess, tree, child, path, index + 1, select));
            });
        });
    }
}
