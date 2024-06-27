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
package consulo.versionControlSystem.impl.internal.change;

import consulo.project.Project;
import consulo.util.collection.ContainerUtil;
import consulo.util.io.FileUtil;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.change.FilePathsHelper;
import consulo.versionControlSystem.change.IgnoreSettingsType;
import consulo.versionControlSystem.change.IgnoredBeanFactory;
import consulo.versionControlSystem.change.IgnoredFileBean;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.event.BulkFileListener;
import consulo.virtualFileSystem.event.VFileContentChangeEvent;
import consulo.virtualFileSystem.event.VFileEvent;
import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class IgnoredFilesComponent {
  private final Set<IgnoredFileBean> myFilesToIgnore;
  private final Map<String, IgnoredFileBean> myFilesMap;
  private final Set<String> myDirectoriesManuallyRemovedFromIgnored;
  private final ReadWriteLock myLock = new ReentrantReadWriteLock();
  private final Lock myReadLock = myLock.readLock();
  private final Lock myWriteLock = myLock.writeLock();
  private final Project myProject;

  public IgnoredFilesComponent(@Nonnull Project project, final boolean registerListener) {
    myProject = project;
    myFilesToIgnore = new LinkedHashSet<>();
    myFilesMap = new HashMap<>();

    if (registerListener) {
      project.getMessageBus().connect(project).subscribe(BulkFileListener.class, new BulkFileListener() {
        @Override
        public void after(@Nonnull List<? extends VFileEvent> events) {
          if (hasSignificantChanges(events)) {
            resetCaches();
          }
        }

        private <T extends VFileEvent> boolean hasSignificantChanges(List<T> events) {
          return ContainerUtil.exists(events, event -> !(event instanceof VFileContentChangeEvent));
        }
      });
    }
    myDirectoriesManuallyRemovedFromIgnored = new HashSet<>();
  }

  public IgnoredFilesComponent(@Nonnull IgnoredFilesComponent other) {
    myProject = other.myProject;
    myFilesToIgnore = new LinkedHashSet<>(other.myFilesToIgnore);
    myFilesMap = new HashMap<>(other.myFilesMap);
    myDirectoriesManuallyRemovedFromIgnored = new HashSet<>(other.myDirectoriesManuallyRemovedFromIgnored);
  }

  public void add(final IgnoredFileBean... filesToIgnore) {
    myWriteLock.lock();
    try {
      Collections.addAll(myFilesToIgnore, filesToIgnore);
      addIgnoredFiles(filesToIgnore);
    }
    finally {
      myWriteLock.unlock();
    }
  }

  public Set<String> getDirectoriesManuallyRemovedFromIgnored() {
    return Collections.unmodifiableSet(myDirectoriesManuallyRemovedFromIgnored);
  }

  public void setDirectoriesManuallyRemovedFromIgnored(Set<String> directories) {
    myDirectoriesManuallyRemovedFromIgnored.clear();
    myDirectoriesManuallyRemovedFromIgnored.addAll(directories);
  }

  public void addIgnoredDirectoryImplicitly(@Nonnull String path, @Nonnull Project project) {
    myWriteLock.lock();
    try {
      if (myDirectoriesManuallyRemovedFromIgnored.contains(path) || myDirectoriesManuallyRemovedFromIgnored.contains(path + "/")) {
        return;
      }
      for (IgnoredFileBean bean : myFilesToIgnore) {
        if (bean.getType() == IgnoreSettingsType.UNDER_DIR && FileUtil.isAncestor(bean.getPath(), path, false)) {
          return;
        }
      }
      List<IgnoredFileBean> toRemove = new ArrayList<>();
      for (IgnoredFileBean bean : myFilesToIgnore) {
        if ((bean.getType() == IgnoreSettingsType.UNDER_DIR || bean.getType() == IgnoreSettingsType.FILE) &&
          FileUtil.isAncestor(path, bean.getPath(), false)) {
          toRemove.add(bean);
        }
      }
      myFilesToIgnore.removeAll(toRemove);
      myFilesToIgnore.add(IgnoredBeanFactory.ignoreUnderDirectory(path, project));
    }
    finally {
      myWriteLock.unlock();
    }
  }

  private void addIgnoredFiles(@Nonnull IgnoredFileBean[] filesToIgnore) {
    for (IgnoredFileBean bean : filesToIgnore) {
      if (IgnoreSettingsType.FILE.equals(bean.getType())) {
        final Project project = bean.getProject();
        final VirtualFile baseDir = project.getBaseDir();
        if (baseDir != null) {
          // if baseDir == null, then nothing will be added to map, but check will still be correct through set
          myFilesMap.put(FilePathsHelper.convertPath(baseDir.getPath(), bean.getPath()), bean);
        }
      }
    }
  }

  public void clear() {
    myWriteLock.lock();
    try {
      myFilesToIgnore.clear();
      myFilesMap.clear();
    }
    finally {
      myWriteLock.unlock();
    }
  }

  public boolean isEmpty() {
    myReadLock.lock();
    try {
      return myFilesToIgnore.isEmpty();
    }
    finally {
      myReadLock.unlock();
    }
  }

  public void set(final IgnoredFileBean... filesToIgnore) {
    myWriteLock.lock();
    try {
      myFilesToIgnore.clear();
      Collections.addAll(myFilesToIgnore, filesToIgnore);
      myFilesMap.clear();
      addIgnoredFiles(filesToIgnore);
    }
    finally {
      myWriteLock.unlock();
    }
  }

  @Nonnull
  public IgnoredFileBean[] getFilesToIgnore() {
    myReadLock.lock();
    try {
      return myFilesToIgnore.toArray(new IgnoredFileBean[myFilesToIgnore.size()]);
    }
    finally {
      myReadLock.unlock();
    }
  }

  private void resetCaches() {
    myWriteLock.lock();
    try {
      for (IgnoredFileBean bean : myFilesToIgnore) {
        bean.resetCache();
      }
    }
    finally {
      myWriteLock.unlock();
    }
  }

  public boolean isIgnoredFile(@Nonnull FilePath filePath) {
    myReadLock.lock();
    try {
      if (myFilesToIgnore.isEmpty()) return false;

      final String path = FilePathsHelper.convertPath(filePath);
      final IgnoredFileBean fileBean = myFilesMap.get(path);

      VirtualFile file = filePath.getVirtualFile();
      if (file == null) {
        return false;
      }

      if (fileBean != null && fileBean.matchesFile(file)) return true;

      for (IgnoredFileBean bean : myFilesToIgnore) {
        if (bean.matchesFile(file)) return true;
      }
      return false;
    }
    finally {
      myReadLock.unlock();
    }
  }
}
