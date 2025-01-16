// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.impl.internal.change;

import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.application.util.registry.Registry;
import consulo.component.ProcessCanceledException;
import consulo.component.messagebus.MessageBusConnection;
import consulo.disposer.Disposable;
import consulo.virtualFileSystem.fileType.FileTypeEvent;
import consulo.virtualFileSystem.fileType.FileTypeListener;
import consulo.logging.Logger;
import consulo.module.content.layer.event.ModuleRootEvent;
import consulo.module.content.layer.event.ModuleRootListener;
import consulo.project.Project;
import consulo.ui.ModalityState;
import consulo.util.collection.ContainerUtil;
import consulo.util.collection.HashingStrategy;
import consulo.util.collection.Sets;
import consulo.util.concurrent.ActionCallback;
import consulo.util.lang.StringUtil;
import consulo.util.lang.reflect.ReflectionUtil;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.change.ChangesUtil;
import consulo.versionControlSystem.change.VcsDirtyScopeManager;
import consulo.versionControlSystem.change.VcsInvalidated;
import consulo.versionControlSystem.change.VcsModifiableDirtyScope;
import consulo.versionControlSystem.internal.ChangeListManagerEx;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.versionControlSystem.util.VcsUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.*;

@ServiceImpl
@Singleton
public final class VcsDirtyScopeManagerImpl extends VcsDirtyScopeManager implements Disposable {
    private static final Logger LOG = Logger.getInstance(VcsDirtyScopeManagerImpl.class);

    private final Project myProject;

    @Nonnull
    private DirtBuilder myDirtBuilder = new DirtBuilder();
    @Nullable
    private DirtBuilder myDirtInProgress;
    @Nullable
    private ActionCallback myRefreshInProgress;

    private boolean myReady;
    private final Object LOCK = new Object();

    @Nonnull
    public static VcsDirtyScopeManagerImpl getInstanceImpl(@Nonnull Project project) {
        return ((VcsDirtyScopeManagerImpl) getInstance(project));
    }

    @Inject
    public VcsDirtyScopeManagerImpl(@Nonnull Project project) {
        myProject = project;

        MessageBusConnection busConnection = myProject.getMessageBus().connect();
        busConnection.subscribe(FileTypeListener.class, new FileTypeListener() {
            @Override
            public void fileTypesChanged(@Nonnull FileTypeEvent event) {
                // Listen changes in 'FileTypeManager.getIgnoredFilesList':
                //   'ProjectLevelVcsManager.getVcsFor' depends on it via 'ProjectLevelVcsManager.isIgnored',
                //   which might impact which files are visible in ChangeListManager.

                // Event does not allow to listen for 'getIgnoredFilesList' changes directly, listen for all generic events instead.
                boolean isGenericEvent = event.getAddedFileType() == null && event.getRemovedFileType() == null;
                if (isGenericEvent) {
                    ApplicationManager.getApplication().invokeLater(() -> markEverythingDirty(), ModalityState.nonModal(), myProject.getDisposed());
                }
            }
        });

        if (Registry.is("ide.hide.excluded.files")) {
            busConnection.subscribe(ModuleRootListener.class, new ModuleRootListener() {
                @Override
                public void rootsChanged(@Nonnull ModuleRootEvent event) {
                    // 'ProjectLevelVcsManager.getVcsFor' depends on excluded roots via 'ProjectLevelVcsManager.isIgnored'
                    ApplicationManager.getApplication().invokeLater(() -> markEverythingDirty(), ModalityState.nonModal(), myProject.getDisposed());
                }
            });
            //busConnection.subscribe(AdditionalLibraryRootsListener.TOPIC, ((presentableLibraryName, oldRoots, newRoots, libraryNameForDebug) -> {
            //  ApplicationManager.getApplication().invokeLater(() -> markEverythingDirty(), ModalityState.NON_MODAL, myProject.getDisposed());
            //}));
        }
    }

    private static ProjectLevelVcsManager getVcsManager(@Nonnull Project project) {
        return ProjectLevelVcsManager.getInstance(project);
    }

