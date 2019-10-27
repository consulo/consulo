// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.

/*
 * @author max
 */
package com.intellij.psi.stubs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ObjectUtils;
import com.intellij.util.Processor;
import com.intellij.util.Processors;
import com.intellij.util.SmartList;
import com.intellij.util.indexing.IdFilter;
import com.intellij.util.indexing.IdIterator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;

public abstract class StubIndex {
  private static class StubIndexHolder {
    private static final StubIndex ourInstance = ApplicationManager.getApplication().getComponent(StubIndex.class);
  }

  public static StubIndex getInstance() {
    return StubIndexHolder.ourInstance;
  }

  /**
   * @deprecated use {@link #getElements(StubIndexKey, Object, Project, GlobalSearchScope, Class)}
   */
  @Deprecated
  public <Key, Psi extends PsiElement> Collection<Psi> get(@Nonnull StubIndexKey<Key, Psi> indexKey, @Nonnull Key key, @Nonnull Project project, @Nullable final GlobalSearchScope scope) {
    List<Psi> result = new SmartList<>();
    processElements(indexKey, key, project, scope, (Class<Psi>)PsiElement.class, Processors.cancelableCollectProcessor(result));
    return result;
  }

  public <Key, Psi extends PsiElement> boolean processElements(@Nonnull StubIndexKey<Key, Psi> indexKey,
                                                               @Nonnull Key key,
                                                               @Nonnull Project project,
                                                               @Nullable GlobalSearchScope scope,
                                                               @Nonnull Class<Psi> requiredClass,
                                                               @Nonnull Processor<? super Psi> processor) {
    return processElements(indexKey, key, project, scope, null, requiredClass, processor);
  }

  public <Key, Psi extends PsiElement> boolean processElements(@Nonnull StubIndexKey<Key, Psi> indexKey,
                                                               @Nonnull Key key,
                                                               @Nonnull Project project,
                                                               @Nullable GlobalSearchScope scope,
                                                               @Nullable IdFilter idFilter,
                                                               @Nonnull Class<Psi> requiredClass,
                                                               @Nonnull Processor<? super Psi> processor) {
    return processElements(indexKey, key, project, scope, requiredClass, processor);
  }

  @Nonnull
  public abstract <Key> Collection<Key> getAllKeys(@Nonnull StubIndexKey<Key, ?> indexKey, @Nonnull Project project);

  public <K> boolean processAllKeys(@Nonnull StubIndexKey<K, ?> indexKey, @Nonnull Project project, @Nonnull Processor<? super K> processor) {
    return processAllKeys(indexKey, processor, GlobalSearchScope.allScope(project), null);
  }

  public <K> boolean processAllKeys(@Nonnull StubIndexKey<K, ?> indexKey, @Nonnull Processor<? super K> processor, @Nonnull GlobalSearchScope scope, @Nullable IdFilter idFilter) {
    return processAllKeys(indexKey, ObjectUtils.assertNotNull(scope.getProject()), processor);
  }

  /**
   * @deprecated use {@link #getElements(StubIndexKey, Object, Project, GlobalSearchScope, Class)}
   */
  @Deprecated
  @Nonnull
  public <Key, Psi extends PsiElement> Collection<Psi> safeGet(@Nonnull StubIndexKey<Key, Psi> indexKey,
                                                               @Nonnull Key key,
                                                               @Nonnull final Project project,
                                                               @Nullable GlobalSearchScope scope,
                                                               @Nonnull Class<Psi> requiredClass) {
    return getElements(indexKey, key, project, scope, requiredClass);
  }

  @Nonnull
  public static <Key, Psi extends PsiElement> Collection<Psi> getElements(@Nonnull StubIndexKey<Key, Psi> indexKey,
                                                                          @Nonnull Key key,
                                                                          @Nonnull final Project project,
                                                                          @Nullable final GlobalSearchScope scope,
                                                                          @Nonnull Class<Psi> requiredClass) {
    return getElements(indexKey, key, project, scope, null, requiredClass);
  }

  @Nonnull
  public static <Key, Psi extends PsiElement> Collection<Psi> getElements(@Nonnull StubIndexKey<Key, Psi> indexKey,
                                                                          @Nonnull Key key,
                                                                          @Nonnull final Project project,
                                                                          @Nullable final GlobalSearchScope scope,
                                                                          @Nullable IdFilter idFilter,
                                                                          @Nonnull Class<Psi> requiredClass) {
    final List<Psi> result = new SmartList<>();
    Processor<Psi> processor = Processors.cancelableCollectProcessor(result);
    getInstance().processElements(indexKey, key, project, scope, idFilter, requiredClass, processor);
    return result;
  }

  @Nonnull
  public abstract <Key> IdIterator getContainingIds(@Nonnull StubIndexKey<Key, ?> indexKey, @Nonnull Key dataKey, @Nonnull Project project, @Nonnull final GlobalSearchScope scope);

  public abstract void forceRebuild(@Nonnull Throwable e);
}
