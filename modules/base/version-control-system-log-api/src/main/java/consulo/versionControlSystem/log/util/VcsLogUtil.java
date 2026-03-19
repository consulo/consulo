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
package consulo.versionControlSystem.log.util;

import consulo.externalService.statistic.ConvertUsagesUtil;
import consulo.externalService.statistic.UsageTrigger;
import consulo.ui.ex.action.AnActionEvent;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.log.*;
import consulo.versionControlSystem.log.graph.VisibleGraph;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;

import org.jspecify.annotations.Nullable;
import java.util.*;
import java.util.function.Function;

import static consulo.util.lang.ObjectUtil.notNull;
import static java.util.Collections.singletonList;

public class VcsLogUtil {
  public static final int MAX_SELECTED_COMMITS = 1000;

  
  public static Map<VirtualFile, Set<VcsRef>> groupRefsByRoot(Collection<VcsRef> refs) {
    return groupByRoot(refs, VcsRef::getRoot);
  }

  
  private static <T> Map<VirtualFile, Set<T>> groupByRoot(Collection<T> items, Function<T, VirtualFile> rootGetter) {
    Map<VirtualFile, Set<T>> map = new TreeMap<>(Comparator.comparing(VirtualFile::getPresentableUrl));
    for (T item : items) {
      VirtualFile root = rootGetter.apply(item);
      Set<T> set = map.get(root);
      if (set == null) {
        set = new HashSet<T>();
        map.put(root, set);
      }
      set.add(item);
    }
    return map;
  }

  
  public static List<Integer> getVisibleCommits(final VisibleGraph<Integer> visibleGraph) {
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

  public static int compareRoots(VirtualFile root1, VirtualFile root2) {
    return root1.getPresentableUrl().compareTo(root2.getPresentableUrl());
  }

  
  private static Set<VirtualFile> collectRoots(Collection<FilePath> files, Set<VirtualFile> roots) {
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
          if (FileUtil.isAncestor(VirtualFileUtil.virtualToIoFile(root), filePath.getIOFile(), false)) {
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
          if (VirtualFileUtil.isAncestor(virtualFile, root, false)) {
            selectedRoots.add(root);
          }
        }
      }
    }

    return selectedRoots;
  }


  // collect absolutely all roots that might be visible
  // if filters unset returns just all roots
  
  public static Set<VirtualFile> getAllVisibleRoots(Collection<VirtualFile> roots,
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
  
  public static Set<FilePath> getFilteredFilesForRoot(VirtualFile root, VcsLogFilterCollection filterCollection) {
    if (filterCollection.getStructureFilter() == null) return Collections.emptySet();
    Collection<FilePath> files = filterCollection.getStructureFilter().getFiles();

    return new HashSet<>(ContainerUtil.filter(files, filePath -> {
      VirtualFile virtualFile = filePath.getVirtualFile();
      return root.equals(virtualFile) || FileUtil.isAncestor(VirtualFileUtil.virtualToIoFile(root), filePath.getIOFile(), false);
    }));
  }

  
  public static <T> List<T> collectFirstPack(List<T> list, int max) {
    return list.subList(0, Math.min(list.size(), max));
  }

  
  public static Set<VirtualFile> getVisibleRoots(VcsLogUi logUi) {
    VcsLogFilterCollection filters = logUi.getFilterUi().getFilters();
    Set<VirtualFile> roots = logUi.getDataPack().getLogProviders().keySet();
    return getAllVisibleRoots(roots, filters.getRootFilter(), filters.getStructureFilter());
  }

  public static @Nullable String getSingleFilteredBranch(VcsLogBranchFilter filter, VcsLogRefs refs) {
    String branchName = null;
    Set<VirtualFile> checkedRoots = new HashSet<>();
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

  public static void triggerUsage(AnActionEvent e) {
    String text = e.getPresentation().getText();
    if (text != null) {
      triggerUsage(text);
    }
  }

  public static void triggerUsage(String text) {
    UsageTrigger.trigger("vcs.log." + ConvertUsagesUtil.ensureProperKey(text).replace(" ", ""));
  }

  public static boolean maybeRegexp(String text) {
    return StringUtil.containsAnyChar(text, "()[]{}.*?+^$\\|");
  }

  
  public static VcsFullCommitDetails getDetails(VcsLogData data, VirtualFile root, Hash hash) throws VcsException {
    return notNull(ContainerUtil.getFirstItem(getDetails(data.getLogProvider(root), root, singletonList(hash.asString()))));
  }

  
  public static List<? extends VcsFullCommitDetails> getDetails(VcsLogProvider logProvider, VirtualFile root, List<String> hashes)
          throws VcsException {
    List<VcsFullCommitDetails> result = ContainerUtil.newArrayList();
    logProvider.readFullDetails(root, hashes, result::add);
    return result;
  }
}
