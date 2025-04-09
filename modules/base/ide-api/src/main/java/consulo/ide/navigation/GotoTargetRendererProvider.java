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

package consulo.ide.navigation;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.editor.ui.PsiElementListCellRenderer;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author yole
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface GotoTargetRendererProvider {
    ExtensionPointName<GotoTargetRendererProvider> EP_NAME = ExtensionPointName.create(GotoTargetRendererProvider.class);

    interface Options {
        boolean hasDifferentNames();
    }

    @Nullable
    default PsiElementListCellRenderer getRenderer(PsiElement element, @Nonnull Options options) {
        return getRenderer(element);
    }

    @Nullable
    default PsiElementListCellRenderer getRenderer(PsiElement element) {
        throw new AbstractMethodError();
    }
}
