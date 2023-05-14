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

package consulo.ide.impl.psi.impl.cache.impl;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.application.ApplicationManager;
import consulo.application.dumb.IndexNotReadyException;
import consulo.application.progress.ProgressIndicatorProvider;
import consulo.application.util.ReadActionProcessor;
import consulo.application.util.function.CommonProcessors;
import consulo.application.util.function.Computable;
import consulo.application.util.function.Processor;
import consulo.component.ProcessCanceledException;
import consulo.language.cacheBuilder.CacheManager;
import consulo.ide.impl.psi.impl.cache.impl.id.IdIndex;
import consulo.language.psi.stub.IdIndexEntry;
import consulo.language.content.FileIndexFacade;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.psi.scope.GlobalSearchScope;
import consulo.language.psi.stub.FileBasedIndex;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import jakarta.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Zhuravlev
 *         Date: Jan 16, 2008
 */
@Singleton
@ServiceImpl
public class IndexCacheManagerImpl extends CacheManager {
  private final Project myProject;
  private final PsiManager myPsiManager;

  @Inject
  public IndexCacheManagerImpl(PsiManager psiManager) {
    myPsiManager = psiManager;
    myProject = psiManager.getProject();
  }

  @Override
  @Nonnull
  public PsiFile[] getFilesWithWord(@Nonnull final String word, final short occurenceMask, @Nonnull final GlobalSearchScope scope, final boolean caseSensitively) {
    if (myProject.isDefault()) {
      return PsiFile.EMPTY_ARRAY;
    }
    CommonProcessors.CollectProcessor<PsiFile> processor = new CommonProcessors.CollectProcessor<PsiFile>();
    processFilesWithWord(processor, word, occurenceMask, scope, caseSensitively);
    return processor.getResults().isEmpty() ? PsiFile.EMPTY_ARRAY : processor.toArray(PsiFile.EMPTY_ARRAY);
  }

  @Override
  @Nonnull
  public VirtualFile[] getVirtualFilesWithWord(@Nonnull final String word, final short occurenceMask, @Nonnull final GlobalSearchScope scope, final boolean caseSensitively) {
    if (myProject.isDefault()) {
      return VirtualFile.EMPTY_ARRAY;
    }

    final List<VirtualFile> vFiles = new ArrayList<VirtualFile>(5);
    collectVirtualFilesWithWord(new CommonProcessors.CollectProcessor<VirtualFile>(vFiles), word, occurenceMask, scope, caseSensitively);
    return vFiles.isEmpty() ? VirtualFile.EMPTY_ARRAY : vFiles.toArray(new VirtualFile[vFiles.size()]);
  }

  // IMPORTANT!!!
  // Since implementation of virtualFileProcessor.process() may call indices directly or indirectly,
  // we cannot call it inside FileBasedIndex.processValues() method except in collecting form
  // If we do, deadlocks are possible (IDEADEV-42137). Process the files without not holding indices' read lock.
  private boolean collectVirtualFilesWithWord(@Nonnull final Processor<VirtualFile> fileProcessor,
                                              @Nonnull final String word, final short occurrenceMask,
                                              @Nonnull final GlobalSearchScope scope, final boolean caseSensitively) {
    if (myProject.isDefault()) {
      return true;
    }

    try {
      return ApplicationManager.getApplication().runReadAction(new Computable<Boolean>() {
        @Override
        public Boolean compute() {
          return FileBasedIndex.getInstance().processValues(IdIndex.NAME, new IdIndexEntry(word, caseSensitively), null, new FileBasedIndex.ValueProcessor<Integer>() {
            final FileIndexFacade index = FileIndexFacade.getInstance(myProject);
            @Override
            public boolean process(final VirtualFile file, final Integer value) {
              ProgressIndicatorProvider.checkCanceled();
              final int mask = value.intValue();
              if ((mask & occurrenceMask) != 0 && index.shouldBeFound(scope, file)) {
                if (!fileProcessor.process(file)) return false;
              }
              return true;
            }
          }, scope);
        }
      });
    }
    catch (IndexNotReadyException e) {
      throw new ProcessCanceledException();
    }
  }

  @Override
  public boolean processFilesWithWord(@Nonnull final Processor<PsiFile> psiFileProcessor, @Nonnull final String word, final short occurrenceMask, @Nonnull final GlobalSearchScope scope, final boolean caseSensitively) {
    final List<VirtualFile> vFiles = new ArrayList<VirtualFile>(5);
    collectVirtualFilesWithWord(new CommonProcessors.CollectProcessor<VirtualFile>(vFiles), word, occurrenceMask, scope, caseSensitively);
    if (vFiles.isEmpty()) return true;

    final Processor<VirtualFile> virtualFileProcessor = new ReadActionProcessor<VirtualFile>() {
      @RequiredReadAction
      @Override
      public boolean processInReadAction(VirtualFile virtualFile) {
        if (virtualFile.isValid()) {
          final PsiFile psiFile = myPsiManager.findFile(virtualFile);
          return psiFile == null || psiFileProcessor.process(psiFile);
        }
        return true;
      }
    };


    // IMPORTANT!!!
    // Since implementation of virtualFileProcessor.process() may call indices directly or indirectly,
    // we cannot call it inside FileBasedIndex.processValues() method
    // If we do, deadlocks are possible (IDEADEV-42137). So first we obtain files with the word specified,
    // and then process them not holding indices' read lock.
    for (VirtualFile vFile : vFiles) {
      ProgressIndicatorProvider.checkCanceled();
      if (!virtualFileProcessor.process(vFile)) {
        return false;
      }
    }
    return true;
  }
}
