/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package consulo.ide.impl.idea.vcs.log.util;

import consulo.index.io.KeyDescriptor;
import consulo.index.io.Page;
import consulo.index.io.PersistentBTreeEnumerator;
import consulo.index.io.PersistentEnumeratorBase;
import consulo.project.Project;
import consulo.ide.impl.idea.openapi.util.io.FileUtil;
import consulo.util.lang.StringUtil;
import consulo.index.io.data.IOUtil;
import consulo.virtualFileSystem.VirtualFile;
import consulo.ide.impl.idea.util.PathUtil;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.versionControlSystem.log.VcsLogProvider;
import consulo.container.boot.ContainerPathManager;

import jakarta.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class PersistentUtil {
  @Nonnull
  public static final File LOG_CACHE = new File(ContainerPathManager.get().getSystemPath(), "vcs-log");
  @Nonnull
  private static final String CORRUPTION_MARKER = "corruption.marker";

  @Nonnull
  public static String calcLogId(@Nonnull Project project, @Nonnull Map<VirtualFile, VcsLogProvider> logProviders) {
    int hashcode = calcLogProvidersHash(logProviders);
    return project.getLocationHash() + "." + Integer.toHexString(hashcode);
  }

  private static int calcLogProvidersHash(@Nonnull final Map<VirtualFile, VcsLogProvider> logProviders) {
    List<VirtualFile> sortedRoots = ContainerUtil.sorted(logProviders.keySet(), Comparator.comparing(VirtualFile::getPath));
    return StringUtil.join(sortedRoots, root -> root.getPath() + "." + logProviders.get(root).getSupportedVcs().getName(), ".").hashCode();
  }

  @Nonnull
  public static File getStorageFile(@Nonnull String storageKind, @Nonnull String logId, int version) {
    File subdir = new File(LOG_CACHE, storageKind);
    String safeLogId = PathUtil.suggestFileName(logId, true, true);
    final File mapFile = new File(subdir, safeLogId + "." + version);
    if (!mapFile.exists()) {
      IOUtil.deleteAllFilesStartingWith(new File(subdir, safeLogId));
    }
    return mapFile;
  }

  public static void cleanupOldStorageFile(@Nonnull String storageKind, @Nonnull String logId) {
    File subdir = new File(LOG_CACHE, storageKind);
    String safeLogId = PathUtil.suggestFileName(logId, true, true);
    IOUtil.deleteAllFilesStartingWith(new File(subdir, safeLogId));

    File[] files = subdir.listFiles();
    if (files != null && files.length == 0) {
      subdir.delete();
    }
  }

  @Nonnull
  public static <T> PersistentEnumeratorBase<T> createPersistentEnumerator(@Nonnull KeyDescriptor<T> keyDescriptor,
                                                                           @Nonnull String storageKind,
                                                                           @Nonnull String logId,
                                                                           int version) throws IOException {
    File storageFile = getStorageFile(storageKind, logId, version);

    return IOUtil.openCleanOrResetBroken(() ->
                                                 new PersistentBTreeEnumerator<>(storageFile, keyDescriptor, Page.PAGE_SIZE, null, version),
                                         storageFile);
  }

  public static boolean deleteWithRenamingAllFilesStartingWith(@Nonnull File baseFile) {
    File parentFile = baseFile.getParentFile();
    if (parentFile == null) return false;

    File[] files = parentFile.listFiles(pathname -> pathname.getName().startsWith(baseFile.getName()));
    if (files == null) return true;

    boolean deleted = true;
    for (File f : files) {
      deleted &= FileUtil.deleteWithRenaming(f);
    }
    return deleted;
  }

  // this method cleans up all storage files for a project in a specified subdir
  // it assumes that these storage files all start with "safeLogId."
  // as method getStorageFile creates them
  // so these two methods should be changed in sync
  public static boolean cleanupStorageFiles(@Nonnull String subdirName, @Nonnull String id) {
    File subdir = new File(LOG_CACHE, subdirName);
    String safeLogId = PathUtil.suggestFileName(id, true, true);
    return deleteWithRenamingAllFilesStartingWith(new File(subdir, safeLogId + "."));
  }

  // do not forget to change cleanupStorageFiles method when editing this one
  @Nonnull
  public static File getStorageFile(@Nonnull String subdirName,
                                    @Nonnull String kind,
                                    @Nonnull String id,
                                    int version,
                                    boolean cleanupOldVersions) {
    File subdir = new File(LOG_CACHE, subdirName);
    String safeLogId = PathUtil.suggestFileName(id, true, true);
    File file = new File(subdir, safeLogId + "." + kind + "." + version);
    if (cleanupOldVersions && !file.exists()) {
      IOUtil.deleteAllFilesStartingWith(new File(subdir, safeLogId + "." + kind));
    }
    return file;
  }

  @Nonnull
  public static File getCorruptionMarkerFile() {
    return new File(LOG_CACHE, CORRUPTION_MARKER);
  }
}
