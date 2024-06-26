// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.find.impl;

import consulo.application.Application;
import consulo.application.ApplicationManager;
import consulo.application.ReadAction;
import consulo.application.impl.internal.progress.CoreProgressManager;
import consulo.application.internal.TooManyUsagesStatus;
import consulo.application.progress.EmptyProgressIndicator;
import consulo.application.progress.ProgressIndicator;
import consulo.application.progress.ProgressManager;
import consulo.application.util.function.Processor;
import consulo.application.util.registry.Registry;
import consulo.component.ProcessCanceledException;
import consulo.content.ContentIterator;
import consulo.content.FileIndex;
import consulo.content.scope.SearchScope;
import consulo.find.FindBundle;
import consulo.find.FindModel;
import consulo.ide.impl.idea.find.FindInProjectSearchEngine;
import consulo.ide.impl.idea.find.findInProject.FindInProjectManager;
import consulo.ide.impl.idea.openapi.vfs.VfsUtilCore;
import consulo.ide.impl.idea.usages.UsageLimitUtil;
import consulo.ide.impl.idea.usages.impl.UsageViewManagerImpl;
import consulo.ide.impl.idea.util.containers.ContainerUtil;
import consulo.language.file.FileTypeManager;
import consulo.language.internal.PsiSearchHelperEx;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.PsiUtilCore;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.scope.GlobalSearchScopeUtil;
import consulo.language.psi.scope.LocalSearchScope;
import consulo.language.psi.search.PsiSearchHelper;
import consulo.logging.Logger;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.ProjectFileIndex;
import consulo.module.content.ProjectRootManager;
import consulo.module.content.layer.OrderEnumerator;
import consulo.project.Project;
import consulo.project.ProjectCoreUtil;
import consulo.ui.ex.awt.UIUtil;
import consulo.usage.FindUsagesProcessPresentation;
import consulo.usage.UsageInfo;
import consulo.util.lang.Pair;
import consulo.util.lang.StringUtil;
import consulo.util.lang.function.Condition;
import consulo.virtualFileSystem.RawFileLoader;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.VirtualFileFilter;
import consulo.virtualFileSystem.VirtualFileWithId;
import consulo.virtualFileSystem.internal.CompactVirtualFileSet;
import consulo.virtualFileSystem.util.VirtualFileVisitor;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * @author peter
 */
class FindInProjectTask {
  private static final Comparator<VirtualFile> SEARCH_RESULT_FILE_COMPARATOR =
          Comparator.comparing((VirtualFile f) -> f instanceof VirtualFileWithId ? ((VirtualFileWithId)f).getId() : 0).thenComparing(VirtualFile::getName) // in case files without id are also searched
                  .thenComparing(VirtualFile::getPath);
  private static final Logger LOG = Logger.getInstance(FindInProjectTask.class);
  private static final int FILES_SIZE_LIMIT = 70 * 1024 * 1024; // megabytes.
  private final FindModel myFindModel;
  private final Project myProject;
  private final PsiManager myPsiManager;
  @Nullable
  private final VirtualFile myDirectory;
  private final ProjectFileIndex myProjectFileIndex;
  private final FileIndex myFileIndex;
  private final Condition<VirtualFile> myFileMask;
  private final ProgressIndicator myProgress;
  @Nullable
  private final Module myModule;
  private final Set<VirtualFile> myLargeFiles = Collections.synchronizedSet(new HashSet<>());
  private final Set<? extends VirtualFile> myFilesToScanInitially;
  private final AtomicLong myTotalFilesSize = new AtomicLong();
  private final
  @Nonnull
  List<FindInProjectSearchEngine.FindInProjectSearcher> mySearchers;

  FindInProjectTask(@Nonnull final FindModel findModel, @Nonnull final Project project, @Nonnull Set<? extends VirtualFile> filesToScanInitially) {
    myFindModel = findModel;
    myProject = project;
    myFilesToScanInitially = filesToScanInitially;
    myDirectory = FindInProjectUtil.getDirectory(findModel);
    myPsiManager = PsiManager.getInstance(project);

    final String moduleName = findModel.getModuleName();
    myModule = moduleName == null ? null : ReadAction.compute(() -> ModuleManager.getInstance(project).findModuleByName(moduleName));
    myProjectFileIndex = ProjectRootManager.getInstance(project).getFileIndex();
    myFileIndex = myModule == null ? myProjectFileIndex : ModuleRootManager.getInstance(myModule).getFileIndex();

    Condition<CharSequence> patternCondition = FindInProjectUtil.createFileMaskCondition(findModel.getFileFilter());

    myFileMask = file -> file != null && patternCondition.value(file.getNameSequence());

    final ProgressIndicator progress = ProgressManager.getInstance().getProgressIndicator();
    myProgress = progress != null ? progress : new EmptyProgressIndicator();

    TooManyUsagesStatus.createFor(myProgress);

    mySearchers = ContainerUtil.mapNotNull(FindInProjectSearchEngine.getExtensions(), se -> se.createSearcher(findModel, project));
  }

