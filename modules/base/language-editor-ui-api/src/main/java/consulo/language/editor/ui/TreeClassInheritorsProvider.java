/*
 * Copyright 2013-2022 consulo.io
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
package consulo.language.editor.ui;

import consulo.application.util.query.Query;
import consulo.language.psi.PsiNamedElement;
import consulo.project.content.scope.ProjectAwareSearchScope;

import javax.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 25-Jul-22
 */
public abstract class TreeClassInheritorsProvider<T extends PsiNamedElement> {
  private final T myBaseClass;
  private final ProjectAwareSearchScope myScope;

  public TreeClassInheritorsProvider(T baseClass, ProjectAwareSearchScope scope) {
    myBaseClass = baseClass;
    myScope = scope;
  }

  public T getBaseClass() {
    return myBaseClass;
  }

  public ProjectAwareSearchScope getScope() {
    return myScope;
  }

  @Nonnull
  public abstract Query<T> searchForInheritors(T baseClass, ProjectAwareSearchScope searchScope, boolean checkDeep);

  public abstract boolean isInheritor(T clazz, T baseClass, boolean checkDeep);

  public abstract String[] getNames();

  public Query<T> searchForInheritorsOfBaseClass() {
    return searchForInheritors(myBaseClass, myScope, true);
  }

  public boolean isInheritorOfBaseClass(T aClass) {
    return isInheritor(aClass, myBaseClass, true);
  }
}