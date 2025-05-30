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
package consulo.language.editor.rawHighlight;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.component.extension.ExtensionPointName;
import consulo.language.psi.PsiFile;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

@ExtensionAPI(ComponentScope.APPLICATION)
public interface HighlightInfoFilter {
    ExtensionPointName<HighlightInfoFilter> EXTENSION_POINT_NAME = ExtensionPointName.create(HighlightInfoFilter.class);

    /**
     * @param file - might (and will be) null. Return true in this case if you'd like to switch this kind of highlighting in ANY file
     */
    boolean accept(@Nonnull HighlightInfo highlightInfo, @Nullable PsiFile file);
}
