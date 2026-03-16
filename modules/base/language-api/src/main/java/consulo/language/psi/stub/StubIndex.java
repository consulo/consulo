// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.language.psi.stub;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.application.ApplicationManager;
import consulo.application.util.function.Processors;
import consulo.index.io.IdIterator;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.project.content.scope.ProjectAwareSearchScope;
import consulo.project.content.scope.ProjectScopes;
import consulo.util.lang.ObjectUtil;

import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * @author max
 */
@ServiceAPI(value = ComponentScope.APPLICATION, lazy = false)
public abstract class StubIndex {
  private static class StubIndexHolder {
    private static final StubIndex ourInstance = ApplicationManager.getApplication().getComponent(StubIndex.class);
  }

  public static StubIndex getInstance() {
    return StubIndexHolder.ourInstance;
  }

  /**
   * @deprecated use {@link #getElements(StubIndexKey, Object, Project, ProjectAwareSearchScope, Class)}
   */
  @Deprecated
  public <Key, Psi extends PsiElement> Collection<Psi> get(StubIndexKey<Key, Psi> indexKey, Key key, Project project, @Nullable ProjectAwareSearchScope scope) {
    List<Psi> result = new ArrayList<Psi>();
    processElements(indexKey, key, project, scope, (Class<Psi>)PsiElement.class, Processors.cancelableCollectProcessor(result));
    return result;
  }

  public <Key, Psi extends PsiElement> boolean processElements(StubIndexKey<Key, Psi> indexKey,
                                                               Key key,
                                                               Project project,
                                                               @Nullable ProjectAwareSearchScope scope,
                                                               Class<Psi> requiredClass,
                                                               Predicate<? super Psi> processor) {
    return processElements(indexKey, key, project, scope, null, requiredClass, processor);
  }

  public <Key, Psi extends PsiElement> boolean processElements(StubIndexKey<Key, Psi> indexKey,
                                                               Key key,
                                                               Project project,
                                                               @Nullable ProjectAwareSearchScope scope,
                                                               @Nullable IdFilter idFilter,
                                                               Class<Psi> requiredClass,
                                                               Predicate<? super Psi> processor) {
    return processElements(indexKey, key, project, scope, requiredClass, processor);
  }

  
  public abstract <Key> Collection<Key> getAllKeys(StubIndexKey<Key, ?> indexKey, Project project);

  public <K> boolean processAllKeys(StubIndexKey<K, ?> indexKey, Project project, Predicate<? super K> processor) {
    return processAllKeys(indexKey, processor, ProjectScopes.getAllScope(project), null);
  }

  public <K> boolean processAllKeys(StubIndexKey<K, ?> indexKey, Predicate<? super K> processor, ProjectAwareSearchScope scope, @Nullable IdFilter idFilter) {
    return processAllKeys(indexKey, ObjectUtil.assertNotNull(scope.getProject()), processor);
  }

  /**
   * @deprecated use {@link #getElements(StubIndexKey, Object, Project, ProjectAwareSearchScope, Class)}
   */
  @Deprecated
  
  public <Key, Psi extends PsiElement> Collection<Psi> safeGet(StubIndexKey<Key, Psi> indexKey,
                                                               Key key,
                                                               Project project,
                                                               @Nullable ProjectAwareSearchScope scope,
                                                               Class<Psi> requiredClass) {
    return getElements(indexKey, key, project, scope, requiredClass);
  }

  
  public static <Key, Psi extends PsiElement> Collection<Psi> getElements(StubIndexKey<Key, Psi> indexKey,
                                                                          Key key,
                                                                          Project project,
                                                                          @Nullable ProjectAwareSearchScope scope,
                                                                          Class<Psi> requiredClass) {
    return getElements(indexKey, key, project, scope, null, requiredClass);
  }

  
  public static <Key, Psi extends PsiElement> Collection<Psi> getElements(StubIndexKey<Key, Psi> indexKey,
                                                                          Key key,
                                                                          Project project,
                                                                          @Nullable ProjectAwareSearchScope scope,
                                                                          @Nullable IdFilter idFilter,
                                                                          Class<Psi> requiredClass) {
    List<Psi> result = new ArrayList<Psi>();
    Predicate<Psi> processor = Processors.cancelableCollectProcessor(result);
    getInstance().processElements(indexKey, key, project, scope, idFilter, requiredClass, processor);
    return result;
  }

  
  public abstract <Key> IdIterator getContainingIds(StubIndexKey<Key, ?> indexKey, Key dataKey, Project project, ProjectAwareSearchScope scope);

  public abstract void forceRebuild(Throwable e);
}
