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

import consulo.ui.image.Image;
import consulo.versionControlSystem.FilePath;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 2026-10-17
 */
public final class ChangesTreeSnapshot {
    public static final ChangesTreeSnapshot EMPTY = new ChangesTreeSnapshot(List.of(), Map.of(), Map.of(), Map.of(), Map.of(), Map.of(), false);

    private final List<ChangesTreeItem> myRoots;
    private final Map<ChangesTreeItem, List<ChangesTreeItem>> myChildren;
    private final Map<ChangesTreeItem, Integer> myFileCounts;
    private final Map<ChangesTreeItem, String> myLocations;
    private final Map<ChangesTreeItem, Image> myIcons;
    private final Map<ChangesTreeItem, FilePath> myPaths;
    private final boolean myFlatten;

    ChangesTreeSnapshot(List<ChangesTreeItem> roots,
                        Map<ChangesTreeItem, List<ChangesTreeItem>> children,
                        Map<ChangesTreeItem, Integer> fileCounts,
                        Map<ChangesTreeItem, String> locations,
                        Map<ChangesTreeItem, Image> icons,
                        Map<ChangesTreeItem, FilePath> paths,
                        boolean flatten) {
        myRoots = roots;
        myChildren = children;
        myFileCounts = fileCounts;
        myLocations = locations;
        myIcons = icons;
        myPaths = paths;
        myFlatten = flatten;
    }

    public @Nullable Image getIcon(ChangesTreeItem item) {
        return myIcons.get(item);
    }

    public @Nullable FilePath getPath(ChangesTreeItem item) {
        return myPaths.get(item);
    }

    public @Nullable String getLocation(ChangesTreeItem item) {
        return myLocations.get(item);
    }

    public List<ChangesTreeItem> getChildren(@Nullable ChangesTreeItem parent) {
        if (parent == null) {
            return myRoots;
        }
        return myChildren.getOrDefault(parent, List.of());
    }

    public boolean hasChildren(ChangesTreeItem item) {
        return !getChildren(item).isEmpty();
    }

    public boolean isGroup(ChangesTreeItem item) {
        return item instanceof ChangeListTreeItem
            || item instanceof ChangesGroupTreeItem
            || item instanceof FileGroupTreeItem
            || item instanceof ModuleTreeItem
            || item instanceof RepositoryTreeItem
            || item instanceof DirectoryTreeItem;
    }

    public int getFileCount(ChangesTreeItem item) {
        return myFileCounts.getOrDefault(item, 0);
    }

    public boolean isFlatten() {
        return myFlatten;
    }

    public List<ChangesTreeItem> getRoots() {
        return myRoots;
    }

    public List<ChangesTreeItem> collectLeaves(ChangesTreeItem item) {
        List<ChangesTreeItem> result = new ArrayList<>();
        collectLeaves(item, result);
        return result;
    }

    public boolean anyLeaf(ChangesTreeItem item, Predicate<ChangesTreeItem> predicate) {
        if (!isGroup(item)) {
            return predicate.test(item);
        }

        for (ChangesTreeItem child : getChildren(item)) {
            if (!(child instanceof ChangesGroupTreeItem) && anyLeaf(child, predicate)) {
                return true;
            }
        }
        return false;
    }

    private void collectLeaves(ChangesTreeItem item, List<ChangesTreeItem> result) {
        if (!isGroup(item)) {
            result.add(item);
            return;
        }

        for (ChangesTreeItem child : getChildren(item)) {
            // groups added by ChangesViewModifier are not part of their change list
            if (!(child instanceof ChangesGroupTreeItem)) {
                collectLeaves(child, result);
            }
        }
    }
}
