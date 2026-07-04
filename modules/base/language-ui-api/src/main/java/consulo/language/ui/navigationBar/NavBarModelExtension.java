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
package consulo.language.ui.navigationBar;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataProvider;
import consulo.dataContext.DataSink;
import consulo.dataContext.DataSnapshot;
import consulo.language.psi.PsiElement;
import consulo.project.Project;
import consulo.ui.image.Image;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

/**
 * The interface has a default implementation ({@link DefaultNavBarExtension}) which is normally registered as last.
 * That means that custom implementations are called before the default one - with the exception of {@link #adjustElement(PsiElement)}
 * method, for which the order is reverse.
 *
 * @author anna
 * @since 2008-02-04
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface NavBarModelExtension {
    Key<Boolean> IGNORE_IN_NAVBAR = Key.create("IGNORE_IN_NAVBAR");

    default @Nullable Image getIcon(Object object) {
        return null;
    }

    @Nullable
    String getPresentableText(Object object);

    default @Nullable String getPresentableText(Object object, boolean forPopup) {
        return getPresentableText(object);
    }

    default @Nullable PsiElement getParent(PsiElement psiElement) {
        return null;
    }

    default @Nullable PsiElement adjustElement(PsiElement psiElement) {
        return psiElement;
    }

    default Collection<VirtualFile> additionalRoots(Project project) {
        return List.of();
    }

    default @Nullable Object getData(Key<?> dataId, DataProvider provider) {
        return null;
    }

    default void uiDataSnapshot(DataSink sink, DataSnapshot snapshot) {
    }

    default @Nullable String getPopupMenuGroup(DataProvider provider) {
        return null;
    }

    default PsiElement getLeafElement(DataContext dataContext) {
        return null;
    }

    default boolean processChildren(Object object, Object rootElement, Predicate<Object> processor) {
        return true;
    }

    default boolean normalizeChildren() {
        return true;
    }

    default @Nullable Boolean shouldExpandOnClick(PsiElement psiElement) {
        return null;
    }
}
