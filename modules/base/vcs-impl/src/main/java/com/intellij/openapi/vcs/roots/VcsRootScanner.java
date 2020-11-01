// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package com.intellij.openapi.vcs.roots;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import consulo.logging.Logger;
import com.intellij.openapi.progress.util.BackgroundTaskUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vcs.ProjectLevelVcsManager;
import com.intellij.openapi.vcs.VcsRootChecker;
import com.intellij.openapi.vcs.impl.VcsInitObject;
import com.intellij.openapi.vcs.impl.VcsStartupActivity;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.Alarm;
import com.intellij.vfs.AsyncVfsEventsPostProcessor;
import consulo.disposer.Disposable;
import jakarta.inject.Singleton;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.MissingResourceException;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.intellij.openapi.vfs.VirtualFileVisitor.*;

@Singleton
public final class VcsRootScanner implements Disposable {
  private static final Logger LOG = Logger.getInstance(VcsRootScanner.class);

  @Nonnull
  private final VcsRootProblemNotifier myRootProblemNotifier;
  @Nonnull
  private final Project myProject;

  @Nonnull
  private final Alarm myAlarm;
  private static final long WAIT_BEFORE_SCAN = TimeUnit.SECONDS.toMillis(1);

  @Nonnull
  public static VcsRootScanner getInstance(@Nonnull Project project) {
    return project.getInstance(VcsRootScanner.class);
  }

  public VcsRootScanner(@Nonnull Project project) {
    myProject = project;
    myRootProblemNotifier = VcsRootProblemNotifier.createInstance(project);
    myAlarm = new Alarm(Alarm.ThreadToUse.POOLED_THREAD, this);

    AsyncVfsEventsPostProcessor.getInstance().addListener(this::filesChanged, this);
    //VcsRootChecker.EXTENSION_POINT_NAME.addChangeListener(this::scheduleScan, this);
    //VcsEP.EP_NAME.addChangeListener(this::scheduleScan, this);
  }

  @Override
  public void dispose() {
  }

  private void filesChanged(@Nonnull List<? extends VFileEvent> events) {
    List<VcsRootChecker> checkers = VcsRootChecker.EXTENSION_POINT_NAME.getExtensionList();
    if (checkers.isEmpty()) return;

    for (VFileEvent event : events) {
      VirtualFile file = event.getFile();
      if (file != null && file.isDirectory()) {
        visitDirsRecursivelyWithoutExcluded(myProject, file, true, dir -> {
          if (isVcsDir(checkers, dir.getName())) {
            scheduleScan();
            return skipTo(file);
          }
          return CONTINUE;
        });
      }
    }
  }

  static void visitDirsRecursivelyWithoutExcluded(@Nonnull Project project,
                                                  @Nonnull VirtualFile root,
                                                  boolean visitIgnoredFoldersThemselves,
                                                  @Nonnull Function<? super VirtualFile, Result> processor) {
    ProjectFileIndex fileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    Option depthLimit = limit(Registry.intValue("vcs.root.detector.folder.depth"));
    Pattern ignorePattern = parseDirIgnorePattern();

    if (isUnderIgnoredDirectory(project, ignorePattern, visitIgnoredFoldersThemselves ? root.getParent() : root)) return;

    VfsUtilCore.visitChildrenRecursively(root, new VirtualFileVisitor<Void>(NO_FOLLOW_SYMLINKS, depthLimit) {
      @Nonnull
      @Override
      public VirtualFileVisitor.Result visitFileEx(@Nonnull VirtualFile file) {
        if (!file.isDirectory()) {
          return CONTINUE;
        }

        if (visitIgnoredFoldersThemselves) {
          Result apply = processor.apply(file);
          if (apply != CONTINUE) {
            return apply;
          }
        }

        if (isIgnoredDirectory(project, ignorePattern, file)) {
          return SKIP_CHILDREN;
        }

        if (ReadAction.compute(() -> project.isDisposed() || !fileIndex.isInContent(file))) {
          return SKIP_CHILDREN;
        }

        if (!visitIgnoredFoldersThemselves) {
          Result apply = processor.apply(file);
          if (apply != CONTINUE) {
            return apply;
          }
        }

        return CONTINUE;
      }
    });
  }

  private static boolean isVcsDir(@Nonnull List<VcsRootChecker> checkers, @Nonnull String filePath) {
    return checkers.stream().anyMatch(it -> it.isVcsDir(filePath));
  }

  public void scheduleScan() {
    if (myAlarm.isDisposed()) return;
    if (VcsRootChecker.EXTENSION_POINT_NAME.getExtensionList().isEmpty()) return;

    myAlarm.cancelAllRequests(); // one scan is enough, no need to queue, they all do the same
    myAlarm.addRequest(() -> BackgroundTaskUtil.runUnderDisposeAwareIndicator(myAlarm, () -> {
      myRootProblemNotifier.rescanAndNotifyIfNeeded();
    }), WAIT_BEFORE_SCAN);
  }


  static boolean isUnderIgnoredDirectory(@Nonnull Project project, @Nullable Pattern ignorePattern, @Nullable VirtualFile dir) {
    VirtualFile parent = dir;
    while (parent != null) {
      if (isIgnoredDirectory(project, ignorePattern, parent)) return true;
      parent = parent.getParent();
    }
    return false;
  }

  private static boolean isIgnoredDirectory(@Nonnull Project project, @Nullable Pattern ignorePattern, @Nonnull VirtualFile dir) {
    if (ProjectLevelVcsManager.getInstance(project).isIgnored(dir)) {
      LOG.debug("Skipping ignored dir: ", dir);
      return true;
    }
    if (ignorePattern != null && ignorePattern.matcher(dir.getName()).matches()) {
      LOG.debug("Skipping dir by pattern: ", dir);
      return true;
    }
    return false;
  }

  @Nullable
  static Pattern parseDirIgnorePattern() {
    try {
      return Pattern.compile(Registry.stringValue("vcs.root.detector.ignore.pattern"));
    }
    catch (MissingResourceException | PatternSyntaxException e) {
      LOG.warn(e);
      return null;
    }
  }

  static final class DetectRootsStartupActivity implements VcsStartupActivity {
    @Override
    public void runActivity(@Nonnull Project project) {
      if (ApplicationManager.getApplication().isUnitTestMode()) return;
      getInstance(project).scheduleScan();
    }

    @Override
    public int getOrder() {
      return VcsInitObject.AFTER_COMMON.getOrder();
    }
  }
}
