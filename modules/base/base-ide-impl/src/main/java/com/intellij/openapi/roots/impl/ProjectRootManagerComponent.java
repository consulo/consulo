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
package com.intellij.openapi.roots.impl;

import com.intellij.ProjectTopics;
import com.intellij.openapi.application.ApplicationListener;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.fileTypes.FileTypeEvent;
import com.intellij.openapi.fileTypes.FileTypeListener;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleScopeProvider;
import com.intellij.openapi.module.impl.scopes.ModuleScopeProviderImpl;
import com.intellij.openapi.project.DumbModeTask;
import com.intellij.openapi.project.DumbServiceImpl;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.WatchedRootsProvider;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.*;
import com.intellij.openapi.vfs.ex.VirtualFileManagerAdapter;
import com.intellij.openapi.vfs.newvfs.NewVirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointer;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerListener;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.FileBasedIndexImpl;
import com.intellij.util.indexing.FileBasedIndexProjectHandler;
import com.intellij.util.indexing.UnindexedFilesUpdater;
import com.intellij.util.messages.MessageBusConnection;
import consulo.annotation.access.RequiredWriteAction;
import consulo.components.impl.stores.BatchUpdateListener;
import consulo.logging.Logger;
import consulo.roots.ContentFolderScopes;
import consulo.roots.OrderEntryWithTracking;
import consulo.vfs.ArchiveFileSystem;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * ProjectRootManager extended with ability to watch events.
 */
@Singleton
public class ProjectRootManagerComponent extends ProjectRootManagerImpl implements ProjectComponent {
  private static final Logger LOG = Logger.getInstance(ProjectRootManagerComponent.class);
  private static final boolean LOG_CACHES_UPDATE = ApplicationManager.getApplication().isInternal() && !ApplicationManager.getApplication().isUnitTestMode();

  private boolean myPointerChangesDetected = false;
  private int myInsideRefresh = 0;
  private final BatchUpdateListener myHandler;
  private final MessageBusConnection myConnection;

  private Set<LocalFileSystem.WatchRequest> myRootsToWatch = new HashSet<LocalFileSystem.WatchRequest>();
  private final boolean myDoLogCachesUpdate;

  @Inject
  public ProjectRootManagerComponent(Project project, StartupManager startupManager) {
    super(project);

    myConnection = project.getMessageBus().connect(project);
    myConnection.subscribe(FileTypeManager.TOPIC, new FileTypeListener() {
      @Override
      public void beforeFileTypesChanged(@Nonnull FileTypeEvent event) {
        beforeRootsChange(true);
      }

      @Override
      public void fileTypesChanged(@Nonnull FileTypeEvent event) {
        rootsChanged(true);
      }
    });

    VirtualFileManager.getInstance().addVirtualFileManagerListener(new VirtualFileManagerAdapter() {
      @Override
      public void afterRefreshFinish(boolean asynchronous) {
        doUpdateOnRefresh();
      }
    }, project);

    if (!project.isDefault()) {
      startupManager.registerStartupActivity(() -> myStartupActivityPerformed = true);
    }

    myHandler = new BatchUpdateListener() {
      @Override
      public void onBatchUpdateStarted() {
        myRootsChanged.levelUp();
        myFileTypesChanged.levelUp();
      }

      @Override
      @RequiredWriteAction
      public void onBatchUpdateFinished() {
        myRootsChanged.levelDown();
        myFileTypesChanged.levelDown();
      }
    };

    myConnection.subscribe(VirtualFilePointerListener.TOPIC, new MyVirtualFilePointerListener());
    myDoLogCachesUpdate = ApplicationManager.getApplication().isInternal() && !ApplicationManager.getApplication().isUnitTestMode();

    myConnection.subscribe(BatchUpdateListener.TOPIC, myHandler);
  }

  @Override
  public void projectOpened() {
    addRootsToWatch();
    ApplicationManager.getApplication().addApplicationListener(new AppListener(), myProject);
  }

  @Override
  public void projectClosed() {
    LocalFileSystem.getInstance().removeWatchedRoots(myRootsToWatch);
  }

  @Override
  protected void addRootsToWatch() {
    final Pair<Set<String>, Set<String>> roots = getAllRoots(false);
    if (roots == null) return;
    myRootsToWatch = LocalFileSystem.getInstance().replaceWatchedRoots(myRootsToWatch, roots.first, roots.second);
  }

  @RequiredWriteAction
  private void beforeRootsChange(boolean fileTypes) {
    if (myProject.isDisposed()) return;
    getBatchSession(fileTypes).beforeRootsChanged();
  }

  @RequiredWriteAction
  private void rootsChanged(boolean fileTypes) {
    getBatchSession(fileTypes).rootsChanged();
  }

