// Copyright 2000-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.

/*
 * Copyright 2013-2026 consulo.io
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

import consulo.application.ReadAction;
import consulo.application.progress.ProgressManager;
import consulo.application.util.UniqueNameBuilder;
import consulo.component.ProcessCanceledException;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.project.Project;
import consulo.project.ui.notification.NotificationType;
import consulo.ui.ex.awt.util.Alarm;
import consulo.ui.ex.awt.util.MergingUpdateQueue;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.Lists;
import consulo.util.collection.MultiMap;
import consulo.util.io.FileUtil;
import consulo.util.lang.ExceptionUtil;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.Functions;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.AllVcses;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.VcsDirectoryMapping;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.VcsMappingListener;
import consulo.versionControlSystem.VcsRootChecker;
import consulo.versionControlSystem.impl.internal.util.DisposableUpdate;
import consulo.versionControlSystem.impl.internal.util.FilePathMapping;
import consulo.versionControlSystem.impl.internal.util.VirtualFileMapping;
import consulo.versionControlSystem.internal.DefaultVcsRootPolicy;
import consulo.versionControlSystem.internal.VcsMappingResolver;
import consulo.versionControlSystem.localize.VcsLocalize;
import consulo.versionControlSystem.ui.VcsBalloonProblemNotifier;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.pointer.VirtualFilePointer;
import consulo.virtualFileSystem.pointer.VirtualFilePointerListener;
import consulo.virtualFileSystem.pointer.VirtualFilePointerManager;
import consulo.virtualFileSystem.status.FileStatusManager;
import org.jetbrains.annotations.TestOnly;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static java.util.function.Function.identity;

/**
 * A project-level holder of VCS mappings
 */
public final class NewMappings implements Disposable {
  private static final Comparator<MappedRoot> ROOT_COMPARATOR = Comparator.comparing(it -> it.root.getPath());
  public static final Comparator<VcsDirectoryMapping> MAPPINGS_COMPARATOR = Comparator.comparing(VcsDirectoryMapping::getDirectory);

  private static final Logger LOG = Logger.getInstance(NewMappings.class);
  private final Object myUpdateLock = new Object();

  private FileWatchRequestsManager myFileWatchRequestsManager;

  private final ProjectLevelVcsManagerImpl myVcsManager;
  private final FileStatusManager myFileStatusManager;
  private final Project myProject;

  private volatile List<VcsDirectoryMapping> myMappings = Collections.emptyList(); // sorted by MAPPINGS_COMPARATOR

  private Disposable myFilePointerDisposable = Disposable.newDisposable();
  private volatile List<MappedRoot> myMappedRoots = Collections.emptyList(); // sorted by ROOT_COMPARATOR
  private volatile RootMapping myMappedRootsMapping = new RootMapping(Collections.emptyList());
  private volatile Map<VirtualFile, String> myMappedRootShortNames = Collections.emptyMap();

  private volatile List<AbstractVcs> myActiveVcses = Collections.emptyList();
  private volatile boolean myActivated = false;

  private final MergingUpdateQueue myRootUpdateQueue;
  private final VirtualFilePointerListener myFilePointerListener;

  public NewMappings(Project project, ProjectLevelVcsManagerImpl vcsManager, FileStatusManager fileStatusManager) {
    myProject = project;
    myVcsManager = vcsManager;
    myFileStatusManager = fileStatusManager;
    myFileWatchRequestsManager = new FileWatchRequestsManager(myProject, this);

    myRootUpdateQueue = new MergingUpdateQueue("NewMappings", 1000, true, null, this, null, Alarm.ThreadToUse.POOLED_THREAD);

    myFilePointerListener = new VirtualFilePointerListener() {
      @Override
      public void validityChanged(VirtualFilePointer[] pointers) {
        scheduleMappedRootsUpdate();
      }
    };

    if (!myProject.isDefault()) {
      myMappings = List.of(new VcsDirectoryMapping("", ""));
    }
  }

  @TestOnly
  public void setFileWatchRequestsManager(FileWatchRequestsManager fileWatchRequestsManager) {
    assert myProject.getApplication().isUnitTestMode();
    myFileWatchRequestsManager = fileWatchRequestsManager;
  }

