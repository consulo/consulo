/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

/*
 * @author max
 */
package com.intellij.psi.stubs;

import com.intellij.openapi.application.ApplicationManager;
import consulo.logging.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.ObjectUtil;
import com.intellij.util.Processor;
import com.intellij.util.Processors;
import com.intellij.util.SmartList;
import com.intellij.util.indexing.IdFilter;
import com.intellij.util.indexing.IdIterator;
import consulo.annotations.Exported;
import consulo.ui.RequiredUIAccess;
import consulo.annotations.RequiredReadAction;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.util.Collection;
import java.util.List;

public abstract class StubIndex {
  private static final Logger LOG = Logger.getInstance("#com.intellij.psi.stubs.StubIndex");

  private static class StubIndexHolder {
    private static final StubIndex ourInstance = ApplicationManager.getApplication().getComponent(StubIndex.class);
  }
  public static StubIndex getInstance() {
    return StubIndexHolder.ourInstance;
  }

  /**
   * @deprecated use {@link #getElements(StubIndexKey, Object, com.intellij.openapi.project.Project, com.intellij.psi.search.GlobalSearchScope, Class)}
   */
  public abstract <Key, Psi extends PsiElement> Collection<Psi> get(@Nonnull StubIndexKey<Key, Psi> indexKey,
                                                                    @Nonnull Key key,
                                                                    @Nonnull Project project,
                                                                    @javax.annotation.Nullable final GlobalSearchScope scope);

  /**
   * @deprecated use {@link #getElements(StubIndexKey, Object, com.intellij.openapi.project.Project, com.intellij.psi.search.GlobalSearchScope, Class)}
   */
  public <Key, Psi extends PsiElement> Collection<Psi> get(@Nonnull StubIndexKey<Key, Psi> indexKey,
                                                           @Nonnull Key key,
                                                           @Nonnull Project project,
                                                           @javax.annotation.Nullable final GlobalSearchScope scope,
                                                           IdFilter filter) {
    return get(indexKey, key, project, scope);
  }

  /**
   * @deprecated use processElements
   */
  public <Key, Psi extends PsiElement> boolean process(@Nonnull StubIndexKey<Key, Psi> indexKey,
                                                       @Nonnull Key key,
                                                       @Nonnull Project project,
                                                       @javax.annotation.Nullable GlobalSearchScope scope,
                                                       @Nonnull Processor<? super Psi> processor) {
    return processElements(indexKey, key, project, scope, (Class<Psi>)PsiElement.class, processor);
  }

  public abstract <Key, Psi extends PsiElement> boolean processElements(@Nonnull StubIndexKey<Key, Psi> indexKey,
                                                                        @Nonnull Key key,
                                                                        @Nonnull Project project,
                                                                        @javax.annotation.Nullable GlobalSearchScope scope,
                                                                        Class<Psi> requiredClass,
                                                                        @Nonnull Processor<? super Psi> processor);

  /**
   * @deprecated use processElements
   */
  public <Key, Psi extends PsiElement> boolean process(@Nonnull StubIndexKey<Key, Psi> indexKey,
                                                       @Nonnull Key key,
                                                       @Nonnull Project project,
                                                       @Nullable GlobalSearchScope scope,
                                                       @SuppressWarnings("UnusedParameters") IdFilter idFilter,
                                                       @Nonnull Processor<? super Psi> processor) {
    return process(indexKey, key, project, scope, processor);
  }

  public <Key, Psi extends PsiElement> boolean processElements(@Nonnull StubIndexKey<Key, Psi> indexKey,
                                                               @Nonnull Key key,
                                                               @Nonnull Project project,
                                                               @javax.annotation.Nullable GlobalSearchScope scope,
                                                               IdFilter idFilter,
                                                               @Nonnull Class<Psi> requiredClass,
                                                               @Nonnull Processor<? super Psi> processor) {
    return process(indexKey, key, project, scope, processor);
  }

  @Nonnull
  public abstract <Key> Collection<Key> getAllKeys(@Nonnull StubIndexKey<Key, ?> indexKey, @Nonnull Project project);

  public abstract <K> boolean processAllKeys(@Nonnull StubIndexKey<K, ?> indexKey, @Nonnull Project project, Processor<K> processor);

  public <K> boolean processAllKeys(@Nonnull StubIndexKey<K, ?> indexKey, @Nonnull Processor<K> processor,
                                    @Nonnull GlobalSearchScope scope, @javax.annotation.Nullable IdFilter idFilter) {
    return processAllKeys(indexKey, ObjectUtil.assertNotNull(scope.getProject()), processor);
  }

  /**
   * @deprecated use {@link #getElements(StubIndexKey, Object, com.intellij.openapi.project.Project, com.intellij.psi.search.GlobalSearchScope, Class)}
   */
  public <Key, Psi extends PsiElement> Collection<Psi> safeGet(@Nonnull StubIndexKey<Key, Psi> indexKey,
                                                               @Nonnull Key key,
                                                               @Nonnull final Project project,
                                                               final GlobalSearchScope scope,
                                                               @Nonnull Class<Psi> requiredClass) {
    return getElements(indexKey, key, project, scope, requiredClass);
  }

  public static <Key, Psi extends PsiElement> Collection<Psi> getElements(@Nonnull StubIndexKey<Key, Psi> indexKey,
                                                                          @Nonnull Key key,
                                                                          @Nonnull final Project project,
                                                                          @javax.annotation.Nullable final GlobalSearchScope scope,
                                                                          @Nonnull Class<Psi> requiredClass) {
    return getElements(indexKey, key, project, scope, null, requiredClass);
  }

  public static <Key, Psi extends PsiElement> Collection<Psi> getElements(@Nonnull StubIndexKey<Key, Psi> indexKey,
                                                                          @Nonnull Key key,
                                                                          @Nonnull final Project project,
                                                                          @javax.annotation.Nullable final GlobalSearchScope scope,
                                                                          @Nullable IdFilter idFilter,
                                                                          @Nonnull Class<Psi> requiredClass) {
    final List<Psi> result = new SmartList<>();
    Processor<Psi> processor = Processors.cancelableCollectProcessor(result);
    getInstance().processElements(indexKey, key, project, scope, idFilter, requiredClass, processor);
    return result;
  }

  @Nonnull
  @Exported
  public abstract <Key> IdIterator getContainingIds(@Nonnull StubIndexKey<Key, ?> indexKey, @Nonnull Key dataKey,
                                                    @Nonnull Project project,
                                                    @Nonnull final GlobalSearchScope scope);

  @RequiredUIAccess
  @RequiredReadAction
  protected <Psi extends PsiElement> void reportStubPsiMismatch(Psi psi, VirtualFile file, Class<Psi> requiredClass) {
    LOG.error("Invalid stub element type in index: " + file + ". found: " + psi + ". expected: " + requiredClass);
  }
  public abstract void forceRebuild(@Nonnull Throwable e);
}
