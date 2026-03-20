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

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.language.psi.PsiNamedElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.content.scope.ProjectAwareSearchScope;

import org.jspecify.annotations.Nullable;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 25-Jul-22
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface TreeClassChooserFactory {
  @FunctionalInterface
  interface ClassProvider<T extends PsiNamedElement> {
    
    Collection<T> getClassesByName(Project project, String name, boolean searchInLibraries, String pattern, ProjectAwareSearchScope searchScope);
  }

  @FunctionalInterface
  interface InheritorsProviderFactory<T extends PsiNamedElement> {
    @Nullable TreeClassInheritorsProvider<T> create(T baseClass, ProjectAwareSearchScope scope);
  }

  interface Builder<T extends PsiNamedElement> {
    
    Builder<T> withTitle(LocalizeValue title);

    
    Builder<T> withSearchScope(ProjectAwareSearchScope searchScope);

    
    Builder<T> withClassFilter(Predicate<T> classFilter);

    
    Builder<T> withBaseClass(T baseClass);

    
    Builder<T> withInitialClass(T initialClass);

    
    Builder<T> withShowMembers(boolean value);

    
    Builder<T> withShowLibraryContents(boolean value);

    
    Builder<T> withClassProvider(ClassProvider<T> classProvider);

    
    Builder<T> withInheritorsProvider(InheritorsProviderFactory<T> inheritorsProvider);

    /**
     * return converter, which converter user data from project tree node, to target element
     */
    
    Builder<T> withTreeElementConverter(Function<Object, T> userObjectElementConverter);

    
    TreeChooser<T> build();
  }

  static TreeClassChooserFactory getInstance(Project project) {
    return project.getInstance(TreeClassChooserFactory.class);
  }

  
  <T extends PsiNamedElement> Builder<T> newChooser(Class<T> elementClazz);
}
