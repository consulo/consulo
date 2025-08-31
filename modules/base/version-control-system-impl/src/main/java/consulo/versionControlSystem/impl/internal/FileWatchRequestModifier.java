/*
 * Copyright 2000-2012 JetBrains s.r.o.
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
package consulo.versionControlSystem.impl.internal;

import consulo.logging.Logger;
import consulo.project.Project;
import consulo.util.collection.Maps;
import consulo.util.io.FileUtil;
import consulo.versionControlSystem.VcsDirectoryMapping;
import consulo.virtualFileSystem.LocalFileSystem;

import java.util.*;

/**
* @author irengrig
*/
public class FileWatchRequestModifier implements Runnable {
  private static final Logger LOG = Logger.getInstance(FileWatchRequestModifier.class);

  private final Project myProject;
  private final NewMappings myNewMappings;
  private final Map<VcsDirectoryMapping, LocalFileSystem.WatchRequest> myDirectoryMappingWatches;
  private final LocalFileSystem myLfs;

  public FileWatchRequestModifier(Project project, NewMappings newMappings, LocalFileSystem localFileSystem) {
    myLfs = localFileSystem;
    myProject = project;
    myNewMappings = newMappings;
    myDirectoryMappingWatches = new HashMap<>();
  }

  @Override
  public void run() {
    if (!myProject.isInitialized() || myProject.isDisposed()) return;
    List<VcsDirectoryMapping> copy = myNewMappings.getDirectoryMappings();

    List<VcsDirectoryMapping> added = new ArrayList<>(copy);
    added.removeAll(myDirectoryMappingWatches.keySet());

    List<VcsDirectoryMapping> deleted = new ArrayList<>(myDirectoryMappingWatches.keySet());
    deleted.removeAll(copy);

    Map<String, VcsDirectoryMapping> toAdd = Maps.newHashMap(FileUtil.PATH_HASHING_STRATEGY);
    for (VcsDirectoryMapping mapping : added) {
      if (!mapping.isDefaultMapping()) {
        toAdd.put(FileUtil.toCanonicalPath(mapping.getDirectory()), mapping);
      }
    }

    Collection<LocalFileSystem.WatchRequest> toRemove = new LinkedList<>();
    for (VcsDirectoryMapping mapping : deleted) {
      if (mapping.isDefaultMapping()) continue;
      LocalFileSystem.WatchRequest removed = myDirectoryMappingWatches.remove(mapping);
      if (removed != null) {
        toRemove.add(removed);
      }
    }

    Set<LocalFileSystem.WatchRequest> requests = myLfs.replaceWatchedRoots(toRemove, toAdd.keySet(), null);
    for (LocalFileSystem.WatchRequest request : requests) {
      VcsDirectoryMapping mapping = toAdd.get(request.getRootPath());
      if (mapping != null) {
        myDirectoryMappingWatches.put(mapping, request);
      }
      else {
        LOG.error("root=" + request.getRootPath() + " toAdd=" + toAdd.keySet());
      }
    }
  }
}