  private void doUpdateOnRefresh() {
    if (ApplicationManager.getApplication().isUnitTestMode() && (!myStartupActivityPerformed || myProject.isDisposed())) {
      return; // in test mode suppress addition to a queue unless project is properly initialized
    }
    if (myProject.isDefault()) {
      return;
    }

    if (myDoLogCachesUpdate) LOG.debug("refresh");
    DumbServiceImpl dumbService = DumbServiceImpl.getInstance(myProject);
    DumbModeTask task = FileBasedIndexProjectHandler.createChangedFilesIndexingTask(myProject);
    if (task != null) {
      dumbService.queueTask(task);
    }
  }

  private boolean affectsRoots(VirtualFilePointer[] pointers) {
    Pair<Set<String>, Set<String>> roots = getAllRoots(true);
    if (roots == null) return false;

    for (VirtualFilePointer pointer : pointers) {
      final String path = url2path(pointer.getUrl());
      if (roots.first.contains(path) || roots.second.contains(path)) return true;
    }

    return false;
  }

  @Override
  protected void fireBeforeRootsChangeEvent(boolean fileTypes) {
    isFiringEvent = true;
    try {
      myProject.getMessageBus().syncPublisher(ProjectTopics.PROJECT_ROOTS).beforeRootsChange(new ModuleRootEventImpl(myProject, fileTypes));
    }
    finally {
      isFiringEvent = false;
    }
  }

  @Override
  protected void fireRootsChangedEvent(boolean fileTypes) {
    isFiringEvent = true;
    try {
      myProject.getMessageBus().syncPublisher(ProjectTopics.PROJECT_ROOTS).rootsChanged(new ModuleRootEventImpl(myProject, fileTypes));
    }
    finally {
      isFiringEvent = false;
    }
  }

  private static String url2path(String url) {
    String path = VfsUtilCore.urlToPath(url);

    int separatorIndex = path.indexOf(StandardFileSystems.JAR_SEPARATOR);
    if (separatorIndex < 0) return path;
    return path.substring(0, separatorIndex);
  }

  @Nullable
  private Pair<Set<String>, Set<String>> getAllRoots(boolean includeSourceRoots) {
    if (myProject.isDefault()) return null;

    final Set<String> recursive = new HashSet<String>();
    final Set<String> flat = new HashSet<String>();

    final String projectFilePath = myProject.getProjectFilePath();
    final File projectDirFile = projectFilePath == null ? null : new File(projectFilePath).getParentFile();
    if (projectDirFile != null && projectDirFile.getName().equals(Project.DIRECTORY_STORE_FOLDER)) {
      recursive.add(projectDirFile.getAbsolutePath());
    }
    else {
      flat.add(projectFilePath);
      final VirtualFile workspaceFile = myProject.getWorkspaceFile();
      if (workspaceFile != null) {
        flat.add(workspaceFile.getPath());
      }
    }

    for (WatchedRootsProvider extension : WatchedRootsProvider.EP_NAME.getExtensionList(myProject)) {
      recursive.addAll(extension.getRootsToWatch());
    }

    addRootsFromModules(includeSourceRoots, recursive, flat);

    return Pair.create(recursive, flat);
  }

