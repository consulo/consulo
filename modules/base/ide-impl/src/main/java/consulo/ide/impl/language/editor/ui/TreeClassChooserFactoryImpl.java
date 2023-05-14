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
package consulo.ide.impl.language.editor.ui;

import consulo.annotation.component.ServiceImpl;
import consulo.ide.impl.idea.ide.util.AbstractTreeClassChooserDialog;
import consulo.language.editor.ui.TreeChooser;
import consulo.language.editor.ui.TreeClassChooserFactory;
import consulo.language.editor.ui.TreeClassInheritorsProvider;
import consulo.language.psi.PsiNamedElement;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.content.scope.ProjectAwareSearchScope;
import consulo.project.content.scope.ProjectScopes;
import jakarta.inject.Inject;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import javax.swing.tree.DefaultMutableTreeNode;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * @author VISTALL
 * @since 25-Jul-22
 */
@ServiceImpl
public class TreeClassChooserFactoryImpl implements TreeClassChooserFactory {
  private static class BuilderImpl<T extends PsiNamedElement> implements Builder<T> {
    private final Class<T> myElementClass;
    private final Project myProject;

    private LocalizeValue myTitle = LocalizeValue.localizeTODO("Choose Element");
    private ProjectAwareSearchScope myProjectAwareSearchScope;
    private Predicate<T> myClassFilter = t -> true;
    private T myBaseClass;
    private T myInitialClass;
    private boolean myShowMembers;
    private boolean myShowLibraryContents = true;
    private ClassProvider<T> myClassProvider = (project, name, searchInLibraries, pattern, searchScope) -> List.of();
    private InheritorsProviderFactory<T> myInheritorsProviderSupplier = (b, s) -> null;
    private Function<Object, T> myUserDataElementConverter = o -> null;

    public BuilderImpl(Project project, Class<T> elementClazz) {
      myProject = project;
      myElementClass = elementClazz;
      myProjectAwareSearchScope = ProjectScopes.getProjectScope(project);
    }

    @Nonnull
    @Override
    public Builder<T> withTitle(@Nonnull LocalizeValue title) {
      myTitle = title;
      return this;
    }

    @Nonnull
    @Override
    public Builder<T> withSearchScope(@Nonnull ProjectAwareSearchScope searchScope) {
      myProjectAwareSearchScope = searchScope;
      return this;
    }

    @Nonnull
    @Override
    public Builder<T> withClassFilter(@Nonnull Predicate<T> classFilter) {
      myClassFilter = classFilter;
      return this;
    }

    @Nonnull
    @Override
    public Builder<T> withBaseClass(T baseClass) {
      myBaseClass = baseClass;
      return this;
    }

    @Nonnull
    @Override
    public Builder<T> withInitialClass(T initialClass) {
      myInitialClass = initialClass;
      return this;
    }

    @Nonnull
    @Override
    public Builder<T> withShowMembers(boolean value) {
      myShowMembers = value;
      return this;
    }

    @Nonnull
    @Override
    public Builder<T> withShowLibraryContents(boolean value) {
      myShowLibraryContents = value;
      return this;
    }

    @Nonnull
    @Override
    public Builder<T> withClassProvider(@Nonnull ClassProvider<T> classProvider) {
      myClassProvider = classProvider;
      return this;
    }

    @Nonnull
    @Override
    public Builder<T> withInheritorsProvider(@Nonnull InheritorsProviderFactory<T> inheritorsProvider) {
      myInheritorsProviderSupplier = inheritorsProvider;
      return this;
    }

    @Nonnull
    @Override
    public Builder<T> withTreeElementConverter(@Nonnull Function<Object, T> userObjectElementConverter) {
      myUserDataElementConverter = userObjectElementConverter;
      return this;
    }

    @Nonnull
    @Override
    public TreeChooser<T> build() {
      return new AbstractTreeClassChooserDialog<>(myTitle.get(), myProject, myProjectAwareSearchScope, myElementClass, myClassFilter, myBaseClass, myInitialClass, myShowMembers, myShowLibraryContents) {
        @Override
        protected T getSelectedFromTreeUserObject(DefaultMutableTreeNode node) {
          Object userObject = node.getUserObject();
          if (userObject == null) {
            return null;
          }
          return myUserDataElementConverter.apply(userObject);
        }

        @Nonnull
        @Override
        protected Collection<T> getClassesByName(String name, boolean checkBoxState, String pattern, ProjectAwareSearchScope searchScope) {
          return myClassProvider.getClassesByName(getProject(), name, checkBoxState, pattern, searchScope);
        }

        @Nullable
        @Override
        protected TreeClassInheritorsProvider<T> getInheritorsProvider(@Nonnull T baseClass) {
          return myInheritorsProviderSupplier.create(baseClass, getScope());
        }
      };
    }
  }

  private final Project myProject;

  @Inject
  public TreeClassChooserFactoryImpl(Project project) {
    myProject = project;
  }

  @Nonnull
  @Override
  public <T extends PsiNamedElement> Builder<T> newChooser(@Nonnull Class<T> elementClazz) {
    return new BuilderImpl<>(myProject, elementClazz);
  }
}