  void findUsages(@Nonnull FindUsagesProcessPresentation processPresentation, @Nonnull Predicate<? super UsageInfo> consumer) {
    CoreProgressManager.assertUnderProgress(myProgress);

    try {
      myProgress.setIndeterminate(true);
      myProgress.setText(FindBundle.message("progress.text.scanning.indexed.files"));
      Set<VirtualFile> filesForFastWordSearch = ReadAction.compute(this::getFilesForFastWordSearch);
      myProgress.setIndeterminate(false);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Searching for " + myFindModel.getStringToFind() + " in " + filesForFastWordSearch.size() + " indexed files");
      }

      searchInFiles(filesForFastWordSearch, processPresentation, consumer);

      myProgress.setIndeterminate(true);
      myProgress.setText(FindBundle.message("progress.text.scanning.non.indexed.files"));
      boolean canRelyOnIndices = canRelyOnSearchers();
      final Collection<VirtualFile> otherFiles = collectFilesInScope(filesForFastWordSearch, canRelyOnIndices);
      myProgress.setIndeterminate(false);

      if (LOG.isDebugEnabled()) {
        LOG.debug("Searching for " + myFindModel.getStringToFind() + " in " + otherFiles.size() + " non-indexed files");
      }
      myProgress.checkCanceled();
      long start = System.currentTimeMillis();
      searchInFiles(otherFiles, processPresentation, consumer);
      if (canRelyOnIndices && otherFiles.size() > 1000) {
        long time = System.currentTimeMillis() - start;
        logStats(otherFiles, time);
      }
    }
    catch (ProcessCanceledException e) {
      processPresentation.setCanceled(true);
      if (LOG.isDebugEnabled()) {
        LOG.debug("Usage search canceled", e);
      }
    }

    if (!myLargeFiles.isEmpty()) {
      processPresentation.setLargeFilesWereNotScanned(myLargeFiles);
    }