  private void addRootsFromModules(boolean includeSourceRoots, Set<String> recursive, Set<String> flat) {
    final Module[] modules = ModuleManager.getInstance(myProject).getModules();
    for (Module module : modules) {
      final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);

      addRootsToTrack(moduleRootManager.getContentRootUrls(), recursive, flat);

      if (includeSourceRoots) {
        addRootsToTrack(moduleRootManager.getContentFolderUrls(ContentFolderScopes.all(false)), recursive, flat);
      }

      final OrderEntry[] orderEntries = moduleRootManager.getOrderEntries();
      for (OrderEntry entry : orderEntries) {
        if (entry instanceof OrderEntryWithTracking) {
          for (OrderRootType orderRootType : OrderRootType.getAllTypes()) {
            addRootsToTrack(entry.getUrls(orderRootType), recursive, flat);
          }
        }
      }
    }
  }

  private static void addRootsToTrack(final String[] urls, final Collection<String> recursive, final Collection<String> flat) {
    for (String url : urls) {
      if (url != null) {
        final String protocol = VirtualFileManager.extractProtocol(url);
        if (protocol == null || LocalFileSystem.PROTOCOL.equals(protocol)) {
          recursive.add(extractLocalPath(url));
        }
        else {
          VirtualFileSystem fileSystem = VirtualFileManager.getInstance().getFileSystem(protocol);
          if (fileSystem instanceof ArchiveFileSystem) {
            flat.add(extractLocalPath(url));
          }
        }
      }
    }
  }

  @Override
  protected void doSynchronizeRoots() {
    if (!myStartupActivityPerformed) return;

    if (myDoLogCachesUpdate) {
      LOG.debug(new Throwable("sync roots"));
    }
    else if (!ApplicationManager.getApplication().isUnitTestMode()) LOG.info("project roots have changed");

    DumbServiceImpl dumbService = DumbServiceImpl.getInstance(myProject);
    if (FileBasedIndex.getInstance() instanceof FileBasedIndexImpl) {
      dumbService.queueTask(new UnindexedFilesUpdater(myProject));
    }
  }

  @Override
  public void markRootsForRefresh() {
    Set<String> paths = ContainerUtil.newTroveSet(FileUtil.PATH_HASHING_STRATEGY);
    addRootsFromModules(false, paths, paths);

    LocalFileSystem fs = LocalFileSystem.getInstance();
    for (String path : paths) {
      VirtualFile root = fs.findFileByPath(path);
      if (root instanceof NewVirtualFile) {
        ((NewVirtualFile)root).markDirtyRecursively();
      }
    }
  }

  @Override
  protected void clearScopesCaches() {
    super.clearScopesCaches();
    LibraryScopeCache.getInstance(myProject).clear();
  }

  @Override
  public void clearScopesCachesForModules() {
    super.clearScopesCachesForModules();
    if (!myProject.isModulesReady()) {
      return;
    }
    
    Module[] modules = ModuleManager.getInstance(myProject).getModules();
    for (Module module : modules) {
      ModuleScopeProvider scopeProvider = ModuleScopeProvider.getInstance(module);
      if(scopeProvider instanceof ModuleScopeProviderImpl) {
        ((ModuleScopeProviderImpl)scopeProvider).clearCache();
      }
    }
  }

  private class AppListener implements ApplicationListener {
    @Override
    public void beforeWriteActionStart(@Nonnull Object action) {
      myInsideRefresh++;
    }

    @Override
    public void writeActionFinished(@Nonnull Object action) {
      if (--myInsideRefresh == 0) {
        if (myPointerChangesDetected) {
          myPointerChangesDetected = false;
          myProject.getMessageBus().syncPublisher(ProjectTopics.PROJECT_ROOTS).rootsChanged(new ModuleRootEventImpl(myProject, false));

          doSynchronizeRoots();

          addRootsToWatch();
        }
      }
    }
  }

  private class MyVirtualFilePointerListener implements VirtualFilePointerListener {
    @Override
    public void beforeValidityChanged(@Nonnull VirtualFilePointer[] pointers) {
      if (!myProject.isDisposed()) {
        if (myInsideRefresh == 0) {
          if (affectsRoots(pointers)) {
            beforeRootsChange(false);
            if (myDoLogCachesUpdate) LOG.debug(new Throwable(pointers.length > 0 ? pointers[0].getPresentableUrl() : ""));
          }
        }
        else if (!myPointerChangesDetected) {
          //this is the first pointer changing validity
          if (affectsRoots(pointers)) {
            myPointerChangesDetected = true;
            myProject.getMessageBus().syncPublisher(ProjectTopics.PROJECT_ROOTS).beforeRootsChange(new ModuleRootEventImpl(myProject, false));
            if (myDoLogCachesUpdate) LOG.debug(new Throwable(pointers.length > 0 ? pointers[0].getPresentableUrl() : ""));
          }
        }
      }
    }

    @Override
    public void validityChanged(@Nonnull VirtualFilePointer[] pointers) {
      if (!myProject.isDisposed()) {
        if (myInsideRefresh > 0) {
          clearScopesCaches();
        }
        else if (affectsRoots(pointers)) {
          rootsChanged(false);
        }
      }
    }
  }

  private final VirtualFilePointerListener myRootsChangedListener = new VirtualFilePointerListener() {
    @Override
    public void beforeValidityChanged(@Nonnull VirtualFilePointer[] pointers) {
      if (myProject.isDisposed()) {
        return;
      }

      if (myInsideRefresh == 0) {
        beforeRootsChange(false);
        if (LOG_CACHES_UPDATE || LOG.isDebugEnabled()) {
          LOG.debug(new Throwable(pointers.length > 0 ? pointers[0].getPresentableUrl() : ""));
        }
      }
      else if (!myPointerChangesDetected) {
        //this is the first pointer changing validity
        myPointerChangesDetected = true;
        myProject.getMessageBus().syncPublisher(ProjectTopics.PROJECT_ROOTS).beforeRootsChange(new ModuleRootEventImpl(myProject, false));
        if (LOG_CACHES_UPDATE || LOG.isDebugEnabled()) {
          LOG.debug(new Throwable(pointers.length > 0 ? pointers[0].getPresentableUrl() : ""));
        }
      }
    }

    @Override
    public void validityChanged(@Nonnull VirtualFilePointer[] pointers) {
      if (myProject.isDisposed()) {
        return;
      }

      if (myInsideRefresh > 0) {
        clearScopesCaches();
      }
      else {
        rootsChanged(false);
      }
    }
  };

  @Nonnull
  @Override
  public VirtualFilePointerListener getRootsValidityChangedListener() {
    return myRootsChangedListener;
  }
}
