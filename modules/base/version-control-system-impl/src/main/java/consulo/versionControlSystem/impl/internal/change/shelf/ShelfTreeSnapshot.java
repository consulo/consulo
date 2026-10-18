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
package consulo.versionControlSystem.impl.internal.change.shelf;

import consulo.project.Project;
import consulo.util.lang.Couple;
import consulo.versionControlSystem.change.shelf.ShelvedBinaryFile;
import consulo.versionControlSystem.change.shelf.ShelvedChangeList;
import consulo.versionControlSystem.impl.internal.util.RelativePathCalculator;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.*;

/**
 * The shelved change lists and their files as the shelf view shows them. The patches are read while the snapshot is
 * built, so it is built off the UI thread and the view only paints it.
 *
 * @author VISTALL
 * @since 2026-10-18
 */
final class ShelfTreeSnapshot {
    static final ShelfTreeSnapshot EMPTY = new ShelfTreeSnapshot(false, List.of(), Map.of(), Map.of(), Map.of());

    private final boolean myHasLists;
    private final List<ShelvedChangeListImpl> myLists;
    private final Map<ShelvedChangeListImpl, List<Object>> myFiles;
    private final Map<Object, ShelvedChangeListImpl> myOwners;
    private final Map<Couple<String>, String> myMovedMessages;

    private ShelfTreeSnapshot(boolean hasLists,
                              List<ShelvedChangeListImpl> lists,
                              Map<ShelvedChangeListImpl, List<Object>> files,
                              Map<Object, ShelvedChangeListImpl> owners,
                              Map<Couple<String>, String> movedMessages) {
        myHasLists = hasLists;
        myLists = lists;
        myFiles = files;
        myOwners = owners;
        myMovedMessages = movedMessages;
    }

    static ShelfTreeSnapshot build(Project project, ShelveChangesManagerImpl manager) {
        List<ShelvedChangeList> shelved = manager.getShelvedChangeLists();
        List<ShelvedChangeList> recycled = manager.getRecycledShelvedChangeLists();

        List<ShelvedChangeListImpl> lists = new ArrayList<>();
        addSorted(lists, shelved);
        if (manager.isShowRecycled()) {
            addSorted(lists, recycled);
        }

        Map<ShelvedChangeListImpl, List<Object>> files = new IdentityHashMap<>();
        Map<Object, ShelvedChangeListImpl> owners = new IdentityHashMap<>();
        Map<Couple<String>, String> movedMessages = new HashMap<>();
        for (ShelvedChangeListImpl list : lists) {
            List<Object> listFiles = new ArrayList<>();
            for (ShelvedChangeImpl change : list.getChanges(project)) {
                putMovedMessage(movedMessages, change.getBeforePath(), change.getAfterPath());
                listFiles.add(change);
            }
            for (ShelvedBinaryFile file : list.getBinaryFiles()) {
                putMovedMessage(movedMessages, file.getBeforePath(), file.getAfterPath());
                listFiles.add(file);
            }
            listFiles.sort(Comparator.comparing(ShelfTreeSnapshot::fileName, String.CASE_INSENSITIVE_ORDER));
            files.put(list, List.copyOf(listFiles));
            for (Object file : listFiles) {
                owners.put(file, list);
            }
        }
        return new ShelfTreeSnapshot(!shelved.isEmpty() || !recycled.isEmpty(), List.copyOf(lists), files, owners, movedMessages);
    }

    private static void addSorted(List<ShelvedChangeListImpl> target, List<ShelvedChangeList> lists) {
        List<ShelvedChangeListImpl> sorted = new ArrayList<>();
        for (ShelvedChangeList list : lists) {
            if (list instanceof ShelvedChangeListImpl impl) {
                sorted.add(impl);
            }
        }
        sorted.sort(Comparator.comparing(ShelvedChangeListImpl::getDate).reversed());
        target.addAll(sorted);
    }

    private static void putMovedMessage(Map<Couple<String>, String> movedMessages, @Nullable String beforeName, @Nullable String afterName) {
        String movedMessage = RelativePathCalculator.getMovedString(beforeName, afterName);
        if (movedMessage != null) {
            movedMessages.put(Couple.of(beforeName, afterName), movedMessage);
        }
    }

    /**
     * Whether the shelf holds any list at all, recycled ones included - the tab stays while there is something to show
     * after "Show Recently Deleted".
     */
    boolean hasLists() {
        return myHasLists;
    }

    List<ShelvedChangeListImpl> getLists() {
        return myLists;
    }

    List<Object> getFiles(ShelvedChangeListImpl list) {
        return myFiles.getOrDefault(list, List.of());
    }

    @Nullable
    ShelvedChangeListImpl getOwner(Object file) {
        return myOwners.get(file);
    }

    @Nullable
    String getMovedMessage(@Nullable String beforePath, @Nullable String afterPath) {
        return myMovedMessages.get(Couple.of(beforePath, afterPath));
    }

    @Nullable
    static String path(Object file) {
        if (file instanceof ShelvedChangeImpl change) {
            return change.getBeforePath();
        }
        if (file instanceof ShelvedBinaryFile binaryFile) {
            return binaryFile.getBeforePath() == null ? binaryFile.getAfterPath() : binaryFile.getBeforePath();
        }
        return null;
    }

    static String fileName(Object file) {
        String path = path(file);
        if (path == null) {
            return "";
        }
        String systemPath = path.replace('/', File.separatorChar);
        int index = systemPath.lastIndexOf(File.separatorChar);
        return index >= 0 ? systemPath.substring(index + 1) : systemPath;
    }
}
