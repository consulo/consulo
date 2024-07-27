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
package consulo.versionControlSystem.impl.internal.change.commited;

import consulo.container.boot.ContainerPathManager;
import consulo.project.Project;
import consulo.versionControlSystem.*;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;
import java.util.function.Function;

public class CachesHolder {
  private static final String VCS_CACHE_PATH = "vcsCache";

  private final Project myProject;
  private final Map<String, ChangesCacheFile> myCacheFiles;
  private final RepositoryLocationCache myLocationCache;
  private final ProjectLevelVcsManager myPlManager;

  public CachesHolder(final Project project, final RepositoryLocationCache locationCache) {
    myProject = project;
    myLocationCache = locationCache;
    myPlManager = ProjectLevelVcsManager.getInstance(myProject);
    myCacheFiles = new ConcurrentHashMap<>();
  }

  public CachesHolder(final Project project, final RepositoryLocationCache locationCache, final ProjectLevelVcsManager manager) {
    myProject = project;
    myPlManager = manager;
    myLocationCache = locationCache;
    myCacheFiles = new ConcurrentHashMap<>();
  }

  /**
   * Returns all paths that will be used to collect committed changes about. ideally, for one checkout there should be one file
   */
  public Map<VirtualFile, RepositoryLocation> getAllRootsUnderVcs(final AbstractVcs vcs) {
    final RootsCalculator calculator = new RootsCalculator(myProject, vcs, myLocationCache);
    return calculator.getRoots();
  }

  public void iterateAllRepositoryLocations(final BiPredicate<RepositoryLocation, AbstractVcs> locationProcessor) {
    final AbstractVcs[] vcses = myPlManager.getAllActiveVcss();
    for (AbstractVcs vcs : vcses) {
      final CommittedChangesProvider provider = vcs.getCommittedChangesProvider();
      if (provider instanceof CachingCommittedChangesProvider) {
        final Map<VirtualFile, RepositoryLocation> map = getAllRootsUnderVcs(vcs);
        for (VirtualFile root : map.keySet()) {
          final RepositoryLocation location = map.get(root);
          if (!locationProcessor.test(location, vcs)) {
            return;
          }
        }
      }
    }
  }

  public void iterateAllCaches(final Function<ChangesCacheFile, Boolean> consumer) {
    final AbstractVcs[] vcses = myPlManager.getAllActiveVcss();
    for (AbstractVcs vcs : vcses) {
      final CommittedChangesProvider provider = vcs.getCommittedChangesProvider();
      if (provider instanceof CachingCommittedChangesProvider) {
        final Map<VirtualFile, RepositoryLocation> map = getAllRootsUnderVcs(vcs);
        for (VirtualFile root : map.keySet()) {
          final RepositoryLocation location = map.get(root);
          final ChangesCacheFile cacheFile = getCacheFile(vcs, root, location);
          if (Boolean.TRUE.equals(consumer.apply(cacheFile))) {
            return;
          }
        }
      }
    }
  }

  public void clearAllCaches() {
    for (Map.Entry<String, ChangesCacheFile> entry : myCacheFiles.entrySet()) {
      entry.getValue().delete();
    }
    myCacheFiles.clear();
  }

  public List<ChangesCacheFile> getAllCaches() {
    final List<ChangesCacheFile> result = new ArrayList<>();
    iterateAllCaches(new Function<>() {
      @Nonnull
      public Boolean apply(final ChangesCacheFile changesCacheFile) {
        result.add(changesCacheFile);
        return false;
      }
    });
    return result;
  }

  public ChangesCacheFile getCacheFile(AbstractVcs vcs, VirtualFile root, RepositoryLocation location) {
    final String key = location.getKey();
    ChangesCacheFile cacheFile = myCacheFiles.get(key);
    if (cacheFile == null) {
      cacheFile = new ChangesCacheFile(myProject, getCachePath(location), vcs, root, location);
      myCacheFiles.put(key, cacheFile);
    }
    return cacheFile;
  }

  public File getCacheBasePath() {
    File file = new File(ContainerPathManager.get().getSystemPath(), VCS_CACHE_PATH);
    file = new File(file, myProject.getLocationHash());
    return file;
  }

  @SuppressWarnings({"HardCodedStringLiteral"})
  private File getCachePath(final RepositoryLocation location) {
    File file = getCacheBasePath();
    file.mkdirs();
    String s = location.getKey();
    try {
      final byte[] bytes = MessageDigest.getInstance("MD5").digest(s.getBytes(StandardCharsets.UTF_8));
      StringBuilder result = new StringBuilder();
      for (byte aByte : bytes) {
        result.append(String.format("%02x", aByte));
      }
      return new File(file, result.toString());
    }
    catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
  }

  public ChangesCacheFile haveCache(RepositoryLocation location) {
    String key = location.getKey();
    if (myCacheFiles.containsKey(key)) return myCacheFiles.get(key);
    key = key.endsWith("/") ? key : (key + "/");
    for (String s : myCacheFiles.keySet()) {
      if (key.startsWith(s) || s.startsWith(key)) return myCacheFiles.get(s);
    }
    return null;
  }
}
