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

/*
 * User: anna
 * Date: 04-Feb-2008
 */
package consulo.ide.navigationToolbar;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataProvider;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * The interface has a default implementation ({@link DefaultNavBarExtension}) which is normally registered as last.
 * That means that custom implementations are called before the default one - with the exception of {@link #adjustElement(PsiElement)}
 * method, for which the order is reverse.
 *
 * @author anna
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface NavBarModelExtension {
  ExtensionPointName<NavBarModelExtension> EP_NAME = ExtensionPointName.create(NavBarModelExtension.class);

  @Nullable
  String getPresentableText(Object object);

  @Nullable
  default String getPresentableText(Object object, boolean forPopup) {
    return getPresentableText(object);
  }

  @Nullable
  PsiElement getParent(@Nonnull PsiElement psiElement);

  @Nullable
  PsiElement adjustElement(PsiElement psiElement);

  Collection<VirtualFile> additionalRoots(Project project);

  @Nullable
  default Object getData(@Nonnull Key<?> dataId, @Nonnull DataProvider provider) {
    return null;
  }

  @Nullable
  default String getPopupMenuGroup(@Nonnull DataProvider provider) {
    return null;
  }

  default PsiElement getLeafElement(@Nonnull DataContext dataContext) {
    return null;
  }

  default boolean processChildren(Object object, Object rootElement, Predicate<Object> processor) {
    return true;
  }

  default boolean normalizeChildren() {
    return true;
  }
}