    public void startListenForChanges() {
        ReadAction.run(() -> {
            boolean ready = !myProject.isDisposed() && myProject.isOpen();
            synchronized (LOCK) {
                myReady = ready;
            }
            if (ready) {
                VcsDirtyScopeVfsListener.install(myProject);
                markEverythingDirty();
            }
        });
    }

    @Override
    public void markEverythingDirty() {
        if ((!myProject.isOpen()) || myProject.isDisposed() || getVcsManager(myProject).getAllActiveVcss().length == 0) {
            return;
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("everything dirty: " + findFirstInterestingCallerClass());
        }

        boolean wasReady;
        ActionCallback ongoingRefresh;
        synchronized (LOCK) {
            wasReady = myReady;
            if (wasReady) {
                myDirtBuilder.markEverythingDirty();
            }
            ongoingRefresh = myRefreshInProgress;
        }

        if (wasReady) {
            ChangeListManagerEx.getInstanceEx(myProject).scheduleUpdateImpl();
          if (ongoingRefresh != null) {
            ongoingRefresh.setRejected();
          }
        }
    }

    @Override
    public void dispose() {
        synchronized (LOCK) {
            myReady = false;
            myDirtBuilder = new DirtBuilder();
            myDirtInProgress = null;
            myRefreshInProgress = null;
        }
    }

    private @Nonnull Map<VcsRoot, Set<FilePath>> groupByVcs(@Nullable Iterable<? extends FilePath> from) {
        if (from == null) {
            return Collections.emptyMap();
        }

        ProjectLevelVcsManager vcsManager = getVcsManager(myProject);
        Map<VcsRoot, Set<FilePath>> map = new HashMap<>();
        for (FilePath path : from) {
            VcsRoot vcsRoot = vcsManager.getVcsRootObjectFor(path);
            if (vcsRoot != null && vcsRoot.getVcs() != null) {
                Set<FilePath> pathSet = map.computeIfAbsent(vcsRoot, key -> {
                    HashingStrategy<FilePath> strategy = getDirtyScopeHashingStrategy(key.getVcs());
                    return strategy == null ? new HashSet<>() : Sets.newHashSet(strategy);
                });
                pathSet.add(path);
            }
        }
        return map;
    }

    @Nonnull
    private Map<VcsRoot, Set<FilePath>> groupFilesByVcs(@Nullable Collection<? extends VirtualFile> from) {
      if (from == null) {
        return Collections.emptyMap();
      }
        return groupByVcs(() -> ContainerUtil.mapIterator(from.iterator(), file -> VcsUtil.getFilePath(file)));
    }

    void fileVcsPathsDirty(@Nonnull Map<VcsRoot, Set<FilePath>> filesConverted,
                           @Nonnull Map<VcsRoot, Set<FilePath>> dirsConverted) {
      if (filesConverted.isEmpty() && dirsConverted.isEmpty()) {
        return;
      }

        if (LOG.isDebugEnabled()) {
            LOG.debug(String.format("dirty files: %s; dirty dirs: %s; %s",
                toString(filesConverted), toString(dirsConverted), findFirstInterestingCallerClass()));
        }

        boolean hasSomethingDirty = false;
        for (VcsRoot vcsRoot : ContainerUtil.union(filesConverted.keySet(), dirsConverted.keySet())) {
            Set<FilePath> files = Sets.notNullize(filesConverted.get(vcsRoot));
            Set<FilePath> dirs = Sets.notNullize(dirsConverted.get(vcsRoot));

            synchronized (LOCK) {
                if (myReady) {
                    hasSomethingDirty |= myDirtBuilder.addDirtyFiles(vcsRoot, files, dirs);
                }
            }
        }

        if (hasSomethingDirty) {
            ChangeListManagerEx.getInstanceEx(myProject).scheduleUpdateImpl();
        }
    }

    @Override
    public void filePathsDirty(@Nullable Collection<? extends FilePath> filesDirty,
                               @Nullable Collection<? extends FilePath> dirsRecursivelyDirty) {
        try {
            fileVcsPathsDirty(groupByVcs(filesDirty), groupByVcs(dirsRecursivelyDirty));
        }
        catch (ProcessCanceledException ignore) {
        }
    }

