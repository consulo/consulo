// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.virtualFileSystem.impl.internal.watcher;

import consulo.logging.Logger;
import consulo.util.collection.MultiMap;
import consulo.util.collection.SmartList;
import consulo.util.lang.Couple;
import consulo.util.lang.Pair;
import consulo.virtualFileSystem.internal.FileSystemUtil;
import org.jspecify.annotations.Nullable;

import java.nio.file.FileSystems;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

class CanonicalPathMap {
  private static final Logger LOG = Logger.getInstance(FileWatcher.class);

  private final List<Path> myRecursiveWatchRoots;
  private final List<Path> myFlatWatchRoots;
  private final List<Path> myCanonicalRecursiveWatchRoots;
  private final List<Path> myCanonicalFlatWatchRoots;
  private final MultiMap<Path, Path> myPathMapping;

  CanonicalPathMap() {
    myRecursiveWatchRoots = myCanonicalRecursiveWatchRoots = myFlatWatchRoots = myCanonicalFlatWatchRoots = Collections.emptyList();
    myPathMapping = MultiMap.empty();
  }

  CanonicalPathMap(List<Path> recursive, List<Path> flat) {
    myRecursiveWatchRoots = new ArrayList<>(recursive);
    myFlatWatchRoots = new ArrayList<>(flat);

    List<Pair<Path, Path>> mapping = new SmartList<>();
    Map<Path, Path> resolvedPaths = resolvePaths(recursive, flat);
    myCanonicalRecursiveWatchRoots = mapPaths(resolvedPaths, recursive, mapping);
    myCanonicalFlatWatchRoots = mapPaths(resolvedPaths, flat, mapping);

    myPathMapping = MultiMap.createConcurrentSet();
    addMapping(mapping);
  }

  private static Map<Path, Path> resolvePaths(Collection<Path> recursiveRoots, Collection<Path> flatRoots) {
    Map<Path, Path> result = new ConcurrentHashMap<>();
    Stream.concat(recursiveRoots.stream(), flatRoots.stream()).parallel().forEach(root -> {
      Path resolved = resolveSymLink(root);
      if (resolved != null) {
        result.put(root, resolved);
      }
    });
    return result;
  }

  /**
   * Symbolic links are resolved for the paths of this machine only - a watcher of another machine reports the paths as they were
   * given to it.
   */
  private static @Nullable Path resolveSymLink(Path root) {
    if (root.getFileSystem() != FileSystems.getDefault()) {
      return null;
    }
    String resolved = FileSystemUtil.resolveSymLink(root.toString());
    if (resolved == null) {
      return null;
    }
    try {
      return Path.of(resolved);
    }
    catch (InvalidPathException e) {
      return null;
    }
  }

  private static List<Path> mapPaths(Map<Path, Path> resolvedPaths, List<Path> paths, Collection<? super Pair<Path, Path>> mapping) {
    List<Path> canonicalPaths = new ArrayList<>(paths);
    for (int i = 0; i < paths.size(); i++) {
      Path path = paths.get(i);
      Path canonicalPath = resolvedPaths.get(path);
      if (canonicalPath != null && !path.equals(canonicalPath)) {
        canonicalPaths.set(i, canonicalPath);
        mapping.add(Couple.of(canonicalPath, path));
      }
    }
    return canonicalPaths;
  }

  List<Path> getCanonicalRecursiveWatchRoots() {
    return myCanonicalRecursiveWatchRoots;
  }

  List<Path> getCanonicalFlatWatchRoots() {
    return myCanonicalFlatWatchRoots;
  }

  List<Path> getRecursiveWatchRoots() {
    return myRecursiveWatchRoots;
  }

  public void addMapping(Collection<? extends Pair<Path, Path>> mapping) {
    for (Pair<Path, Path> pair : mapping) {
      // See if we are adding a mapping that itself should be mapped to a different path
      // Example: /foo/real_path -> /foo/symlink, /foo/remapped_path -> /foo/real_path
      // In this case, if the file watcher returns /foo/remapped_path/file.txt, we want to report /foo/symlink/file.txt back to IntelliJ.
      Collection<Path> preRemapPathToWatchedPaths = myPathMapping.get(pair.second);
      for (Path realWatchedPath : preRemapPathToWatchedPaths) {
        Collection<Path> remappedPathMappings = myPathMapping.getModifiable(pair.first);
        remappedPathMappings.add(realWatchedPath);
      }

      // Since there can be more than one file watcher and REMAPPING is an implementation detail of the native file watcher,
      // add the mapping as usual even if we added data above.
      Collection<Path> symLinksToCanonicalPath = myPathMapping.getModifiable(pair.first);
      symLinksToCanonicalPath.add(pair.second);
    }
  }

  /**
   * Maps reported paths from canonical representation to requested paths, then filters out those which do not fall under watched roots.
   *
   * <h3>Exactness</h3>
   * Some watchers (notable the native one on OS X) report a parent directory as dirty instead of the "exact" file path.
   * <p>
   * For flat roots, it means that if and only if the exact dirty file path is returned, we should compare the parent to the flat roots,
   * otherwise we should compare to path given to us because it is already the parent of the actual dirty path.
   * <p>
   * For recursive roots, if the path given to us is already the parent of the actual dirty path, we need to compare the path to the parent
   * of the recursive root because if the root itself was changed, we need to know about it.
   */
  Collection<Path> getWatchedPaths(Path reportedPath, boolean isExact) {
    if (myFlatWatchRoots.isEmpty() && myRecursiveWatchRoots.isEmpty()) return Collections.emptyList();

    Collection<Path> affectedPaths = applyMapping(reportedPath);
    Collection<Path> changedPaths = new SmartList<>();

    ext:
    for (Path path : affectedPaths) {
      for (Path root : myFlatWatchRoots) {
        if (path.equals(root)) {
          changedPaths.add(path);
          continue ext;
        }
        if (isExact) {
          if (isParent(path, root)) {
            changedPaths.add(path);
            continue ext;
          }
        }
        else if (isParent(root, path)) {
          changedPaths.add(root);
        }
      }

      for (Path root : myRecursiveWatchRoots) {
        if (path.startsWith(root)) {
          changedPaths.add(path);
          continue ext;
        }
        if (!isExact && isParent(root, path)) {
          changedPaths.add(root);
        }
      }
    }

    if (changedPaths.isEmpty() && LOG.isDebugEnabled()) {
      LOG.debug("Not watchable, filtered: " + reportedPath);
    }

    return changedPaths;
  }

  private static boolean isParent(Path path, Path parent) {
    return parent.equals(path.getParent());
  }

  private Collection<Path> applyMapping(Path reportedPath) {
    if (myPathMapping.isEmpty()) {
      return Collections.singletonList(reportedPath);
    }

    List<Path> results = new SmartList<>(reportedPath);
    Path root = reportedPath.getRoot();
    int nameCount = reportedPath.getNameCount();
    for (int i = root != null ? -1 : 0; i < nameCount; ++i) {
      Path runningPath = i < 0 ? root : root != null ? root.resolve(reportedPath.subpath(0, i + 1)) : reportedPath.subpath(0, i + 1);
      Collection<Path> mappedPaths = myPathMapping.get(runningPath);
      for (Path mappedPath : mappedPaths) {
        // Append the specific file suffix to the mapped watch root.
        results.add(i + 1 < nameCount ? mappedPath.resolve(reportedPath.subpath(i + 1, nameCount).toString()) : mappedPath);
      }
    }

    return results;
  }
}