  public AbstractVcs[] getActiveVcses() {
    return myActiveVcses.toArray(new AbstractVcs[0]);
  }

  public boolean hasActiveVcss() {
    return !myActiveVcses.isEmpty();
  }

  public boolean isActivated() {
    return myActivated;
  }

  public void activateActiveVcses() {
    synchronized (myUpdateLock) {
      if (myActivated) return;
      myActivated = true;
      LOG.debug("activated");
    }
    updateActiveVcses(true);

    boolean fireMappingsChangedEvent = true;
    if (haveDefaultMapping() != null) {
      updateMappedRootsFast(fireMappingsChangedEvent);
      scheduleMappedRootsUpdateWithoutDelay();
    }
    else {
      updateMappedRoots(fireMappingsChangedEvent);
    }
  }

  private void updateActiveVcses(boolean forceFireEvent) {
    if (!myActivated) return;
    if (myProject.isDisposed()) return;

    List<VcsDirectoryMapping> mappings = myMappings;
    Set<AbstractVcs> newVcses = new LinkedHashSet<>(ContainerUtil.mapNotNull(myMappings, this::getMappingsVcs));

    List<AbstractVcs> oldVcses;
    synchronized (myUpdateLock) {
      if (myMappings != mappings) return;

      oldVcses = myActiveVcses;
      myActiveVcses = List.copyOf(newVcses);
    }

    Collection<AbstractVcs> toAdd = ContainerUtil.subtract(myActiveVcses, oldVcses);
    Collection<AbstractVcs> toRemove = ContainerUtil.subtract(oldVcses, myActiveVcses);
    VcsActivator activator = new VcsActivator(toAdd, toRemove);

    activator.activate();
  }

