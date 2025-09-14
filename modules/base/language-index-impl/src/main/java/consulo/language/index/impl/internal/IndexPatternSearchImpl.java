// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.index.impl.internal;

import consulo.annotation.component.ServiceImpl;
import consulo.language.psi.PsiFile;
import consulo.language.psi.search.IndexPattern;
import consulo.language.psi.search.IndexPatternProvider;
import consulo.language.psi.search.IndexPatternSearch;
import consulo.language.psi.stub.todo.TodoCacheManager;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

/**
 * @author yole
 */
@Singleton
@ServiceImpl
public final class IndexPatternSearchImpl extends IndexPatternSearch {
  @Inject
  public IndexPatternSearchImpl() {
    registerExecutor(new IndexPatternSearcher());
  }

  @Override
  protected int getOccurrencesCountImpl(@Nonnull PsiFile file, @Nonnull IndexPatternProvider provider) {
    int count = TodoCacheManager.getInstance(file.getProject()).getTodoCount(file.getVirtualFile(), provider);
    if (count != -1) return count;
    return search(file, provider).findAll().size();
  }

  @Override
  protected int getOccurrencesCountImpl(@Nonnull PsiFile file, @Nonnull IndexPattern pattern) {
    int count = TodoCacheManager.getInstance(file.getProject()).getTodoCount(file.getVirtualFile(), pattern);
    if (count != -1) return count;
    return search(file, pattern).findAll().size();
  }
}
