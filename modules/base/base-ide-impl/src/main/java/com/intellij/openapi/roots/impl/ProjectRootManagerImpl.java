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

package com.intellij.openapi.roots.impl;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.*;
import com.intellij.openapi.roots.ex.ProjectRootManagerEx;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.roots.libraries.LibraryTable;
import com.intellij.openapi.util.EmptyRunnable;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.pointers.VirtualFilePointerListener;
import com.intellij.psi.PsiManager;
import com.intellij.util.containers.ContainerUtil;
import consulo.annotation.access.RequiredWriteAction;
import consulo.logging.Logger;
import consulo.roots.ContentFolderScopes;
import consulo.roots.OrderEntryWithTracking;
import consulo.vfs.ArchiveFileSystem;

import javax.annotation.Nonnull;
import java.util.*;

/**
 * @author max
 */
public class ProjectRootManagerImpl extends ProjectRootManagerEx {
  private static final Logger LOG = Logger.getInstance(ProjectRootManagerImpl.class);

  protected final Project myProject;

  private final OrderRootsCache myRootsCache;

  private final Map<RootProvider, Set<OrderEntry>> myRegisteredRootProviders = new HashMap<RootProvider, Set<OrderEntry>>();
  protected final List<OrderEntryWithTracking> myModuleExtensionWithSdkOrderEntries = ContainerUtil.newArrayList();
  protected boolean myStartupActivityPerformed = false;
  private final RootProviderChangeListener myRootProviderChangeListener = new RootProviderChangeListener();
  private final VirtualFilePointerListener myRootsValidityChangedListener = new VirtualFilePointerListener() {
  };

  protected class BatchSession {
    private int myBatchLevel = 0;
    private boolean myChanged = false;

    private final boolean myFileTypes;

    private BatchSession(final boolean fileTypes) {
      myFileTypes = fileTypes;
    }

    protected void levelUp() {
      if (myBatchLevel == 0) {
        myChanged = false;
      }
      myBatchLevel += 1;
    }

    @RequiredWriteAction
    protected void levelDown() {
      myBatchLevel -= 1;
      if (myChanged && myBatchLevel == 0) {
        try {
          fireChange();
        }
        finally {
          myChanged = false;
        }
      }
    }

    @RequiredWriteAction
    private boolean fireChange() {
      return fireRootsChanged(myFileTypes);
    }

    @RequiredWriteAction
    protected void beforeRootsChanged() {
      if (myBatchLevel == 0 || !myChanged) {
        if (fireBeforeRootsChanged(myFileTypes)) {
          myChanged = true;
        }
      }
    }

    @RequiredWriteAction
    protected void rootsChanged() {
      if (myBatchLevel == 0) {
        if (fireChange()) {
          myChanged = false;
        }
      }
    }
  }

  private class RootProviderChangeListener implements RootProvider.RootSetChangedListener {
    private boolean myInsideRootsChange;

    @Override
    public void rootSetChanged(final RootProvider wrapper) {
      if (myInsideRootsChange) return;
      myInsideRootsChange = true;
      try {
        makeRootsChange(EmptyRunnable.INSTANCE, false, true);
      }
      finally {
        myInsideRootsChange = false;
      }
    }
  }

  protected final BatchSession myRootsChanged = new BatchSession(false);
  protected final BatchSession myFileTypesChanged = new BatchSession(true);

  public static ProjectRootManagerImpl getInstanceImpl(Project project) {
    return (ProjectRootManagerImpl)getInstance(project);
  }

  public ProjectRootManagerImpl(Project project) {
    myProject = project;
    myRootsCache = new OrderRootsCache(project);
  }

  @Override
  @Nonnull
  public ProjectFileIndex getFileIndex() {
    return ProjectFileIndex.getInstance(myProject);
  }

  @Override
  @Nonnull
  public List<String> getContentRootUrls() {
    final List<String> result = new ArrayList<String>();
    for (Module module : getModuleManager().getModules()) {
      final String[] urls = ModuleRootManager.getInstance(module).getContentRootUrls();
      ContainerUtil.addAll(result, urls);
    }
    return result;
  }

  @Override
  @Nonnull
  public VirtualFile[] getContentRoots() {
    final List<VirtualFile> result = new ArrayList<VirtualFile>();
    for (Module module : getModuleManager().getModules()) {
      final VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();
      ContainerUtil.addAll(result, contentRoots);
    }
    return VfsUtilCore.toVirtualFileArray(result);
  }

  @Override
  public VirtualFile[] getContentSourceRoots() {
    final List<VirtualFile> result = new ArrayList<VirtualFile>();
    for (Module module : getModuleManager().getModules()) {
      final VirtualFile[] sourceRoots = ModuleRootManager.getInstance(module).getContentFolderFiles(ContentFolderScopes.all(false));
      ContainerUtil.addAll(result, sourceRoots);
    }
    return VfsUtilCore.toVirtualFileArray(result);
  }

