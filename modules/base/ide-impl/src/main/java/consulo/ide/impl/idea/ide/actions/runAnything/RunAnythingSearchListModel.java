// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything;

import consulo.ide.impl.idea.ide.actions.runAnything.activity.RunAnythingProvider;
import consulo.ide.impl.idea.ide.actions.runAnything.groups.RunAnythingCompletionGroup;
import consulo.ide.impl.idea.ide.actions.runAnything.groups.RunAnythingGroup;
import consulo.ide.impl.idea.ide.actions.runAnything.groups.RunAnythingHelpGroup;
import consulo.ide.impl.idea.ide.actions.runAnything.groups.RunAnythingRecentGroup;
import consulo.localize.LocalizeValue;
import consulo.ui.ex.awt.CollectionListModel;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.util.collection.MultiMap;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

@SuppressWarnings("unchecked")
public abstract class RunAnythingSearchListModel extends CollectionListModel<Object> {
    protected RunAnythingSearchListModel() {
        super();
        clearIndexes();
    }

    @Nonnull
    protected abstract List<RunAnythingGroup> getGroups();

    void clearIndexes() {
        RunAnythingGroup.clearIndexes(getGroups());
    }

    @Nullable
    RunAnythingGroup findGroupByMoreIndex(int index) {
        return RunAnythingGroup.findGroupByMoreIndex(getGroups(), index);
    }

    void shiftIndexes(int baseIndex, int shift) {
        RunAnythingGroup.shiftIndexes(getGroups(), baseIndex, shift);
    }

    @Nullable
    LocalizeValue getTitle(int titleIndex) {
        return RunAnythingGroup.getTitle(getGroups(), titleIndex);
    }

    @Nullable
    RunAnythingGroup findItemGroup(int titleIndex) {
        return RunAnythingGroup.findItemGroup(getGroups(), titleIndex);
    }

    int[] getAllIndexes() {
        RunAnythingGroup.getAllIndexes(getGroups());
        return new int[0];
    }

    boolean isMoreIndex(int index) {
        return RunAnythingGroup.isMoreIndex(getGroups(), index);
    }

    int next(int index) {
        int[] all = getAllIndexes();
        Arrays.sort(all);
        for (int next : all) {
            if (next > index) {
                return next;
            }
        }
        return 0;
    }

    int prev(int index) {
        int[] all = getAllIndexes();
        Arrays.sort(all);
        for (int i = all.length - 1; i >= 0; i--) {
            if (all[i] != -1 && all[i] < index) {
                return all[i];
            }
        }
        return all[all.length - 1];
    }

    public void update() {
        fireContentsChanged(this, 0, getSize() - 1);
    }

    public static class RunAnythingMainListModel extends RunAnythingSearchListModel {
        @Nonnull
        @Override
        public List<RunAnythingGroup> getGroups() {
            List<RunAnythingGroup> groups = ContainerUtil.newArrayList(RunAnythingRecentGroup.INSTANCE);
            groups.addAll(RunAnythingCompletionGroup.MAIN_GROUPS);
            return groups;
        }
    }

    public static class RunAnythingHelpListModel extends RunAnythingSearchListModel {
        @Nullable
        private List<RunAnythingGroup> myHelpGroups;

        @Nonnull
        @Override
        protected List<RunAnythingGroup> getGroups() {
            if (myHelpGroups == null) {
                myHelpGroups = new ArrayList<>();
                MultiMap<String, RunAnythingProvider> groupBy =
                    ContainerUtil.groupBy(RunAnythingProvider.EP_NAME.getExtensionList(), RunAnythingProvider::getHelpGroupTitle);

                for (Map.Entry<String, Collection<RunAnythingProvider>> entry : groupBy.entrySet()) {
                    myHelpGroups.add(new RunAnythingHelpGroup<>(entry.getKey(), entry.getValue()));
                }

                myHelpGroups.addAll(RunAnythingHelpGroup.EP_NAME.getExtensionList());

            }
            return myHelpGroups;
        }
    }
}