    if (!myProgress.isCanceled()) {
      myProgress.setText(FindBundle.message("find.progress.search.completed"));
    }
  }

  private static void logStats(@Nonnull Collection<? extends VirtualFile> otherFiles, long time) {
    Map<String, Long> extensionToCount = otherFiles.stream().collect(Collectors.groupingBy(file -> StringUtil.toLowerCase(StringUtil.notNullize(file.getExtension())), Collectors.counting()));
    String topExtensions = extensionToCount.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).map(entry -> entry.getKey() + "(" + entry.getValue() + ")").limit(10)
            .collect(Collectors.joining(", "));

    LOG.info("Search in " + otherFiles.size() + " files with unknown types took " + time + "ms.\n" +
             "Mapping their extensions to an existing file type (e.g. Plain Text) might speed up the search.\n" +
             "Most frequent non-indexed file extensions: " + topExtensions);
  }

  private void searchInFiles(@Nonnull Collection<? extends VirtualFile> virtualFiles,
                             @Nonnull FindUsagesProcessPresentation processPresentation,
                             @Nonnull final Predicate<? super UsageInfo> consumer) {
    AtomicInteger occurrenceCount = new AtomicInteger();
    AtomicInteger processedFileCount = new AtomicInteger();
    Map<VirtualFile, Set<UsageInfo>> usagesBeingProcessed = new ConcurrentHashMap<>();
    Processor<VirtualFile> processor = virtualFile -> {
      if (!virtualFile.isValid()) return true;

      long fileLength = UsageViewManagerImpl.getFileLength(virtualFile);
      if (fileLength == -1) return true; // Binary or invalid

      final boolean skipProjectFile = ProjectCoreUtil.isProjectOrWorkspaceFile(virtualFile) && !myFindModel.isSearchInProjectFiles();
      if (skipProjectFile && !Registry.is("find.search.in.project.files")) return true;

      if (RawFileLoader.getInstance().isLargeForContentLoading(fileLength)) {
        myLargeFiles.add(virtualFile);
        return true;
      }

      myProgress.checkCanceled();
      if (myProgress.isRunning()) {
        double fraction = (double)processedFileCount.incrementAndGet() / virtualFiles.size();
        myProgress.setFraction(fraction);
      }
      myProgress.setText(FindBundle.message("find.searching.for.string.in.file.progress", myFindModel.getStringToFind(), virtualFile.getPresentableUrl()));
      myProgress.setText2(FindBundle.message("find.searching.for.string.in.file.occurrences.progress", occurrenceCount));

      Pair.NonNull<PsiFile, VirtualFile> pair = ReadAction.compute(() -> findFile(virtualFile));
      if (pair == null) return true;

      Set<UsageInfo> processedUsages = usagesBeingProcessed.computeIfAbsent(virtualFile, __ -> ContainerUtil.newConcurrentSet());
      PsiFile psiFile = pair.first;
      VirtualFile sourceVirtualFile = pair.second;
      AtomicBoolean projectFileUsagesFound = new AtomicBoolean();
      if (!FindInProjectUtil.processUsagesInFile(psiFile, sourceVirtualFile, myFindModel, info -> {
        if (skipProjectFile) {
          projectFileUsagesFound.set(true);
          return true;
        }
        if (processedUsages.contains(info)) {
          return true;
        }
        boolean success = consumer.test(info);
        processedUsages.add(info);
        return success;
      })) return false;
      usagesBeingProcessed.remove(virtualFile); // after the whole virtualFile processed successfully, remove mapping to save memory

      if (projectFileUsagesFound.get()) {
        processPresentation.projectFileUsagesFound(() -> {
          FindModel model = myFindModel.clone();
          model.setSearchInProjectFiles(true);
          FindInProjectManager.getInstance(myProject).startFindInProject(model);
        });
        return true;
      }

      long totalSize;
      if (processedUsages.isEmpty()) {
        totalSize = myTotalFilesSize.get();
      }
      else {
        occurrenceCount.addAndGet(processedUsages.size());
        totalSize = myTotalFilesSize.addAndGet(fileLength);
      }

      if (totalSize > FILES_SIZE_LIMIT) {
        TooManyUsagesStatus tooManyUsagesStatus = TooManyUsagesStatus.getFrom(myProgress);
        if (tooManyUsagesStatus.switchTooManyUsagesStatus()) {
          UIUtil.invokeLaterIfNeeded(() -> {
            String message = FindBundle.message("find.excessive.total.size.prompt", UsageViewManagerImpl.presentableSize(myTotalFilesSize.longValue()), Application.get().getName());
            UsageLimitUtil.Result ret = UsageLimitUtil.showTooManyUsagesWarning(myProject, message, processPresentation.getUsageViewPresentation());
            if (ret == UsageLimitUtil.Result.ABORT) {
              myProgress.cancel();
            }
            tooManyUsagesStatus.userResponded();
          });
        }
        tooManyUsagesStatus.pauseProcessingIfTooManyUsages();
        myProgress.checkCanceled();
      }
      return true;
    };
    List<VirtualFile> sorted = ContainerUtil.sorted(virtualFiles, SEARCH_RESULT_FILE_COMPARATOR);
    ((PsiSearchHelperEx)PsiSearchHelper.getInstance(myProject)).processFilesConcurrentlyDespiteWriteActions(myProject, sorted, myProgress, new AtomicBoolean(), processor);
  }

  // must return non-binary files
  @Nonnull
  private Collection<VirtualFile> collectFilesInScope(@Nonnull final Set<VirtualFile> alreadySearched, final boolean skipIndexed) {
    SearchScope customScope = myFindModel.isCustomScope() ? myFindModel.getCustomScope() : null;
    final GlobalSearchScope globalCustomScope = customScope == null ? null : GlobalSearchScopeUtil.toGlobalSearchScope(customScope, myProject);


    class EnumContentIterator implements ContentIterator {
      private final Set<VirtualFile> myFiles = new CompactVirtualFileSet();

      @Override
      public boolean processFile(@Nonnull final VirtualFile virtualFile) {
        ApplicationManager.getApplication().runReadAction(new Runnable() {
          @Override
          public void run() {
            ProgressManager.checkCanceled();
            if (virtualFile.isDirectory() || !virtualFile.isValid() || !myFileMask.value(virtualFile) || globalCustomScope != null && !globalCustomScope.contains(virtualFile)) {
              return;
            }

            if (skipIndexed && ContainerUtil.find(mySearchers, p -> p.isCovered(virtualFile)) != null) {
              return;
            }

            Pair.NonNull<PsiFile, VirtualFile> pair = findFile(virtualFile);
            if (pair == null) return;
            VirtualFile sourceVirtualFile = pair.second;

            if (sourceVirtualFile != null && !alreadySearched.contains(sourceVirtualFile)) {
              myFiles.add(sourceVirtualFile);
            }
          }
        });
        return true;
      }

      @Nonnull
      private Collection<VirtualFile> getFiles() {
        return myFiles;
      }
    }

    final EnumContentIterator iterator = new EnumContentIterator();

    if (customScope instanceof LocalSearchScope) {
      for (VirtualFile file : GlobalSearchScopeUtil.getLocalScopeFiles((LocalSearchScope)customScope)) {
        iterator.processFile(file);
      }
    }
    else if (customScope instanceof Iterable) {  // GlobalSearchScope can span files out of project roots e.g. FileScope / FilesScope
      //noinspection unchecked
      for (VirtualFile file : (Iterable<VirtualFile>)customScope) {
        iterator.processFile(file);
      }
    }
    else if (myDirectory != null) {
      boolean checkExcluded = !ProjectFileIndex.SERVICE.getInstance(myProject).isExcluded(myDirectory) && !Registry.is("find.search.in.excluded.dirs");
      VirtualFileVisitor.Option limit = VirtualFileVisitor.limit(myFindModel.isWithSubdirectories() ? -1 : 1);
      VfsUtilCore.visitChildrenRecursively(myDirectory, new VirtualFileVisitor<Void>(limit) {
        @Override
        public boolean visitFile(@Nonnull VirtualFile file) {
          if (checkExcluded && myProjectFileIndex.isExcluded(file)) return false;
          iterator.processFile(file);
          return true;
        }
      });
    }
    else {
      boolean success = myFileIndex.iterateContent(iterator);
      if (success && globalCustomScope != null && globalCustomScope.isSearchInLibraries()) {
        final VirtualFile[] librarySources = ReadAction.compute(() -> {
          OrderEnumerator enumerator = myModule == null ? OrderEnumerator.orderEntries(myProject) : OrderEnumerator.orderEntries(myModule);
          return enumerator.withoutModuleSourceEntries().withoutDepModules().getSourceRoots();
        });
        iterateAll(librarySources, globalCustomScope, iterator);
      }
    }
    return iterator.getFiles();
  }

  private static void iterateAll(@Nonnull VirtualFile[] files, @Nonnull final GlobalSearchScope searchScope, @Nonnull final ContentIterator iterator) {
    final FileTypeManager fileTypeManager = FileTypeManager.getInstance();
    final VirtualFileFilter contentFilter = file -> file.isDirectory() || !fileTypeManager.isFileIgnored(file) && !file.getFileType().isBinary() && searchScope.contains(file);
    for (VirtualFile file : files) {
      if (!VfsUtilCore.iterateChildrenRecursively(file, contentFilter, iterator)) break;
    }
  }

  private boolean canRelyOnSearchers() {
    return ContainerUtil.find(mySearchers, s -> s.isReliable()) != null;
  }

  @Nonnull
  private Set<VirtualFile> getFilesForFastWordSearch() {
    final Set<VirtualFile> resultFiles = new CompactVirtualFileSet();
    for (VirtualFile file : myFilesToScanInitially) {
      if (myFileMask.value(file)) {
        resultFiles.add(file);
      }
    }

    for (FindInProjectSearchEngine.FindInProjectSearcher searcher : mySearchers) {
      Collection<VirtualFile> virtualFiles = searcher.searchForOccurrences();
      for (VirtualFile file : virtualFiles) {
        if (myFileMask.value(file)) resultFiles.add(file);
      }
    }

    return resultFiles;
  }

  private Pair.NonNull<PsiFile, VirtualFile> findFile(@Nonnull final VirtualFile virtualFile) {
    PsiFile psiFile = myPsiManager.findFile(virtualFile);
    if (psiFile != null) {
      PsiElement sourceFile = psiFile.getNavigationElement();
      if (sourceFile instanceof PsiFile) psiFile = (PsiFile)sourceFile;
      if (psiFile.getFileType().isBinary()) {
        psiFile = null;
      }
    }
    VirtualFile sourceVirtualFile = PsiUtilCore.getVirtualFile(psiFile);
    if (psiFile == null || psiFile.getFileType().isBinary() || sourceVirtualFile == null || sourceVirtualFile.getFileType().isBinary()) {
      return null;
    }

    return Pair.createNonNull(psiFile, sourceVirtualFile);
  }
}