    @Override
    public void filesDirty(@Nullable Collection<? extends VirtualFile> filesDirty,
                           @Nullable Collection<? extends VirtualFile> dirsRecursivelyDirty) {
        try {
            fileVcsPathsDirty(groupFilesByVcs(filesDirty), groupFilesByVcs(dirsRecursivelyDirty));
        }
        catch (ProcessCanceledException ignore) {
        }
    }

    @Override
    public void fileDirty(@Nonnull final VirtualFile file) {
        fileDirty(VcsUtil.getFilePath(file));
    }

    @Override
    public void fileDirty(@Nonnull final FilePath file) {
        filePathsDirty(Collections.singleton(file), null);
    }

    @Override
    public void dirDirtyRecursively(@Nonnull final VirtualFile dir) {
        dirDirtyRecursively(VcsUtil.getFilePath(dir));
    }

    @Override
    public void dirDirtyRecursively(@Nonnull final FilePath path) {
        filePathsDirty(null, Collections.singleton(path));
    }

    /**
     * Take current dirty scope into processing.
     * Should call {@link #changesProcessed} when done to notify {@link #whatFilesDirty} that scope is no longer dirty.
     */
    @Nullable
    public VcsInvalidated retrieveScopes() {
        ActionCallback callback = new ActionCallback();
        DirtBuilder dirtBuilder;
        synchronized (LOCK) {
          if (!myReady) {
            return null;
          }
            LOG.assertTrue(myDirtInProgress == null);

            dirtBuilder = myDirtBuilder;
            myDirtInProgress = dirtBuilder;
            myDirtBuilder = new DirtBuilder();
            myRefreshInProgress = callback;
        }
        return calculateInvalidated(dirtBuilder, callback);
    }

    public boolean hasDirtyScopes() {
        synchronized (LOCK) {
          if (!myReady) {
            return false;
          }
            LOG.assertTrue(myDirtInProgress == null);

            return !myDirtBuilder.isEmpty();
        }
    }

    public void changesProcessed() {
        synchronized (LOCK) {
            myDirtInProgress = null;
            myRefreshInProgress = null;
        }
    }

    @Nonnull
    private VcsInvalidated calculateInvalidated(@Nonnull DirtBuilder dirt, @Nonnull ActionCallback callback) {
        boolean isEverythingDirty = dirt.isEverythingDirty();
        List<VcsModifiableDirtyScope> scopes = dirt.buildScopes(myProject);
        return new VcsInvalidated(scopes, isEverythingDirty, callback);
    }

    @Nonnull
    @Override
    public Collection<FilePath> whatFilesDirty(@Nonnull final Collection<? extends FilePath> files) {
        return ReadAction.compute(() -> {
            Collection<FilePath> result = new ArrayList<>();
            synchronized (LOCK) {
              if (!myReady) {
                return Collections.emptyList();
              }

                for (FilePath fp : files) {
                    if (myDirtBuilder.isFileDirty(fp) ||
                        myDirtInProgress != null && myDirtInProgress.isFileDirty(fp)) {
                        result.add(fp);
                    }
                }
            }
            return result;
        });
    }

    @Nonnull
    private static String toString(@Nonnull Map<VcsRoot, Set<FilePath>> filesByVcs) {
        return StringUtil.join(filesByVcs.keySet(), vcs
            -> vcs.getVcs() + ": " + StringUtil.join(filesByVcs.get(vcs), path -> path.getPath(), "\n"), "\n");
    }

    @Nullable
    private static Class<?> findFirstInterestingCallerClass() {
        for (int i = 1; i <= 7; i++) {
            Class<?> clazz = ReflectionUtil.findCallerClass(i);
          if (clazz == null || !clazz.getName().contains(VcsDirtyScopeManagerImpl.class.getName())) {
            return clazz;
          }
        }
        return null;
    }

    public static @Nullable HashingStrategy<FilePath> getDirtyScopeHashingStrategy(@Nonnull AbstractVcs vcs) {
        return vcs.needsCaseSensitiveDirtyScope() ? ChangesUtil.CASE_SENSITIVE_FILE_PATH_HASHING_STRATEGY : null;
    }
}