  public void setMapping(String path, String activeVcsName) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("setMapping path = '" + path + "' vcs = " + activeVcsName, new Throwable());
    }
    VcsDirectoryMapping newMapping = new VcsDirectoryMapping(path, activeVcsName);

    List<VcsDirectoryMapping> newMappings = new ArrayList<>(myMappings);
    newMappings.removeIf(mapping -> Objects.equals(mapping.getDirectory(), newMapping.getDirectory()));
    newMappings.add(newMapping);

    updateVcsMappings(newMappings);
  }

  @TestOnly
  public void waitMappedRootsUpdate() {
    myRootUpdateQueue.flush();
  }

  @TestOnly
  public void freezeMappedRootsUpdate(Disposable disposable) {
    myRootUpdateQueue.setPassThrough(false);
    myRootUpdateQueue.suspend();
    Disposer.register(disposable, () -> {
      myRootUpdateQueue.setPassThrough(false);
      myRootUpdateQueue.resume();
    });
  }

  public void updateMappedVcsesImmediately() {
    LOG.debug("updateMappingsImmediately");

    updateActiveVcses(false);

    synchronized (myUpdateLock) {
      if (!myActivated) return;

      Disposer.dispose(myFilePointerDisposable);
      myFilePointerDisposable = Disposable.newDisposable();

      myMappedRoots = Collections.emptyList();
      myMappedRootsMapping = new RootMapping(Collections.emptyList());
      myMappedRootShortNames = Collections.emptyMap();

      dumpMappedRootsToLog();
    }
    notifyMappingsChanged();

    scheduleMappedRootsUpdate();
  }

  public void scheduleMappedRootsUpdate() {
    myRootUpdateQueue.queue(new DisposableUpdate(this, "update") {
      @Override
      public void doRun() {
        updateMappedRoots(true);
      }
    });
  }

  private void scheduleMappedRootsUpdateWithoutDelay() {
    scheduleMappedRootsUpdate();
    myRootUpdateQueue.sendFlush();
  }

  private void updateVcsMappings(List<VcsDirectoryMapping> mappings) {
    updateVcsMappings(mappings, true);
  }

  private void updateVcsMappings(List<VcsDirectoryMapping> mappings, boolean updateRootsImmediately) {
    myRootUpdateQueue.cancelAllUpdates();

    List<VcsDirectoryMapping> newMappings = List.copyOf(ContainerUtil.sorted(removeDuplicates(mappings), MAPPINGS_COMPARATOR));
    synchronized (myUpdateLock) {
      boolean mappingsChanged = !myMappings.equals(newMappings);
      if (!mappingsChanged) return; // mappings are up-to-date

      myMappings = newMappings;

      dumpMappingsToLog();
    }

    updateActiveVcses(false);

    if (updateRootsImmediately) {
      boolean fireMappingsChangedEvent = false;
      if (myProject.getApplication().isDispatchThread() &&
          ContainerUtil.exists(newMappings, it -> it.isDefaultMapping())) {
        updateMappedRootsFast(fireMappingsChangedEvent);
        scheduleMappedRootsUpdateWithoutDelay();
      }
      else {
        updateMappedRoots(fireMappingsChangedEvent);
      }
    }
    else {
      scheduleMappedRootsUpdateWithoutDelay();
    }

    // do not fire event from ProjectLevelVcsManager service initialization
    if (updateRootsImmediately || isActivated()) {
      notifyMappingsChanged();
    }
  }

  private void updateMappedRoots(boolean fireMappingsChangedEvent) {
    myRootUpdateQueue.cancelAllUpdates();

    updateMappedRootsImpl(fireMappingsChangedEvent, false);
  }

  private void updateMappedRootsFast(boolean fireMappingsChangedEvent) {
    updateMappedRootsImpl(fireMappingsChangedEvent, true);
  }

  private void updateMappedRootsImpl(boolean fireMappingsChangedEvent, boolean onlyFastUpdate) {
    if (!myActivated) return;
    LOG.debug("updateMappedRoots: fastUpdate = " + onlyFastUpdate);

    List<VcsDirectoryMapping> mappings;
    List<MappedRoot> mappedRoots;
    synchronized (myUpdateLock) {
      mappings = myMappings;
      mappedRoots = myMappedRoots;
    }
    if (mappedRoots.isEmpty()) {
      mappedRoots = getCachedMappedRootsIfNeeded(mappings);
    }
    Mappings newMappedRoots = collectMappedRoots(mappings, mappedRoots, onlyFastUpdate);

    setNewMappedRoots(mappings, newMappedRoots, fireMappingsChangedEvent);
  }

  private void setNewMappedRoots(List<VcsDirectoryMapping> mappings,
                                 Mappings newMappedRoots,
                                 boolean fireMappingsChangedEvent) {
    Map<VirtualFile, String> newMappedRootNames = buildMappingShortNameMap(myProject, newMappedRoots.mappedRoots);

    boolean mappedRootsChanged;
    synchronized (myUpdateLock) {
      if (myMappings != mappings) {
        Disposer.dispose(newMappedRoots.filePointerDisposable);
        return;
      }

      Disposer.dispose(myFilePointerDisposable);
      myFilePointerDisposable = newMappedRoots.filePointerDisposable;

      mappedRootsChanged = !myMappedRoots.equals(newMappedRoots.mappedRoots);
      if (mappedRootsChanged) {
        myMappedRoots = newMappedRoots.mappedRoots;
        myMappedRootsMapping = new RootMapping(newMappedRoots.mappedRoots);
        myMappedRootShortNames = newMappedRootNames;

        dumpMappedRootsToLog();
      }
    }

    if (fireMappingsChangedEvent && mappedRootsChanged) notifyMappingsChanged();
  }

  private static Map<VirtualFile, String> buildMappingShortNameMap(Project project, List<MappedRoot> roots) {
    String basePath = project.getBasePath();
    String builderRoot = basePath != null ? FileUtil.toSystemIndependentName(basePath) : "";

    MultiMap<String, VirtualFile> nameCollisionMap = new MultiMap<>();
    for (MappedRoot mappedRoot : roots) {
      VirtualFile root = mappedRoot.root;
      nameCollisionMap.putValue(root.getName(), root);
    }

    Map<VirtualFile, String> result = new HashMap<>();
    for (Map.Entry<String, Collection<VirtualFile>> entry : nameCollisionMap.entrySet()) {
      Collection<VirtualFile> collisionRoots = entry.getValue();
      if (collisionRoots.size() == 1) {
        // UniqueNameBuilder doesn't support unique names
        for (VirtualFile root : collisionRoots) {
          result.put(root, root.getName());
        }
      }
      else {
        UniqueNameBuilder<VirtualFile> builder = new UniqueNameBuilder<>(builderRoot, File.separator, Integer.MAX_VALUE);
        for (VirtualFile root : collisionRoots) {
          builder.addPath(root, root.getPath());
        }
        for (VirtualFile root : collisionRoots) {
          String shortPath = StringUtil.nullize(builder.getShortPath(root));
          if (shortPath != null) {
            result.put(root, shortPath);
          }
        }
      }
    }
    return result;
  }

  /**
   * Take last mapping in collection in case of duplicates.
   */
  private static List<VcsDirectoryMapping> removeDuplicates(List<VcsDirectoryMapping> mappings) {
    List<VcsDirectoryMapping> newMapping = new ArrayList<>();
    Set<String> paths = new HashSet<>();

    for (VcsDirectoryMapping mapping : ContainerUtil.reverse(mappings)) {
      if (paths.add(mapping.getDirectory())) {
        newMapping.add(mapping);
      }
    }
    return newMapping;
  }

  private List<MappedRoot> getCachedMappedRootsIfNeeded(List<VcsDirectoryMapping> mappings) {
    VcsDirectoryMapping defaultMapping = ContainerUtil.find(mappings, it -> it.isDefaultMapping());
    if (defaultMapping == null) return Collections.emptyList();

    AbstractVcs vcs = getMappingsVcs(defaultMapping);
    if (vcs == null) return Collections.emptyList();

    List<String> oldMappings = VcsDirectoryMappingCache.getInstance(myProject).getMappings(defaultMapping.getVcs());
    return ContainerUtil.mapNotNull(oldMappings, path -> {
      VirtualFile root = VcsMappingResolver.findDirectory(myProject, path, false);
      return root != null ? new MappedRoot(vcs, defaultMapping, root) : null;
    });
  }

  private Mappings collectMappedRoots(List<VcsDirectoryMapping> mappings,
                                      List<MappedRoot> reuseMappedRoots,
                                      boolean onlyFastUpdate) {
    Map<VirtualFile, MappedRoot> mappedRoots = new HashMap<>();
    Disposable pointerDisposable = Disposable.newDisposable();

    if (myProject.isDefault()) {
      return new Mappings(Collections.emptyList(), pointerDisposable);
    }

    try {
      // direct mappings have priority over <Project> mappings
      for (VcsDirectoryMapping mapping : mappings) {
        if (mapping.isDefaultMapping()) {
          continue;
        }

        MappedRoot mappedRoot = findDirectMappingFor(mapping, pointerDisposable);
        if (mappedRoot != null) {
          mappedRoots.putIfAbsent(mappedRoot.root, mappedRoot);
        }
        else {
          LOG.info("Invalid mapping: " + mapping);
        }
      }

      for (VcsDirectoryMapping mapping : mappings) {
        if (!mapping.isDefaultMapping()) {
          continue;
        }

        List<MappedRoot> defaultMappings;
        if (onlyFastUpdate) {
          defaultMappings = reuseDefaultMappingsFrom(mapping, reuseMappedRoots, pointerDisposable);
        }
        else {
          Set<VirtualFile> directMappingDirs = ContainerUtil.map2Set(mappedRoots.values(), it -> it.root);
          try {
            defaultMappings = findDefaultMappingsFor(mapping, directMappingDirs, pointerDisposable);

            VcsDirectoryMappingCache.getInstance(myProject).setMappings(mapping.getVcs(),
                                                                        ContainerUtil.map(defaultMappings, it -> it.root.getPath()));
          }
          catch (VcsException e) {
            LOG.warn("Cannot find default mappings for " + mapping.getVcs(), e);
            defaultMappings = reuseDefaultMappingsFrom(mapping, reuseMappedRoots, pointerDisposable);
          }
        }
        for (MappedRoot mappedRoot : defaultMappings) {
          mappedRoots.putIfAbsent(mappedRoot.root, mappedRoot);
        }
      }

      List<MappedRoot> result = ContainerUtil.sorted(mappedRoots.values(), ROOT_COMPARATOR);

      for (MappedRoot root : result) {
        if (myVcsManager.isIgnoredFileRoot(root.root)) {
          LOG.warn("Root mapping is under ignored root: " + root.root);
        }
      }

      return new Mappings(result, pointerDisposable);
    }
    catch (Throwable e) {
      Disposer.dispose(pointerDisposable);
      ExceptionUtil.rethrow(e);
      throw new IllegalStateException(e);
    }
  }

  private @Nullable MappedRoot findDirectMappingFor(VcsDirectoryMapping mapping,
                                                    Disposable pointerDisposable) {
    AbstractVcs vcs = getMappingsVcs(mapping);
    String rootPath = mapping.getDirectory();

    return ReadAction.compute(() -> {
      VirtualFilePointerManager.getInstance().create(VcsMappingResolver.toUrl(myProject, rootPath), pointerDisposable, myFilePointerListener);

      VirtualFile vcsRoot = VcsMappingResolver.findDirectory(myProject, rootPath, false);
      if (vcsRoot == null || !vcsRoot.isDirectory()) {
        return null;
      }

      if (checkMappedRoot(vcs, vcsRoot)) {
        return new MappedRoot(vcs, mapping, vcsRoot);
      }
      else {
        return new MappedRoot(null, mapping, vcsRoot);
      }
    });
  }

  private List<MappedRoot> findDefaultMappingsFor(VcsDirectoryMapping mapping,
                                                  Set<VirtualFile> directMappingDirs,
                                                  Disposable pointerDisposable) throws VcsException {
    AbstractVcs vcs = getMappingsVcs(mapping);
    if (vcs == null) {
      return Collections.emptyList();
    }

    Collection<VirtualFile> defaultRoots = detectDefaultRootsFor(vcs,
                                                                 DefaultVcsRootPolicy.getInstance(myProject).getDefaultVcsRoots(),
                                                                 directMappingDirs);

    List<MappedRoot> result = new ArrayList<>();
    ReadAction.run(() -> {
      for (VirtualFile vcsRoot : defaultRoots) {
        if (vcsRoot != null && vcsRoot.isDirectory()) {
          VirtualFilePointerManager.getInstance().create(vcsRoot, pointerDisposable, myFilePointerListener);
          result.add(new MappedRoot(vcs, mapping, vcsRoot));
        }
      }
    });
    return result;
  }

  private List<MappedRoot> reuseDefaultMappingsFrom(VcsDirectoryMapping mapping,
                                                    List<MappedRoot> oldMappedRoots,
                                                    Disposable pointerDisposable) {
    // Pretend that mappings did not change at first, and "<Project>" mappings has detected all the roots that were used before.
    // This prevents such roots from being temporally unregistered if they will be detected later by the proper-and-slow logic.
    // Which in its turn allows preserving Change-to-Changelist mappings and
    // avoiding sporadic "file not under VCS root anymore" errors in the middle of operation.

    // For example, if a "$PROJECT_DIR$" was replaced with a "<Project>" mapping as a part of shelve-unshelve operation.

    List<MappedRoot> result = new ArrayList<>();
    ReadAction.run(() -> {
      List<MappedRoot> oldMappings = ContainerUtil.filter(oldMappedRoots, root -> root.mapping.getVcs().equals(mapping.getVcs()));
      for (MappedRoot root : oldMappings) {
        VirtualFilePointerManager.getInstance().create(root.root, pointerDisposable, myFilePointerListener);
        result.add(new MappedRoot(root.vcs, mapping, root.root));
      }
    });
    return result;
  }

  private @Nullable AbstractVcs getMappingsVcs(VcsDirectoryMapping mapping) {
    return AllVcses.getInstance(myProject).getByName(mapping.getVcs());
  }

  private boolean checkMappedRoot(@Nullable AbstractVcs vcs, VirtualFile vcsRoot) {
    try {
      if (vcs == null) return false;
      VcsRootChecker rootChecker = myVcsManager.getRootChecker(vcs);
      return rootChecker.validateRoot(vcsRoot);
    }
    catch (ProcessCanceledException e) {
      throw e;
    }
    catch (Throwable e) {
      LOG.error(e);
      return false;
    }
  }

  private Collection<VirtualFile> detectDefaultRootsFor(AbstractVcs vcs,
                                                        Collection<VirtualFile> projectRoots,
                                                        Set<VirtualFile> mappedDirs) throws VcsException {
    try {
      if (vcs.needsLegacyDefaultMappings()) return projectRoots;

      VcsRootChecker rootChecker = myVcsManager.getRootChecker(vcs);

      Collection<VirtualFile> checkerFiles = rootChecker.detectProjectMappings(myProject, projectRoots, mappedDirs);
      if (checkerFiles != null) return checkerFiles;

      return VcsDefaultMappingUtils.detectProjectMappings(myProject, rootChecker, projectRoots, mappedDirs);
    }
    catch (ProcessCanceledException | VcsException e) {
      throw e;
    }
    catch (Throwable e) {
      LOG.error(e);
      return Collections.emptyList();
    }
  }

  public void notifyMappingsChanged() {
    if (myProject.isDisposed()) return;
    myProject.getMessageBus().syncPublisher(VcsMappingListener.class).directoryMappingChanged();
    myFileStatusManager.fileStatusesChanged();
    myFileWatchRequestsManager.ping();
  }

  private void dumpMappingsToLog() {
    for (VcsDirectoryMapping mapping : myMappings) {
      String path = mapping.isDefaultMapping() ? "<Project>" : mapping.getDirectory();
      String vcs = mapping.getVcs();
      LOG.info(String.format("VCS Root: [%s] - [%s]", vcs, path));
    }
  }

  private void dumpMappedRootsToLog() {
    if (LOG.isDebugEnabled()) {
      for (MappedRoot root : myMappedRoots) {
        LOG.debug(String.format("Mapped Root: [%s] - [%s]", root.vcs, root.root.getPath()));
      }
    }
    else if (haveDefaultMapping() != null) {
      LOG.info("Mapped Roots: " + myMappedRoots.size());
      List<MappedRoot> detectedRoots = ContainerUtil.filter(myMappedRoots, root -> root.mapping.isDefaultMapping());
      for (MappedRoot root : Lists.getFirstItems(detectedRoots, 10)) {
        LOG.info(String.format("Detected mapped Root: [%s] - [%s]", root.vcs, root.root.getPath()));
      }
    }
  }

  public void setDirectoryMappingsFromConfig(List<VcsDirectoryMapping> items) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("setDirectoryMappingsFromConfig, size: " + items.size(), new Throwable());
    }

    updateVcsMappings(withDefaultMapping(items), false);
  }

  public void setDirectoryMappings(List<VcsDirectoryMapping> items) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("setDirectoryMappings, size: " + items.size(), new Throwable());
    }

    updateVcsMappings(withDefaultMapping(items));
  }

  private static List<VcsDirectoryMapping> withDefaultMapping(List<VcsDirectoryMapping> items) {
    if (items.isEmpty()) {
      return Collections.singletonList(new VcsDirectoryMapping("", ""));
    }
    return items;
  }

  public @Nullable MappedRoot getMappedRootFor(@Nullable VirtualFile file) {
    if (file == null || !file.isInLocalLikeFileSystem()) return null;
    if (myMappedRoots.isEmpty()) return null;

    return myMappedRootsMapping.getRootFor(file);
  }

  public @Nullable MappedRoot getMappedRootFor(@Nullable FilePath file) {
    if (file == null || file.isNonLocal()) return null;
    if (myMappedRoots.isEmpty()) return null;

    return myMappedRootsMapping.getRootFor(file);
  }

  public List<MappedRoot> getAllMappedRoots() {
    return myMappedRoots;
  }

  public @Nullable String getShortNameFor(@Nullable VirtualFile file) {
    return myMappedRootShortNames.get(file);
  }

  public List<VirtualFile> getMappingsAsFilesUnderVcs(AbstractVcs vcs) {
    return ContainerUtil.mapNotNull(myMappedRoots, root -> vcs.equals(root.vcs) ? root.root : null);
  }

  public List<VirtualFile> getDefaultRoots() {
    String defaultVcs = haveDefaultMapping();
    if (defaultVcs == null) return Collections.emptyList();
    List<VirtualFile> list = new ArrayList<>(DefaultVcsRootPolicy.getInstance(myProject).getDefaultVcsRoots());
    AbstractVcs<?> vcs = StringUtil.isEmptyOrSpaces(defaultVcs) ? null : AllVcses.getInstance(myProject).getByName(defaultVcs);
    if (vcs == null) {
      return AbstractVcs.filterUniqueRootsDefault(list, identity());
    }
    return vcs.filterUniqueRoots(list, identity());
  }

  @Override
  public void dispose() {
    LOG.debug("disposed");

    VcsActivator activator;
    synchronized (myUpdateLock) {
      Disposer.dispose(myFilePointerDisposable);
      myMappings = Collections.emptyList();
      myMappedRoots = Collections.emptyList();
      myMappedRootsMapping = new RootMapping(Collections.emptyList());
      myMappedRootShortNames = Collections.emptyMap();
      myFilePointerDisposable = Disposable.newDisposable();

      List<AbstractVcs> oldVcses = myActiveVcses;
      myActiveVcses = Collections.emptyList();

      activator = new VcsActivator(Collections.emptyList(), oldVcses);
    }
    activator.activate();
  }

  public List<VcsDirectoryMapping> getDirectoryMappings() {
    return myMappings;
  }

  public List<VcsDirectoryMapping> getDirectoryMappings(String vcsName) {
    return ContainerUtil.filter(myMappings, mapping -> Objects.equals(mapping.getVcs(), vcsName));
  }

  public @Nullable String haveDefaultMapping() {
    VcsDirectoryMapping defaultMapping = ContainerUtil.find(myMappings, mapping -> mapping.isDefaultMapping());
    return defaultMapping != null ? defaultMapping.getVcs() : null;
  }

  public boolean isEmpty() {
    return ContainerUtil.and(myMappings, mapping -> mapping.isNoneMapping());
  }

  public void removeDirectoryMapping(VcsDirectoryMapping mapping) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("remove mapping: " + mapping.getDirectory(), new Throwable());
    }

    List<VcsDirectoryMapping> newMappings = new ArrayList<>(myMappings);
    newMappings.remove(mapping);

    updateVcsMappings(newMappings);
  }

  public void cleanupMappings() {
    List<VcsDirectoryMapping> oldMappings = new ArrayList<>(getDirectoryMappings());

    List<VcsDirectoryMapping> filteredMappings = new ArrayList<>();

    VcsDirectoryMapping defaultMapping = ContainerUtil.find(oldMappings, it -> it.isDefaultMapping());
    if (defaultMapping != null) {
      oldMappings.remove(defaultMapping);
      filteredMappings.add(defaultMapping);
    }

    MultiMap<String, VcsDirectoryMapping> groupedMappings = new MultiMap<>();
    for (VcsDirectoryMapping mapping : oldMappings) {
      groupedMappings.putValue(mapping.getVcs(), mapping);
    }

    for (Map.Entry<String, Collection<VcsDirectoryMapping>> entry : groupedMappings.entrySet()) {
      String vcsName = entry.getKey();
      Collection<VcsDirectoryMapping> mappings = entry.getValue();

      List<Pair<VirtualFile, VcsDirectoryMapping>> objects = ContainerUtil.mapNotNull(mappings, dm -> {
        VirtualFile vf = VcsMappingResolver.findDirectory(myProject, dm.getDirectory(), true);
        return vf == null ? null : Pair.create(vf, dm);
      });

      if (StringUtil.isEmptyOrSpaces(vcsName)) {
        filteredMappings.addAll(ContainerUtil.map(objects, Functions.pairSecond()));
      }
      else {
        AbstractVcs<?> vcs = myVcsManager.findVcsByName(vcsName);
        if (vcs == null) {
          VcsBalloonProblemNotifier.showOverChangesView(
            myProject,
            VcsLocalize.implNotificationContentVcsPluginNotFoundForMappingTo(vcsName).get(),
            NotificationType.ERROR
          );
          filteredMappings.addAll(mappings);
        }
        else {
          List<Pair<VirtualFile, VcsDirectoryMapping>> uniqueRoots = vcs.filterUniqueRoots(new ArrayList<>(objects), pair -> pair.getFirst());
          filteredMappings.addAll(ContainerUtil.map(uniqueRoots, Functions.pairSecond()));
        }
      }
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("NewMappings.cleanupMappings", getDirectoryMappings(), filteredMappings);
    }

    updateVcsMappings(filteredMappings);
  }

  private static final class VcsActivator {
    private final Collection<? extends AbstractVcs> myAddVcses;
    private final Collection<? extends AbstractVcs> myRemoveVcses;

    private VcsActivator(Collection<? extends AbstractVcs> addVcses,
                         Collection<? extends AbstractVcs> removeVcses) {
      myAddVcses = addVcses;
      myRemoveVcses = removeVcses;
    }

    public boolean activate() {
      if (myAddVcses.isEmpty() && myRemoveVcses.isEmpty()) return false;

      ProgressManager.getInstance().executeNonCancelableSection(() -> {
        for (AbstractVcs vcs : myAddVcses) {
          try {
            vcs.doActivate();
          }
          catch (VcsException e) {
            LOG.error(e);
          }
        }
        for (AbstractVcs vcs : myRemoveVcses) {
          try {
            vcs.doDeactivate();
          }
          catch (VcsException e) {
            LOG.error(e);
          }
        }
      });
      return true;
    }
  }

  public boolean haveActiveVcs(String name) {
    return ContainerUtil.exists(myActiveVcses, vcs -> Objects.equals(vcs.getId(), name));
  }

  public void beingUnregistered(String name) {
    if (LOG.isDebugEnabled()) {
      LOG.debug("beingUnregistered " + name, new Throwable());
    }

    List<VcsDirectoryMapping> newMappings = new ArrayList<>(myMappings);
    newMappings.removeIf(mapping -> Objects.equals(mapping.getVcs(), name));

    updateVcsMappings(newMappings);
  }

  public static final class MappedRoot {
    public final @Nullable AbstractVcs vcs;
    public final VcsDirectoryMapping mapping;
    public final VirtualFile root;

    private MappedRoot(@Nullable AbstractVcs vcs, VcsDirectoryMapping mapping, VirtualFile root) {
      this.vcs = vcs;
      this.mapping = mapping;
      this.root = root;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      MappedRoot other = (MappedRoot)o;
      return Objects.equals(vcs, other.vcs) &&
             mapping.equals(other.mapping) &&
             root.equals(other.root);
    }

    @Override
    public int hashCode() {
      return Objects.hash(vcs, mapping, root);
    }
  }

  private static final class Mappings {
    public final List<MappedRoot> mappedRoots;
    public final Disposable filePointerDisposable;

    private Mappings(List<MappedRoot> mappedRoots, Disposable filePointerDisposable) {
      this.mappedRoots = mappedRoots;
      this.filePointerDisposable = filePointerDisposable;
    }
  }

  private static final class RootMapping {
    private final VirtualFileMapping<MappedRoot> myVFMap = new VirtualFileMapping<>();
    private final FilePathMapping<MappedRoot> myPathMapping = new FilePathMapping<>(Platform.current().fs().isCaseSensitive());

    private RootMapping(List<MappedRoot> mappedRoots) {
      for (MappedRoot root : mappedRoots) {
        myVFMap.add(root.root, root);
        myPathMapping.add(root.root.getPath(), root);
      }
    }

    public @Nullable MappedRoot getRootFor(VirtualFile file) {
      return myVFMap.getMappingFor(file);
    }

    public @Nullable MappedRoot getRootFor(FilePath filePath) {
      return myPathMapping.getMappingFor(filePath.getPath());
    }
  }
}
