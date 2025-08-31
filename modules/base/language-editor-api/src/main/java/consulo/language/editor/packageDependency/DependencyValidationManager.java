/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.language.editor.packageDependency;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.content.scope.NamedScope;
import consulo.content.scope.NamedScopesHolder;
import consulo.content.scope.PackageSet;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.util.lang.Pair;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * @author anna
 * @since 2005-02-02
 */
@ServiceAPI(ComponentScope.PROJECT)
public abstract class DependencyValidationManager extends NamedScopesHolder {
  public DependencyValidationManager(Project project) {
    super(project);
  }

  public static DependencyValidationManager getInstance(Project project) {
    return project.getInstance(DependencyValidationManager.class);
  }

  public abstract boolean hasRules();

  @Nullable
  public abstract DependencyRule getViolatorDependencyRule(PsiFile from, PsiFile to);

  @Nonnull
  public abstract DependencyRule[] getViolatorDependencyRules(PsiFile from, PsiFile to);

  @Nonnull
  public abstract DependencyRule[] getApplicableRules(PsiFile file);

  public abstract DependencyRule[] getAllRules();

  public abstract void removeAllRules();

  public abstract void addRule(DependencyRule rule);

  public abstract boolean skipImportStatements();

  public abstract void setSkipImportStatements(boolean skip);

  public abstract Map<String,PackageSet> getUnnamedScopes();

  public abstract void reloadRules();

  @Nonnull
  public abstract List<Pair<NamedScope, NamedScopesHolder>> getScopeBasedHighlightingCachedScopes();
}