  @Nonnull
  @Override
  public OrderEnumerator orderEntries() {
    return new ProjectOrderEnumerator(myProject, myRootsCache);
  }

  @Nonnull
  @Override
  public OrderEnumerator orderEntries(@Nonnull Collection<? extends Module> modules) {
    return new ModulesOrderEnumerator(myProject, modules);
  }

  @Override
  public VirtualFile[] getContentRootsFromAllModules() {
    List<VirtualFile> result = new ArrayList<VirtualFile>();
    final Module[] modules = getModuleManager().getSortedModules();
    for (Module module : modules) {
      final VirtualFile[] files = ModuleRootManager.getInstance(module).getContentRoots();
      ContainerUtil.addAll(result, files);
    }
    result.add(myProject.getBaseDir());
    return VfsUtilCore.toVirtualFileArray(result);
  }

  private boolean myMergedCallStarted = false;
  private boolean myMergedCallHasRootChange = false;
  private int myRootsChangesDepth = 0;

  @Override
  public void mergeRootsChangesDuring(@Nonnull Runnable runnable) {
    if (getBatchSession(false).myBatchLevel == 0 && !myMergedCallStarted) {
      LOG.assertTrue(myRootsChangesDepth == 0, "Merged rootsChanged not allowed inside rootsChanged, rootsChanged level == " + myRootsChangesDepth);
      myMergedCallStarted = true;
      myMergedCallHasRootChange = false;
      try {
        runnable.run();
      }
      finally {
        if (myMergedCallHasRootChange) {
          LOG.assertTrue(myRootsChangesDepth == 1, "myMergedCallDepth = " + myRootsChangesDepth);
          getBatchSession(false).rootsChanged();
        }
        myMergedCallStarted = false;
        myMergedCallHasRootChange = false;
      }
    }
    else {
      runnable.run();
    }
  }

  protected void clearScopesCaches() {
    clearScopesCachesForModules();
  }

  @Override
  public void clearScopesCachesForModules() {
    myRootsCache.clearCache();
    if(!myProject.isModulesReady()) {
      return;
    }
    Module[] modules = ModuleManager.getInstance(myProject).getModules();
    for (Module module : modules) {
      ((ModuleRootManagerImpl)ModuleRootManager.getInstance(module)).dropCaches();
    }
  }

  @Override
  public void makeRootsChange(@Nonnull Runnable runnable, boolean filetypes, boolean fireEvents) {
    if (myProject.isDisposed()) return;
    BatchSession session = getBatchSession(filetypes);
    if (fireEvents) session.beforeRootsChanged();
    try {
      runnable.run();
    }
    finally {
      if (fireEvents) session.rootsChanged();
    }
  }

  protected BatchSession getBatchSession(final boolean filetypes) {
    return filetypes ? myFileTypesChanged : myRootsChanged;
  }

  protected boolean isFiringEvent = false;

  @RequiredWriteAction
  private boolean fireBeforeRootsChanged(boolean filetypes) {
    ApplicationManager.getApplication().assertWriteAccessAllowed();

    LOG.assertTrue(!isFiringEvent, "Do not use API that changes roots from roots events. Try using invoke later or something else.");

    if (myMergedCallStarted) {
      LOG.assertTrue(!filetypes, "Filetypes change is not supported inside merged call");
    }

    if (myRootsChangesDepth++ == 0) {
      if (myMergedCallStarted) {
        myMergedCallHasRootChange = true;
        myRootsChangesDepth++; // blocks all firing until finishRootsChangedOnDemand
      }
      fireBeforeRootsChangeEvent(filetypes);
      return true;
    }

    return false;
  }

  protected void fireBeforeRootsChangeEvent(boolean filetypes) {
  }

  @RequiredWriteAction
  private boolean fireRootsChanged(boolean filetypes) {
    if (myProject.isDisposed()) return false;

    ApplicationManager.getApplication().assertWriteAccessAllowed();

    LOG.assertTrue(!isFiringEvent, "Do not use API that changes roots from roots events. Try using invoke later or something else.");

    if (myMergedCallStarted) {
      LOG.assertTrue(!filetypes, "Filetypes change is not supported inside merged call");
    }

    myRootsChangesDepth--;
    if (myRootsChangesDepth > 0) return false;

    clearScopesCaches();

    incModificationCount();

    PsiManager.getInstance(myProject).dropPsiCaches();

    fireRootsChangedEvent(filetypes);

    doSynchronizeRoots();

    addRootsToWatch();

    return true;
  }

  protected void fireRootsChangedEvent(boolean filetypes) {
  }

  protected void addRootsToWatch() {
  }

