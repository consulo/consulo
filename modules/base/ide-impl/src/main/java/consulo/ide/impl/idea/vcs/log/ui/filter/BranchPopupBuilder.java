/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
package consulo.ide.impl.idea.vcs.log.ui.filter;

import consulo.ui.ex.action.ActionGroup;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.versionControlSystem.log.RefGroup;
import consulo.versionControlSystem.log.VcsLogProvider;
import consulo.versionControlSystem.log.VcsLogRefManager;
import consulo.versionControlSystem.log.VcsRef;
import consulo.versionControlSystem.log.VcsLogDataPack;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.versionControlSystem.log.base.SingletonRefGroup;
import consulo.versionControlSystem.log.util.VcsLogUtil;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;

public abstract class BranchPopupBuilder {
  @Nonnull
  private final VcsLogDataPack myDataPack;
  @Nullable
  private final Collection<VirtualFile> myVisibleRoots;
  @Nullable
  private final List<List<String>> myRecentItems;

  protected BranchPopupBuilder(@Nonnull VcsLogDataPack dataPack,
                               @Nullable Collection<VirtualFile> visibleRoots,
                               @Nullable List<List<String>> recentItems) {
    myDataPack = dataPack;
    myVisibleRoots = visibleRoots;
    myRecentItems = recentItems;
  }

  @Nonnull
  protected abstract AnAction createAction(@Nonnull String name);

  protected void createRecentAction(@Nonnull DefaultActionGroup actionGroup, @Nonnull List<String> recentItem) {
    assert myRecentItems == null;
  }

  @Nonnull
  protected AnAction createCollapsedAction(String actionName) {
    return createAction(actionName);
  }

  public ActionGroup build() {
    return createActions(prepareGroups(myDataPack, myVisibleRoots, myRecentItems));
  }

  private static Groups prepareGroups(@Nonnull VcsLogDataPack dataPack,
                                      @Nullable Collection<VirtualFile> visibleRoots,
                                      @Nullable List<List<String>> recentItems) {
    Groups filteredGroups = new Groups();
    Collection<VcsRef> allRefs = dataPack.getRefs().getBranches();
    for (Map.Entry<VirtualFile, Set<VcsRef>> entry : VcsLogUtil.groupRefsByRoot(allRefs).entrySet()) {
      VirtualFile root = entry.getKey();
      if (visibleRoots != null && !visibleRoots.contains(root)) continue;
      Collection<VcsRef> refs = entry.getValue();
      VcsLogProvider provider = dataPack.getLogProviders().get(root);
      VcsLogRefManager refManager = provider.getReferenceManager();
      List<RefGroup> refGroups = refManager.groupForBranchFilter(refs);

      putActionsForReferences(refGroups, filteredGroups);
    }

    if (recentItems != null) {
      for (List<String> recentItem : recentItems) {
        if (recentItem.size() == 1) {
          final String item = ContainerUtil.getFirstItem(recentItem);
          if (filteredGroups.singletonGroups.contains(item) ||
              ContainerUtil.find(filteredGroups.expandedGroups.values(), strings -> strings.contains(item)) != null) {
            continue;
          }
        }
        filteredGroups.recentGroups.add(recentItem);
      }
    }

    return filteredGroups;
  }

  private DefaultActionGroup createActions(@Nonnull Groups groups) {
    DefaultActionGroup actionGroup = new DefaultActionGroup();
    for (String actionName : groups.singletonGroups) {
      actionGroup.add(createAction(actionName));
    }
    if (!groups.recentGroups.isEmpty()) {
      actionGroup.addSeparator("Recent");
      for (List<String> recentItem : groups.recentGroups) {
        createRecentAction(actionGroup, recentItem);
      }
    }
    for (Map.Entry<String, TreeSet<String>> group : groups.expandedGroups.entrySet()) {
      actionGroup.addSeparator(group.getKey());
      for (String actionName : group.getValue()) {
        actionGroup.add(createAction(actionName));
      }
    }
    actionGroup.addSeparator();
    for (Map.Entry<String, TreeSet<String>> group : groups.collapsedGroups.entrySet()) {
      DefaultActionGroup popupGroup = new DefaultActionGroup(group.getKey(), true);
      for (String actionName : group.getValue()) {
        popupGroup.add(createCollapsedAction(actionName));
      }
      actionGroup.add(popupGroup);
    }
    return actionGroup;
  }

  private static class Groups {
    private final TreeSet<String> singletonGroups = new TreeSet<>();
    private final List<List<String>> recentGroups = new ArrayList<>();
    private final TreeMap<String, TreeSet<String>> expandedGroups = new TreeMap<>();
    private final TreeMap<String, TreeSet<String>> collapsedGroups = new TreeMap<>();
  }

  private static void putActionsForReferences(List<RefGroup> references, Groups actions) {
    for (final RefGroup refGroup : references) {
      if (refGroup instanceof SingletonRefGroup) {
        String name = refGroup.getName();
        if (!actions.singletonGroups.contains(name)) {
          actions.singletonGroups.add(name);
        }
      }
      else if (refGroup.isExpanded()) {
        addToGroup(refGroup, actions.expandedGroups);
      }
      else {
        addToGroup(refGroup, actions.collapsedGroups);
      }
    }
  }

  private static void addToGroup(final RefGroup refGroup, TreeMap<String, TreeSet<String>> groupToAdd) {
    TreeSet<String> existingGroup = groupToAdd.get(refGroup.getName());

    TreeSet<String> actions = new TreeSet<>();
    for (VcsRef ref : refGroup.getRefs()) {
      actions.add(ref.getName());
    }

    if (existingGroup == null) {
      groupToAdd.put(refGroup.getName(), actions);
    }
    else {
      for (String action : actions) {
        existingGroup.add(action);
      }
    }
  }
}
