// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.util;

import consulo.application.ReadAction;
import consulo.project.Project;
import consulo.util.lang.StringLenComparator;
import consulo.versionControlSystem.AbstractVcs;
import consulo.versionControlSystem.FilePath;
import consulo.versionControlSystem.ProjectLevelVcsManager;
import consulo.versionControlSystem.VirtualFileFilter;
import consulo.versionControlSystem.change.VcsDirtyScope;
import consulo.versionControlSystem.root.VcsRoot;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.util.VirtualFileUtil;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.function.Predicate;

public class VcsRootIterator {
  // folder path to files to be excluded
  private final Map<String, MyRootFilter> myOtherVcsFolders;
  private final ProjectLevelVcsManager myVcsManager;
  private final Project myProject;

  public VcsRootIterator(Project project, AbstractVcs vcs) {
    myProject = project;
    myVcsManager = ProjectLevelVcsManager.getInstance(project);
    myOtherVcsFolders = new HashMap<>();

    VcsRoot[] allRoots = myVcsManager.getAllVcsRoots();
    VirtualFile[] roots = myVcsManager.getRootsUnderVcs(vcs);
    for (VirtualFile root : roots) {
      MyRootFilter rootPresentFilter = new MyRootFilter(root, vcs.getId());
      rootPresentFilter.init(allRoots);
      myOtherVcsFolders.put(root.getUrl(), rootPresentFilter);
    }
  }

  public boolean acceptFolderUnderVcs(VirtualFile vcsRoot, VirtualFile file) {
    String vcsUrl = vcsRoot.getUrl();
    MyRootFilter rootFilter = myOtherVcsFolders.get(vcsUrl);
    if ((rootFilter != null) && (!rootFilter.accept(file))) {
      return false;
    }
    return !isIgnoredByVcs(myVcsManager, myProject, file);
  }

  private static boolean isIgnoredByVcs(ProjectLevelVcsManager vcsManager, Project project, VirtualFile file) {
    return ReadAction.compute(() -> project.isDisposed() || vcsManager.isIgnored(file));
  }

  private static final class MyRootFilter {
    private final VirtualFile myRoot;
    private final String myVcsName;

    // virtual file URLs
    private final List<String> myExcludedByOthers;

    private MyRootFilter(VirtualFile root, String vcsName) {
      myRoot = root;
      myVcsName = vcsName;

      myExcludedByOthers = new ArrayList<>();
    }

    private void init(VcsRoot[] allRoots) {
      String ourPath = myRoot.getUrl();

      for (VcsRoot root : allRoots) {
        AbstractVcs vcs = root.getVcs();
        if (vcs == null || Objects.equals(vcs.getName(), myVcsName)) continue;
        String url = root.getPath().getUrl();
        if (url.startsWith(ourPath)) {
          myExcludedByOthers.add(url);
        }
      }

      myExcludedByOthers.sort(StringLenComparator.getDescendingInstance());
    }

    public boolean accept(VirtualFile vf) {
      String url = vf.getUrl();
      for (String excludedByOtherVcs : myExcludedByOthers) {
        // use the fact that they are sorted
        if (url.length() > excludedByOtherVcs.length()) return true;
        if (url.startsWith(excludedByOtherVcs)) return false;
      }
      return true;
    }
  }

  public static void iterateVfUnderVcsRoot(Project project,
                                           VirtualFile root,
                                           Predicate<? super VirtualFile> processor) {
    MyRootIterator rootIterator = new MyRootIterator(project, root, null, processor, null);
    rootIterator.iterate();
  }

  public static void iterateVcsRoot(Project project,
                                    VirtualFile root,
                                    Predicate<? super FilePath> processor) {
    iterateVcsRoot(project, root, processor, null);
  }

  public static void iterateVcsRoot(Project project,
                                    VirtualFile root,
                                    Predicate<? super FilePath> processor,
                                    @Nullable VirtualFileFilter directoryFilter) {
    MyRootIterator rootIterator = new MyRootIterator(project, root, processor, null, directoryFilter);
    rootIterator.iterate();
  }

  private static final class MyRootIterator {
    private final Project myProject;
    private final Predicate<? super FilePath> myPathProcessor;
    private final Predicate<? super VirtualFile> myFileProcessor;
    @Nullable
    private final VirtualFileFilter myDirectoryFilter;
    private final VirtualFile myRoot;
    private final MyRootFilter myRootPresentFilter;
    private final ProjectLevelVcsManager myVcsManager;

