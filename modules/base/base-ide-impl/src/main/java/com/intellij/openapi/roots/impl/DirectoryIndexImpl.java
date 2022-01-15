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
package com.intellij.openapi.roots.impl;

import com.intellij.ProjectTopics;
import com.intellij.openapi.fileTypes.FileTypeEvent;
import com.intellij.openapi.fileTypes.FileTypeListener;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.util.LowMemoryWatcher;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.Query;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import consulo.annotation.access.RequiredReadAction;
import consulo.disposer.Disposable;
import consulo.logging.Logger;
import consulo.roots.ContentFolderTypeProvider;
import consulo.util.collection.primitive.ints.ConcurrentIntObjectMap;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

@Singleton
public class DirectoryIndexImpl extends DirectoryIndex implements Disposable  {
  private static final Logger LOG = Logger.getInstance(DirectoryIndexImpl.class);

  private final Project myProject;
  private final MessageBusConnection myConnection;

  private volatile boolean myDisposed = false;
  private volatile RootIndex myRootIndex = null;

  @Inject
  @RequiredReadAction
  public DirectoryIndexImpl(@Nonnull Project project) {
    myProject = project;
    myConnection = project.getMessageBus().connect(project);

    if (myProject.isDefault()) {
      return;
    }

    myConnection.subscribe(FileTypeManager.TOPIC, new FileTypeListener() {
      @Override
      public void fileTypesChanged(@Nonnull FileTypeEvent event) {
        myRootIndex = null;
      }
    });

    myConnection.subscribe(ProjectTopics.PROJECT_ROOTS, new ModuleRootListener() {
      @Override
      public void rootsChanged(ModuleRootEvent event) {
        myRootIndex = null;
      }
    });

    myConnection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@Nonnull List<? extends VFileEvent> events) {
        RootIndex rootIndex = myRootIndex;
        if (rootIndex != null && rootIndex.resetOnEvents(events)) {
          myRootIndex = null;
        }
      }
    });

    LowMemoryWatcher.register(() -> {
      RootIndex index = myRootIndex;
      if (index != null) {
        index.onLowMemory();
      }
    }, project);
  }

  private void dispatchPendingEvents() {
    myConnection.deliverImmediately();
  }

  @Override
  public void dispose() {
    myDisposed = true;
    myRootIndex = null;
  }

  @Override
  @Nonnull
  public Query<VirtualFile> getDirectoriesByPackageName(@Nonnull String packageName, boolean includeLibrarySources) {
    return getRootIndex().getDirectoriesByPackageName(packageName, includeLibrarySources);
  }

  @Nonnull
  private RootIndex getRootIndex() {
    RootIndex rootIndex = myRootIndex;
    if (rootIndex == null) {
      myRootIndex = rootIndex = new RootIndex(myProject, createRootInfoCache());
    }
    return rootIndex;
  }

  protected RootIndex.InfoCache createRootInfoCache() {
    return new RootIndex.InfoCache() {
      // Upsource can't use int-mapping because different files may have the same id there
      private final ConcurrentIntObjectMap<DirectoryInfo> myInfoCache = ContainerUtil.createConcurrentIntObjectMap();

      @Override
      public void cacheInfo(@Nonnull VirtualFile dir, @Nonnull DirectoryInfo info) {
        myInfoCache.put(((NewVirtualFile)dir).getId(), info);
      }

      @Override
      public DirectoryInfo getCachedInfo(@Nonnull VirtualFile dir) {
        return myInfoCache.get(((NewVirtualFile)dir).getId());
      }
    };
  }

  @Override
  public DirectoryInfo getInfoForDirectory(@Nonnull VirtualFile dir) {
    DirectoryInfo info = getInfoForFile(dir);
    return info.isInProject(dir) ? info : null;
  }

  @Nonnull
  @Override
  public DirectoryInfo getInfoForFile(@Nonnull VirtualFile file) {
    checkAvailability();
    dispatchPendingEvents();

    if (!(file instanceof NewVirtualFile)) return NonProjectDirectoryInfo.NOT_SUPPORTED_VIRTUAL_FILE_IMPLEMENTATION;

    return getRootIndex().getInfoForFile(file);
  }

  @Override
  @Nullable
  public ContentFolderTypeProvider getContentFolderType(@Nonnull DirectoryInfo info) {
    if (info.isInModuleSource()) {
      return getRootIndex().getContentFolderType(info);
    }
    return null;
  }

  @Override
  public String getPackageName(@Nonnull VirtualFile dir) {
    checkAvailability();
    if (!(dir instanceof NewVirtualFile)) return null;

    return getRootIndex().getPackageName(dir);
  }

  @Nonnull
  @Override
  public OrderEntry[] getOrderEntries(@Nonnull DirectoryInfo info) {
    checkAvailability();
    return getRootIndex().getOrderEntries(info);
  }

  private void checkAvailability() {
    if (myDisposed) {
      ProgressManager.checkCanceled();
      LOG.error("Directory index is already disposed for " + myProject);
    }
  }
}
