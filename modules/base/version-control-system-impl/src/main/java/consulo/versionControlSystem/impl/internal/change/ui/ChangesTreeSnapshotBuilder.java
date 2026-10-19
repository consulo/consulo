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
import consulo.module.Module;
import consulo.module.content.ProjectFileIndex;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.io.FileUtil;
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
import java.util.Optional;
import java.util.TreeMap;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public final class ChangesTreeSnapshotBuilder {
    private record Leaf(ChangesTreeItem item, FilePath path) {
    }

    private record ModuleRoot(Module module, String rootPath) {
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
    private final @Nullable ProjectFileIndex myFileIndex;

    private final List<ChangesTreeItem> myRoots = new ArrayList<>();
    private final Map<ChangesTreeItem, List<ChangesTreeItem>> myChildren = new HashMap<>();
    private final Map<ChangesTreeItem, Integer> myFileCounts = new HashMap<>();
    private final Map<ChangesTreeItem, String> myLocations = new HashMap<>();
    private final Map<ChangesTreeItem, Image> myIcons = new HashMap<>();
    private final Map<String, Optional<ModuleRoot>> myModuleRoots = new HashMap<>();

    public ChangesTreeSnapshotBuilder(Project project, boolean flatten) {
        myFlatten = flatten;
        myBasePath = project.getBasePath();
        myFileIndex = project.isDefault() ? null : ProjectFileIndex.getInstance(project);
    }

    public ChangesTreeSnapshotBuilder changeLists(Collection<? extends ChangeList> changeLists) {
        for (ChangeList changeList : changeLists) {
            List<Leaf> leaves = new ArrayList<>();
            for (Change change : changeList.getChanges()) {
                leaves.add(leaf(new ChangeTreeItem(change), ChangesUtil.getFilePath(change)));
            }
            addScope(new ChangeListTreeItem(changeList), leaves, true);
        }
        return this;
    }

    public ChangesTreeSnapshotBuilder files(ChangesTreeGroupKind kind, Collection<VirtualFile> files) {
        List<Leaf> leaves = new ArrayList<>();
        for (VirtualFile file : files) {
            leaves.add(leaf(new FileTreeItem(kind, file), VcsUtil.getFilePath(file)));
        }
        addScope(new FileGroupTreeItem(kind), leaves, false);
        return this;
    }

    public ChangesTreeSnapshotBuilder locallyDeleted(Collection<LocallyDeletedChange> changes) {
        List<Leaf> leaves = new ArrayList<>();
        for (LocallyDeletedChange change : changes) {
            leaves.add(leaf(new MissingFileTreeItem(change), change.getPath()));
        }
        addScope(new FileGroupTreeItem(ChangesTreeGroupKind.LOCALLY_DELETED), leaves, false);
        return this;
    }

    public ChangesTreeSnapshotBuilder paths(Collection<FilePath> paths) {
        List<Leaf> leaves = new ArrayList<>();
        for (FilePath path : paths) {
            leaves.add(leaf(new PathTreeItem(path), path));
        }
        addRootLeaves(leaves);
        return this;
    }

    public ChangesTreeSnapshotBuilder changes(Collection<Change> changes) {
        List<Leaf> leaves = new ArrayList<>();
        for (Change change : changes) {
            leaves.add(leaf(new ChangeTreeItem(change), ChangesUtil.getFilePath(change)));
        }
        addRootLeaves(leaves);
        return this;
    }

    private void addRootLeaves(List<Leaf> leaves) {
        if (myFlatten) {
            List<Leaf> sorted = new ArrayList<>(leaves);
            sorted.sort(LEAF_ORDER);
            for (Leaf leaf : sorted) {
                putLocation(leaf);
                myRoots.add(leaf.item());
            }
            return;
        }

        myRoots.addAll(groupChildren(null, leaves).items());
    }

    public ChangesTreeSnapshot build() {
        return new ChangesTreeSnapshot(
            List.copyOf(myRoots),
            Map.copyOf(myChildren),
            Map.copyOf(myFileCounts),
            Map.copyOf(myLocations),
            Map.copyOf(myIcons),
            myFlatten
        );
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
            sorted.forEach(this::putLocation);
            myChildren.put(scope, sorted.stream().map(Leaf::item).toList());
            return;
        }

        myChildren.put(scope, groupChildren(scope, leaves).items());
    }

    private Children groupChildren(@Nullable ChangesTreeItem scope, List<Leaf> leaves) {
        Map<Module, Map<String, Directory>> modules = new TreeMap<>(
            Comparator.comparing(Module::getName, String.CASE_INSENSITIVE_ORDER).thenComparing(Module::getName)
        );
        Directory outside = new Directory("", "");
        for (Leaf leaf : leaves) {
            ModuleRoot moduleRoot = moduleRootOf(leaf.path());
            if (moduleRoot == null) {
                directoryOf(outside, leaf.path(), myBasePath).myLeaves.add(leaf);
                continue;
            }

            String rootPath = moduleRoot.rootPath();
            Directory root = modules.computeIfAbsent(moduleRoot.module(), module -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER))
                .computeIfAbsent(rootPath, path -> new Directory("", path));
            Directory directory = leaf.path().getPath().equals(rootPath) ? root : directoryOf(root, leaf.path(), rootPath);
            directory.myLeaves.add(leaf);
        }

        List<ChangesTreeItem> children = new ArrayList<>();
        int fileCount = 0;
        for (Map.Entry<Module, Map<String, Directory>> entry : modules.entrySet()) {
            ModuleTreeItem item = new ModuleTreeItem(scope, entry.getKey());
            Map<String, Directory> roots = entry.getValue();

            int count;
            if (roots.size() == 1) {
                count = emit(scope, item, roots.values().iterator().next());
            }
            else {
                List<ChangesTreeItem> rootItems = new ArrayList<>();
                count = 0;
                for (Map.Entry<String, Directory> root : roots.entrySet()) {
                    DirectoryTreeItem rootItem = new DirectoryTreeItem(scope, VcsUtil.getFilePath(root.getKey(), true), labelOf(root.getKey()));
                    int rootCount = emit(scope, rootItem, root.getValue());
                    myFileCounts.put(rootItem, rootCount);
                    rootItems.add(rootItem);
                    count += rootCount;
                }
                myChildren.put(item, rootItems);
            }

            myFileCounts.put(item, count);
            children.add(item);
            fileCount += count;
        }

        Children rest = emitChildren(scope, outside);
        children.addAll(rest.items());
        fileCount += rest.fileCount();

        return new Children(children, fileCount);
    }

    private @Nullable ModuleRoot moduleRootOf(FilePath path) {
        ProjectFileIndex fileIndex = myFileIndex;
        if (fileIndex == null) {
            return null;
        }

        FilePath parent = path.getParentPath();
        String key = path.isDirectory() || parent == null ? path.getPath() : parent.getPath();
        return myModuleRoots.computeIfAbsent(key, ignored -> Optional.ofNullable(findModuleRoot(fileIndex, path))).orElse(null);
    }

    private static @Nullable ModuleRoot findModuleRoot(ProjectFileIndex fileIndex, FilePath path) {
        VirtualFile file = path.getVirtualFile();
        for (FilePath parent = path.getParentPath(); file == null && parent != null; parent = parent.getParentPath()) {
            file = parent.getVirtualFile();
        }
        if (file == null) {
            return null;
        }

        Module module = fileIndex.getModuleForFile(file);
        VirtualFile contentRoot = fileIndex.getContentRootForFile(file);
        if (module == null || contentRoot == null) {
            return null;
        }
        return new ModuleRoot(module, contentRoot.getPath());
    }

    private Leaf leaf(ChangesTreeItem item, FilePath path) {
        Image icon = iconOf(item, path);
        if (icon != null) {
            myIcons.put(item, icon);
        }
        return new Leaf(item, path);
    }

    private static @Nullable Image iconOf(ChangesTreeItem item, FilePath path) {
        if (item instanceof FileTreeItem file) {
            return file.file().isDirectory() ? PlatformIconGroup.nodesTreeclosed() : file.file().getFileType().getIcon();
        }
        if (item instanceof PathTreeItem && path.isDirectory()) {
            return PlatformIconGroup.nodesTreeclosed();
        }
        return path.getFileType().getIcon();
    }

    private void putLocation(Leaf leaf) {
        FilePath parent = leaf.path().getParentPath();
        if (parent == null) {
            return;
        }
        String location = presentablePath(parent.getPath());
        if (!location.isEmpty()) {
            myLocations.put(leaf.item(), location);
        }
    }

    private String labelOf(String path) {
        String presentable = presentablePath(path);
        if (!presentable.isEmpty()) {
            return presentable;
        }
        int index = path.lastIndexOf('/');
        return index >= 0 ? path.substring(index + 1) : path;
    }

    private String presentablePath(String path) {
        String basePath = myBasePath;
        if (basePath != null && FileUtil.isAncestor(basePath, path, false)) {
            if (path.length() == basePath.length()) {
                return "";
            }
            return FileUtil.toSystemDependentName(path.substring(basePath.length() + 1));
        }
        return Objects.requireNonNullElse(UserHomeFileUtil.getLocationRelativeToUserHome(path), path);
    }

    private Directory directoryOf(Directory root, FilePath path, @Nullable String basePath) {
        FilePath parent = path.getParentPath();
        if (parent == null) {
            return root;
        }

        String parentPath = parent.getPath();
        Directory current = root;
        String accumulated;
        String relative;
        if (basePath != null && (parentPath.equals(basePath) || parentPath.startsWith(basePath + "/"))) {
            accumulated = basePath;
            relative = parentPath.length() == basePath.length() ? "" : parentPath.substring(basePath.length() + 1);
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
                text = labelOf(compacted.myPath);
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
