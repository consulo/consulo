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
import consulo.versionControlSystem.VcsDirectoryMapping;
import consulo.versionControlSystem.internal.VcsMappingResolver;
import consulo.virtualFileSystem.FileWatchService;

import java.nio.file.Path;
import java.util.*;

/**
* @author irengrig
*/
public class FileWatchRequestModifier implements Runnable {
  private static final Logger LOG = Logger.getInstance(FileWatchRequestModifier.class);

  private final Project myProject;
  private final NewMappings myNewMappings;
  private final Map<VcsDirectoryMapping, FileWatchService.WatchRequest> myDirectoryMappingWatches;
  private final FileWatchService myWatchService;

  public FileWatchRequestModifier(Project project, NewMappings newMappings, FileWatchService watchService) {
    myWatchService = watchService;
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

    // the directories are of the machine of the project
    Map<Path, VcsDirectoryMapping> toAdd = new HashMap<>();
    for (VcsDirectoryMapping mapping : added) {
      if (!mapping.isDefaultMapping()) {
        Path directory = VcsMappingResolver.toNioPath(myProject, mapping.getDirectory());
        if (directory != null) {
          toAdd.put(directory.normalize(), mapping);
        }
      }
    }

    Collection<FileWatchService.WatchRequest> toRemove = new LinkedList<>();
    for (VcsDirectoryMapping mapping : deleted) {
      if (mapping.isDefaultMapping()) continue;
      FileWatchService.WatchRequest removed = myDirectoryMappingWatches.remove(mapping);
      if (removed != null) {
        toRemove.add(removed);
      }
    }

    Set<FileWatchService.WatchRequest> requests = myWatchService.replaceWatchedRoots(toRemove, toAdd.keySet(), null);
    for (FileWatchService.WatchRequest request : requests) {
      VcsDirectoryMapping mapping = toAdd.get(request.getRoot());
      if (mapping != null) {
        myDirectoryMappingWatches.put(mapping, request);
      }
      else {
        LOG.error("root=" + request.getRoot() + " toAdd=" + toAdd.keySet());
      }
    }
  }
}
