/*
 * Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
package consulo.versionControlSystem.impl.internal.annotate;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.annotation.component.ServiceImpl;
import consulo.application.Application;
import consulo.application.PowerSaveMode;
import consulo.application.util.concurrent.AppExecutorUtil;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.event.FileEditorManagerEvent;
import consulo.fileEditor.event.FileEditorManagerListener;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.project.Project;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VcsException;
import consulo.versionControlSystem.annotate.AnnotationProvider;
import consulo.versionControlSystem.annotate.CacheableAnnotationProvider;
import consulo.versionControlSystem.annotate.FileAnnotation;
import consulo.versionControlSystem.annotate.VcsCacheManager;
import consulo.versionControlSystem.annotate.VcsCacheableAnnotationProvider;
import consulo.versionControlSystem.impl.internal.codeVision.VcsCodeVisionProvider;
import consulo.virtualFileSystem.VirtualFile;
import consulo.logging.Logger;
import consulo.virtualFileSystem.status.FileStatus;
import consulo.virtualFileSystem.status.FileStatusManager;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Preloads VCS annotations in the background for the currently open file, so that
 * {@link VcsCodeVisionProvider} can immediately retrieve them without waiting.
 * <p>
 * Ported from JetBrains IntelliJ Community {@code AnnotationsPreloader.kt}.
 */
@ServiceAPI(ComponentScope.PROJECT)
@ServiceImpl
@Singleton
public class AnnotationsPreloader {
  private static final Logger LOG = Logger.getInstance(AnnotationsPreloader.class);

  private final Set<VirtualFile> myPendingFiles = new LinkedHashSet<>();
  private @Nullable Future<?> myFlushFuture;
  private final Project myProject;

  @Inject
  public AnnotationsPreloader(Project project) {
    myProject = project;

    project.getMessageBus().connect(project).subscribe(FileEditorManagerListener.class, new FileEditorManagerListener() {
      @Override
      public void selectionChanged(FileEditorManagerEvent event) {
        if (!isEnabled()) return;
        VirtualFile file = event.getNewFile();
        if (file != null) {
          schedulePreloading(file);
        }
      }
    });
  }

  /**
   * Returns {@code true} when annotation preloading should run.
   * Matches JB's {@code AnnotationsPreloader.isEnabled()} — disabled in Power Save Mode.
   */
  public static boolean isEnabled() {
    return !PowerSaveMode.isEnabled();
  }

  public void schedulePreloading(final VirtualFile file) {
    if (myProject.isDisposed() || file.getFileType().isBinary()) return;

    synchronized (myPendingFiles) {
      myPendingFiles.add(file);
      Future<?> future = myFlushFuture;
      if (future == null || future.isDone()) {
        myFlushFuture = AppExecutorUtil.getAppScheduledExecutorService().schedule(this::flush, 1000, TimeUnit.MILLISECONDS);
      }
    }
  }

  private void flush() {
    List<VirtualFile> files;
    synchronized (myPendingFiles) {
      files = new ArrayList<>(myPendingFiles);
      myPendingFiles.clear();
    }
    for (VirtualFile file : files) {
      if (myProject.isDisposed()) {
        return;
      }
      preload(file);
    }
  }

  private void preload(VirtualFile file) {
    try {
      long start = 0;
      if (LOG.isDebugEnabled()) {
        start = System.currentTimeMillis();
      }
      if (!FileEditorManager.getInstance(myProject).isFileOpen(file)) return;

      CacheableAnnotationProvider annotationProvider = getAnnotationProvider(myProject, file);
      if (annotationProvider == null) return;

      // JB approach: delegate populateCache() to the CacheableAnnotationProvider.
      // For VCS plugins that implement CacheableAnnotationProvider directly (e.g. future git).
      annotationProvider.populateCache(file);

      if (LOG.isDebugEnabled()) {
        LOG.debug("Preloaded VCS annotations for ", file.getName(), " in ",
                  String.valueOf(System.currentTimeMillis() - start), "ms");
      }

      Application.get().invokeLater(() -> {
        if (myProject.isDisposed()) return;
        DaemonCodeAnalyzer.getInstance(myProject).restart();
      });
    }
    catch (VcsException e) {
      LOG.info(e);
    }
  }

  /**
   * Returns a {@link CacheableAnnotationProvider} for the given file, or {@code null} if
   * the file's VCS does not support annotation caching.
   * <p>
   * Ported from JB {@code AnnotationsPreloader.getAnnotationProvider}.
   */
  private static CacheableAnnotationProvider getAnnotationProvider(Project project, VirtualFile file) {
    FileStatus status = FileStatusManager.getInstance(project).getStatus(file);
    if (status == FileStatus.UNKNOWN || status == FileStatus.ADDED || status == FileStatus.IGNORED) return null;

    AbstractVcs vcs = ProjectLevelVcsManager.getInstance(project).getVcsFor(file);
    if (vcs == null) return null;

    AnnotationProvider ap = vcs.getAnnotationProvider();

    // Prefer a direct CacheableAnnotationProvider (JB approach, used when git is updated).
    if (ap instanceof CacheableAnnotationProvider cacheableProvider) {
      return cacheableProvider;
    }

    // Fallback: for VCS plugins (e.g. the current Consulo git) that implement
    // VcsCacheableAnnotationProvider but not CacheableAnnotationProvider yet.
    // Wrap with an adapter that stores the result in VcsCacheManager.
    if (ap instanceof VcsCacheableAnnotationProvider) {
      return new CacheableAnnotationProvider() {
        @Override
        public void populateCache(VirtualFile f) throws VcsException {
          VcsCacheManager cacheManager = VcsCacheManager.getInstance(project);
          if (cacheManager.isCached(f)) return;
          FileAnnotation annotation = ap.annotate(f);
          cacheManager.cacheAnnotation(f, annotation);
        }

        @Override
        public FileAnnotation getFromCache(VirtualFile f) {
          return VcsCacheManager.getInstance(project).getCachedAnnotation(f);
        }
      };
    }

    return null;
  }
}
