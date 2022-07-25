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

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 25-Jul-22
 */
@ServiceAPI(ComponentScope.PROJECT)
public interface TreeClassChooserFactory {
  interface ClassProvider<T extends PsiNamedElement> {
    List<T> getClassesByName(final String name, final boolean searchInLibraries, final String pattern, final ProjectAwareSearchScope searchScope);
  }

  interface InheritorsProviderFactory<T extends PsiNamedElement> {
    TreeClassInheritorsProvider<T> create(@Nonnull T baseClass, @Nonnull ProjectAwareSearchScope scope);
  }

  interface Builder<T extends PsiNamedElement> {
    @Nonnull
    Builder<T> withTitle(@Nonnull LocalizeValue title);

    @Nonnull
    Builder<T> witSearchScope(@Nonnull ProjectAwareSearchScope searchScope);

    @Nonnull
    Builder<T> withClassFilter(@Nonnull Predicate<T> classFilter);

    @Nonnull
    Builder<T> withBaseClass(T baseClass);

    @Nonnull
    Builder<T> withInitialClass(T initialClass);

    @Nonnull
    Builder<T> withShowMembers(boolean value);

    @Nonnull
    Builder<T> withShowLibraryContents(boolean value);

    @Nonnull
    Builder<T> withClassProvider(@Nonnull ClassProvider<T> classProvider);

    @Nonnull
    Builder<T> withInheritorsProvider(@Nonnull InheritorsProviderFactory<T> inheritorsProvider);

    /**
     * return converter, which converter user data from project tree node, to target element
     */
    @Nonnull
    Builder<T> withTreeElementConverter(@Nonnull Function<Object, T> userObjectElementConverter);

    @Nonnull
    TreeChooser<T> build();
  }

  static TreeClassChooserFactory getInstance(Project project) {
    return project.getInstance(TreeClassChooserFactory.class);
  }

  @Nonnull
  <T extends PsiNamedElement> Builder<T> newChooser(@Nonnull Class<T> elementClazz);
}
