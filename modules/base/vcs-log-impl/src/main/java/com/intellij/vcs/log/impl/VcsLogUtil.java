/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package com.intellij.vcs.log.impl;

import com.intellij.internal.statistic.UsageTrigger;
import com.intellij.internal.statistic.beans.ConvertUsagesUtil;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.VcsException;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.Function;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.vcs.log.*;
import com.intellij.vcs.log.data.VcsLogData;
import com.intellij.vcs.log.graph.VisibleGraph;
import com.intellij.vcs.log.ui.VcsLogUiImpl;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.*;

import static com.intellij.util.ObjectUtils.notNull;
import static com.intellij.util.containers.ContainerUtil.getFirstItem;
import static java.util.Collections.singletonList;

public class VcsLogUtil {
  public static final int MAX_SELECTED_COMMITS = 1000;

  @Nonnull
  public static Map<VirtualFile, Set<VcsRef>> groupRefsByRoot(@Nonnull Collection<VcsRef> refs) {
    return groupByRoot(refs, VcsRef::getRoot);
  }

  @Nonnull
  private static <T> Map<VirtualFile, Set<T>> groupByRoot(@Nonnull Collection<T> items, @Nonnull Function<T, VirtualFile> rootGetter) {
    Map<VirtualFile, Set<T>> map = new TreeMap<>(Comparator.comparing(VirtualFile::getPresentableUrl));
    for (T item : items) {
      VirtualFile root = rootGetter.fun(item);
      Set<T> set = map.get(root);
      if (set == null) {
        set = ContainerUtil.newHashSet();
        map.put(root, set);
      }
      set.add(item);
    }
    return map;
  }

  @Nonnull
  public static List<Integer> getVisibleCommits(@Nonnull final VisibleGraph<Integer> visibleGraph) {
    return new AbstractList<Integer>() {
      @Override
      public Integer get(int index) {
        return visibleGraph.getRowInfo(index).getCommit();
      }

      @Override
      public int size() {
        return visibleGraph.getVisibleCommitCount();
      }
    };
  }

  public static int compareRoots(@Nonnull VirtualFile root1, @Nonnull VirtualFile root2) {
    return root1.getPresentableUrl().compareTo(root2.getPresentableUrl());
  }

  @Nonnull
  private static Set<VirtualFile> collectRoots(@Nonnull Collection<FilePath> files, @Nonnull Set<VirtualFile> roots) {
    Set<VirtualFile> selectedRoots = new HashSet<>();

    List<VirtualFile> sortedRoots = ContainerUtil.sorted(roots, Comparator.comparing(VirtualFile::getPath));

    for (FilePath filePath : files) {
      VirtualFile virtualFile = filePath.getVirtualFile();

      if (virtualFile != null && roots.contains(virtualFile)) {
        // if a root itself is selected, add this root
        selectedRoots.add(virtualFile);
      }
      else {
        VirtualFile candidateAncestorRoot = null;
        for (VirtualFile root : sortedRoots) {
          if (FileUtil.isAncestor(VfsUtilCore.virtualToIoFile(root), filePath.getIOFile(), false)) {
            candidateAncestorRoot = root;
          }
        }
        if (candidateAncestorRoot != null) {
          selectedRoots.add(candidateAncestorRoot);
        }
      }

      // add all roots under selected path
      if (virtualFile != null) {
        for (VirtualFile root : roots) {
          if (VfsUtilCore.isAncestor(virtualFile, root, false)) {
            selectedRoots.add(root);
          }
        }
      }
    }

    return selectedRoots;
  }


