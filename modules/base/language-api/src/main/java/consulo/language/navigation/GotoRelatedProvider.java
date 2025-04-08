/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.language.navigation;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.dataContext.DataContext;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;

import java.util.Collections;
import java.util.List;

/**
 * Provides items for Go To -> Related File action.
 * <p>
 * If related items are represented as icons on the gutter use {@link consulo.language.editor.gutter.RelatedItemLineMarkerProvider}
 * to provide both line markers and 'goto related' targets
 *
 * @author Dmitry Avdeev
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public abstract class GotoRelatedProvider {
    @Nonnull
    public List<? extends GotoRelatedItem> getItems(@Nonnull PsiElement psiElement) {
        return Collections.emptyList();
    }

    @Nonnull
    public List<? extends GotoRelatedItem> getItems(@Nonnull DataContext context) {
        return Collections.emptyList();
    }
}
