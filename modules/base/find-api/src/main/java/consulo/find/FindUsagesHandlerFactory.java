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
package consulo.find;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiElement;
import consulo.ui.annotation.RequiredUIAccess;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author yole
 */
@ExtensionAPI(ComponentScope.PROJECT)
public abstract class FindUsagesHandlerFactory {
    public static final ExtensionPointName<FindUsagesHandlerFactory> EP_NAME = ExtensionPointName.create(FindUsagesHandlerFactory.class);

    public abstract boolean canFindUsages(PsiElement element);

    public abstract @Nullable FindUsagesHandler createFindUsagesHandler(PsiElement element, boolean forHighlightUsages);

    /**
     * Creates the handler for an interactive search. Unlike {@link #createFindUsagesHandler(PsiElement, boolean)}, it may
     * ask the user first, for example whether to search for the usages of a base method instead.
     *
     * @return the handler, {@code null} when this factory does not handle the element, or
     * {@link FindUsagesHandler#NULL_HANDLER} when the user cancelled the search
     */
    @RequiredUIAccess
    public CompletableFuture<@Nullable FindUsagesHandler> createFindUsagesHandlerAsync(PsiElement element, boolean forHighlightUsages) {
        return CompletableFuture.completedFuture(createFindUsagesHandler(element, forHighlightUsages));
    }
}