  // collect absolutely all roots that might be visible
  // if filters unset returns just all roots
  @Nonnull
  public static Set<VirtualFile> getAllVisibleRoots(@Nonnull Collection<VirtualFile> roots,
                                                    @Nullable VcsLogRootFilter rootFilter,
                                                    @Nullable VcsLogStructureFilter structureFilter) {
    if (rootFilter == null && structureFilter == null) return new HashSet<>(roots);

    Collection<VirtualFile> fromRootFilter;
    if (rootFilter != null) {
      fromRootFilter = rootFilter.getRoots();
    }
    else {
      fromRootFilter = roots;
    }

    Collection<VirtualFile> fromStructureFilter;
    if (structureFilter != null) {
      fromStructureFilter = collectRoots(structureFilter.getFiles(), new HashSet<>(roots));
    }
    else {
      fromStructureFilter = roots;
    }

    return new HashSet<>(ContainerUtil.intersection(fromRootFilter, fromStructureFilter));
  }

  // for given root returns files that are selected in it
  // if a root is visible as a whole returns empty set
  // same if root is invisible as a whole
  // so check that before calling this method
  @Nonnull
  public static Set<FilePath> getFilteredFilesForRoot(@Nonnull final VirtualFile root, @Nonnull VcsLogFilterCollection filterCollection) {
    if (filterCollection.getStructureFilter() == null) return Collections.emptySet();
    Collection<FilePath> files = filterCollection.getStructureFilter().getFiles();

    return new HashSet<>(ContainerUtil.filter(files, filePath -> {
      VirtualFile virtualFile = filePath.getVirtualFile();
      return root.equals(virtualFile) || FileUtil.isAncestor(VfsUtilCore.virtualToIoFile(root), filePath.getIOFile(), false);
    }));
  }

  @Nonnull
  public static <T> List<T> collectFirstPack(@Nonnull List<T> list, int max) {
    return list.subList(0, Math.min(list.size(), max));
  }

  @Nonnull
  public static Set<VirtualFile> getVisibleRoots(@Nonnull VcsLogUiImpl logUi) {
    VcsLogFilterCollection filters = logUi.getFilterUi().getFilters();
    Set<VirtualFile> roots = logUi.getDataPack().getLogProviders().keySet();
    return getAllVisibleRoots(roots, filters.getRootFilter(), filters.getStructureFilter());
  }

  @Nullable
  public static String getSingleFilteredBranch(@Nonnull VcsLogBranchFilter filter, @Nonnull VcsLogRefs refs) {
    String branchName = null;
    Set<VirtualFile> checkedRoots = ContainerUtil.newHashSet();
    for (VcsRef branch : refs.getBranches()) {
      if (!filter.matches(branch.getName())) continue;

      if (branchName == null) {
        branchName = branch.getName();
      }
      else if (!branch.getName().equals(branchName)) {
        return null;
      }

      if (checkedRoots.contains(branch.getRoot())) return null;
      checkedRoots.add(branch.getRoot());
    }

    return branchName;
  }

  public static void triggerUsage(@Nonnull AnActionEvent e) {
    String text = e.getPresentation().getText();
    if (text != null) {
      triggerUsage(text);
    }
  }

  public static void triggerUsage(@Nonnull String text) {
    UsageTrigger.trigger("vcs.log." + ConvertUsagesUtil.ensureProperKey(text).replace(" ", ""));
  }

  public static boolean maybeRegexp(@Nonnull String text) {
    return StringUtil.containsAnyChar(text, "()[]{}.*?+^$\\|");
  }

  @Nonnull
  public static VcsFullCommitDetails getDetails(@Nonnull VcsLogData data, @Nonnull VirtualFile root, @Nonnull Hash hash) throws VcsException {
    return notNull(getFirstItem(getDetails(data.getLogProvider(root), root, singletonList(hash.asString()))));
  }

  @Nonnull
  public static List<? extends VcsFullCommitDetails> getDetails(@Nonnull VcsLogProvider logProvider, @Nonnull VirtualFile root, @Nonnull List<String> hashes)
          throws VcsException {
    List<VcsFullCommitDetails> result = ContainerUtil.newArrayList();
    logProvider.readFullDetails(root, hashes, result::add);
    return result;
  }
}