    private MyRootIterator(Project project,
                           VirtualFile root,
                           @Nullable Predicate<? super FilePath> pathProcessor,
                           @Nullable Predicate<? super VirtualFile> fileProcessor,
                           @Nullable VirtualFileFilter directoryFilter) {
      myProject = project;
      myPathProcessor = pathProcessor;
      myFileProcessor = fileProcessor;
      myDirectoryFilter = directoryFilter;
      myRoot = root;

      myVcsManager = ProjectLevelVcsManager.getInstance(project);
      AbstractVcs vcs = myVcsManager.getVcsFor(root);
      myRootPresentFilter = vcs == null ? null : new MyRootFilter(root, vcs.getName());
      if (myRootPresentFilter != null) {
        myRootPresentFilter.init(myVcsManager.getAllVcsRoots());
      }
    }

    public void iterate() {
      VirtualFileUtil.visitChildrenRecursively(myRoot, new VirtualFileVisitor<Void>(VirtualFileVisitor.NO_FOLLOW_SYMLINKS) {
        @Override
        public void afterChildrenVisited(@Nonnull VirtualFile file) {
          if (myDirectoryFilter != null) {
            myDirectoryFilter.afterChildrenVisited(file);
          }
        }

        @Nonnull
        @Override
        public Result visitFileEx(@Nonnull VirtualFile file) {
          if (isIgnoredByVcs(myVcsManager, myProject, file)) return SKIP_CHILDREN;
          if (myRootPresentFilter != null && !myRootPresentFilter.accept(file)) return SKIP_CHILDREN;
          if (myProject.isDisposed() || !process(file)) return skipTo(myRoot);
          if (myDirectoryFilter != null && file.isDirectory() && !myDirectoryFilter.shouldGoIntoDirectory(file)) return SKIP_CHILDREN;
          return CONTINUE;
        }
      });
    }

    private boolean process(VirtualFile current) {
      if (myPathProcessor != null) {
        return myPathProcessor.test(VcsUtil.getFilePath(current));
      }
      else if (myFileProcessor != null) {
        return myFileProcessor.test(current);
      }
      return false;
    }
  }

  /**
   * Invoke the {@code iterator} for all files in the dirty scope.
   * For recursively dirty directories all children are processed.
   */
  @SuppressWarnings("ReturnValueIgnored")
  public static void iterate(@Nonnull VcsDirtyScope scope, @Nonnull Predicate<? super FilePath> iterator) {
    Project project = scope.getProject();
    if (project.isDisposed()) return;

    for (FilePath dir : scope.getRecursivelyDirtyDirectories()) {
      VirtualFile vFile = dir.getVirtualFile();
      if (vFile != null && vFile.isValid()) {
        iterateVcsRoot(project, vFile, iterator);
      }
    }

    for (FilePath file : scope.getDirtyFilesNoExpand()) {
      iterator.test(file);
      VirtualFile vFile = file.getVirtualFile();
      if (vFile != null && vFile.isValid() && vFile.isDirectory()) {
        for (VirtualFile child : vFile.getChildren()) {
          iterator.test(VcsUtil.getFilePath(child));
        }
      }
    }
  }

  @SuppressWarnings("ReturnValueIgnored")
  public static void iterateExistingInsideScope(@Nonnull VcsDirtyScope scope, @Nonnull Predicate<? super VirtualFile> iterator) {
    Project project = scope.getProject();
    if (project.isDisposed()) return;

    for (FilePath dir : scope.getRecursivelyDirtyDirectories()) {
      VirtualFile vFile = obtainVirtualFile(dir);
      if (vFile != null && vFile.isValid()) {
        iterateVfUnderVcsRoot(project, vFile, iterator);
      }
    }

    for (FilePath file : scope.getDirtyFilesNoExpand()) {
      VirtualFile vFile = obtainVirtualFile(file);
      if (vFile != null && vFile.isValid()) {
        iterator.test(vFile);
        if (vFile.isDirectory()) {
          for (VirtualFile child : vFile.getChildren()) {
            iterator.test(child);
          }
        }
      }
    }
  }

  @Nullable
  private static VirtualFile obtainVirtualFile(FilePath file) {
    VirtualFile vFile = file.getVirtualFile();
    return vFile == null ? VirtualFileUtil.findFileByIoFile(file.getIOFile(), false) : vFile;
  }
}
