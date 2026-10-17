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

import consulo.application.util.UserHomeFileUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.Change;
import consulo.versionControlSystem.change.ChangeList;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.versionControlSystem.change.LocallyDeletedChange;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public final class ChangesTreeSnapshotBuilder {
    private record Leaf(ChangesTreeItem item, FilePath path) {
    }

    private static final class Directory {
        private final String myName;
        private final String myPath;
        private final Map<String, Directory> myDirectories = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        private final List<Leaf> myLeaves = new ArrayList<>();

        private Directory(String name, String path) {
            myName = name;
            myPath = path;
        }
    }

    private static final Comparator<Leaf> LEAF_ORDER =
        Comparator.comparing((Leaf leaf) -> leaf.path().getName(), String.CASE_INSENSITIVE_ORDER)
            .thenComparing(leaf -> leaf.path().getPath());

    private final boolean myFlatten;
    private final @Nullable String myBasePath;

    private final List<ChangesTreeItem> myRoots = new ArrayList<>();
    private final Map<ChangesTreeItem, List<ChangesTreeItem>> myChildren = new HashMap<>();
    private final Map<ChangesTreeItem, Integer> myFileCounts = new HashMap<>();

    public ChangesTreeSnapshotBuilder(boolean flatten, @Nullable String basePath) {
        myFlatten = flatten;
        myBasePath = basePath;
    }

    public ChangesTreeSnapshotBuilder changeLists(Collection<? extends ChangeList> changeLists) {
        for (ChangeList changeList : changeLists) {
            List<Leaf> leaves = new ArrayList<>();
            for (Change change : changeList.getChanges()) {
                leaves.add(new Leaf(new ChangeTreeItem(change), ChangesUtil.getFilePath(change)));
            }
            addScope(new ChangeListTreeItem(changeList), leaves, true);
        }
        return this;
    }

    public ChangesTreeSnapshotBuilder files(ChangesTreeGroupKind kind, Collection<VirtualFile> files) {
        List<Leaf> leaves = new ArrayList<>();
        for (VirtualFile file : files) {
            leaves.add(new Leaf(new FileTreeItem(kind, file), VcsUtil.getFilePath(file)));
        }
        addScope(new FileGroupTreeItem(kind), leaves, false);
        return this;
    }

    public ChangesTreeSnapshotBuilder locallyDeleted(Collection<LocallyDeletedChange> changes) {
        List<Leaf> leaves = new ArrayList<>();
        for (LocallyDeletedChange change : changes) {
            leaves.add(new Leaf(new MissingFileTreeItem(change), change.getPath()));
        }
        addScope(new FileGroupTreeItem(ChangesTreeGroupKind.LOCALLY_DELETED), leaves, false);
        return this;
    }

    public ChangesTreeSnapshotBuilder paths(Collection<FilePath> paths) {
        List<Leaf> leaves = new ArrayList<>();
        for (FilePath path : paths) {
            leaves.add(new Leaf(new PathTreeItem(path), path));
        }
        addRootLeaves(leaves);
        return this;
    }

    public ChangesTreeSnapshotBuilder changes(Collection<Change> changes) {
        List<Leaf> leaves = new ArrayList<>();
        for (Change change : changes) {
            leaves.add(new Leaf(new ChangeTreeItem(change), ChangesUtil.getFilePath(change)));
        }
        addRootLeaves(leaves);
        return this;
    }

    private void addRootLeaves(List<Leaf> leaves) {
        if (myFlatten) {
            List<Leaf> sorted = new ArrayList<>(leaves);
            sorted.sort(LEAF_ORDER);
            for (Leaf leaf : sorted) {
                myRoots.add(leaf.item());
            }
            return;
        }

        Directory root = new Directory("", "");
        for (Leaf leaf : leaves) {
            directoryOf(root, leaf.path()).myLeaves.add(leaf);
        }
        myRoots.addAll(emitChildren(null, root).items());
    }

    public ChangesTreeSnapshot build() {
        return new ChangesTreeSnapshot(List.copyOf(myRoots), Map.copyOf(myChildren), Map.copyOf(myFileCounts), myFlatten);
    }

    private void addScope(ChangesTreeItem scope, List<Leaf> leaves, boolean keepWhenEmpty) {
        if (leaves.isEmpty() && !keepWhenEmpty) {
            return;
        }

        myRoots.add(scope);
        myFileCounts.put(scope, leaves.size());

        if (myFlatten) {
            List<Leaf> sorted = new ArrayList<>(leaves);
            sorted.sort(LEAF_ORDER);
            myChildren.put(scope, sorted.stream().map(Leaf::item).toList());
            return;
        }

        Directory root = new Directory("", "");
        for (Leaf leaf : leaves) {
            directoryOf(root, leaf.path()).myLeaves.add(leaf);
        }
        emit(scope, scope, root);
    }

    private Directory directoryOf(Directory root, FilePath path) {
        FilePath parent = path.getParentPath();
        if (parent == null) {
            return root;
        }

        String parentPath = parent.getPath();
        Directory current = root;
        String accumulated;
        String relative;
        if (myBasePath != null && (parentPath.equals(myBasePath) || parentPath.startsWith(myBasePath + "/"))) {
            accumulated = myBasePath;
            relative = parentPath.length() == myBasePath.length() ? "" : parentPath.substring(myBasePath.length() + 1);
        }
        else {
            accumulated = "";
            relative = parentPath;
        }

        if (relative.isEmpty()) {
            return current;
        }

        boolean absolute = relative.startsWith("/");
        String[] segments = (absolute ? relative.substring(1) : relative).split("/");
        for (int i = 0; i < segments.length; i++) {
            String segment = segments[i];
            if (segment.isEmpty()) {
                continue;
            }
            String name = absolute && i == 0 ? "/" + segment : segment;
            accumulated = accumulated.isEmpty() ? name : accumulated + "/" + segment;
            String directoryPath = accumulated;
            current = current.myDirectories.computeIfAbsent(name.toLowerCase(), key -> new Directory(name, directoryPath));
        }
        return current;
    }

    private record Children(List<ChangesTreeItem> items, int fileCount) {
    }

    private int emit(@Nullable ChangesTreeItem scope, ChangesTreeItem parentItem, Directory directory) {
        Children children = emitChildren(scope, directory);
        myChildren.put(parentItem, children.items());
        return children.fileCount();
    }

    private Children emitChildren(@Nullable ChangesTreeItem scope, Directory directory) {
        List<ChangesTreeItem> children = new ArrayList<>();
        int fileCount = 0;

        for (Directory child : directory.myDirectories.values()) {
            Directory compacted = child;
            StringBuilder label = new StringBuilder(child.myName);
            while (compacted.myLeaves.isEmpty() && compacted.myDirectories.size() == 1) {
                compacted = compacted.myDirectories.values().iterator().next();
                label.append('/').append(compacted.myName);
            }

            String text = label.toString();
            if (text.startsWith("/")) {
                text = Objects.requireNonNullElse(UserHomeFileUtil.getLocationRelativeToUserHome(text), text);
            }

            DirectoryTreeItem item = new DirectoryTreeItem(scope, VcsUtil.getFilePath(compacted.myPath, true), text);
            int count = emit(scope, item, compacted);
            myFileCounts.put(item, count);
            children.add(item);
            fileCount += count;
        }

        List<Leaf> leaves = new ArrayList<>(directory.myLeaves);
        leaves.sort(LEAF_ORDER);
        for (Leaf leaf : leaves) {
            children.add(leaf.item());
        }
        fileCount += leaves.size();

        return new Children(children, fileCount);
    }
}