  public Project getProject() {
    return myProject;
  }

  protected void doSynchronizeRoots() {
  }

  public void markRootsForRefresh() {
  }

  public static String extractLocalPath(final String url) {
    final String path = VfsUtilCore.urlToPath(url);
    final int jarSeparatorIndex = path.indexOf(ArchiveFileSystem.ARCHIVE_SEPARATOR);
    if (jarSeparatorIndex > 0) {
      return path.substring(0, jarSeparatorIndex);
    }
    return path;
  }

  private ModuleManager getModuleManager() {
    return ModuleManager.getInstance(myProject);
  }

  void subscribeToRootProvider(OrderEntry owner, final RootProvider provider) {
    Set<OrderEntry> owners = myRegisteredRootProviders.get(provider);
    if (owners == null) {
      owners = new HashSet<OrderEntry>();
      myRegisteredRootProviders.put(provider, owners);
      provider.addRootSetChangedListener(myRootProviderChangeListener);
    }
    owners.add(owner);
  }

  void unsubscribeFromRootProvider(OrderEntry owner, final RootProvider provider) {
    Set<OrderEntry> owners = myRegisteredRootProviders.get(provider);
    if (owners != null) {
      owners.remove(owner);
      if (owners.isEmpty()) {
        provider.removeRootSetChangedListener(myRootProviderChangeListener);
        myRegisteredRootProviders.remove(provider);
      }
    }
  }

  void addListenerForTable(LibraryTable.Listener libraryListener, final LibraryTable libraryTable) {
    LibraryTableMultilistener multilistener = myLibraryTableMultilisteners.get(libraryTable);
    if (multilistener == null) {
      multilistener = new LibraryTableMultilistener(libraryTable);
    }
    multilistener.addListener(libraryListener);
  }

  public void addOrderWithTracking(@Nonnull OrderEntryWithTracking orderEntry) {
    myModuleExtensionWithSdkOrderEntries.add(orderEntry);
  }

  public void removeOrderWithTracking(@Nonnull OrderEntryWithTracking orderEntry) {
    myModuleExtensionWithSdkOrderEntries.remove(orderEntry);
  }

  void removeListenerForTable(LibraryTable.Listener libraryListener, final LibraryTable libraryTable) {
    LibraryTableMultilistener multilistener = myLibraryTableMultilisteners.get(libraryTable);
    if (multilistener == null) {
      multilistener = new LibraryTableMultilistener(libraryTable);
    }
    multilistener.removeListener(libraryListener);
  }

  private final Map<LibraryTable, LibraryTableMultilistener> myLibraryTableMultilisteners = new HashMap<LibraryTable, LibraryTableMultilistener>();

  private class LibraryTableMultilistener implements LibraryTable.Listener {
    final List<LibraryTable.Listener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();
    private final LibraryTable myLibraryTable;

    private LibraryTableMultilistener(LibraryTable libraryTable) {
      myLibraryTable = libraryTable;
      myLibraryTable.addListener(this);
      myLibraryTableMultilisteners.put(myLibraryTable, this);
    }

    private void addListener(LibraryTable.Listener listener) {
      myListeners.add(listener);
    }

    private void removeListener(LibraryTable.Listener listener) {
      myListeners.remove(listener);
      if (myListeners.isEmpty()) {
        myLibraryTable.removeListener(this);
        myLibraryTableMultilisteners.remove(myLibraryTable);
      }
    }

    @Override
    public void afterLibraryAdded(final Library newLibrary) {
      incModificationCount();
      mergeRootsChangesDuring(new Runnable() {
        @Override
        public void run() {
          for (LibraryTable.Listener listener : myListeners) {
            listener.afterLibraryAdded(newLibrary);
          }
        }
      });
    }

    @Override
    public void afterLibraryRenamed(final Library library) {
      incModificationCount();
      mergeRootsChangesDuring(new Runnable() {
        @Override
        public void run() {
          for (LibraryTable.Listener listener : myListeners) {
            listener.afterLibraryRenamed(library);
          }
        }
      });
    }

    @Override
    public void beforeLibraryRemoved(final Library library) {
      incModificationCount();
      mergeRootsChangesDuring(new Runnable() {
        @Override
        public void run() {
          for (LibraryTable.Listener listener : myListeners) {
            listener.beforeLibraryRemoved(library);
          }
        }
      });
    }

    @Override
    public void afterLibraryRemoved(final Library library) {
      incModificationCount();
      mergeRootsChangesDuring(new Runnable() {
        @Override
        public void run() {
          for (LibraryTable.Listener listener : myListeners) {
            listener.afterLibraryRemoved(library);
          }
        }
      });
    }
  }

  @Nonnull
  public VirtualFilePointerListener getRootsValidityChangedListener() {
    return myRootsValidityChangedListener;
  }
}
