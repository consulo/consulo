/*
 * Copyright 2013-2026 consulo.io
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
package consulo.language.editor.impl.internal.documentation;

import consulo.language.psi.PsiElement;
import consulo.language.psi.SmartPsiElementPointer;
import org.jspecify.annotations.Nullable;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
public record DocumentationElementSource(
    SmartPsiElementPointer<? extends PsiElement> element,
    @Nullable SmartPsiElementPointer<? extends PsiElement> originalElement,
    @Nullable String anchor,
    @Nullable String documentation
) implements DocumentationSource {
    public DocumentationElementSource(
        SmartPsiElementPointer<? extends PsiElement> element,
        @Nullable SmartPsiElementPointer<? extends PsiElement> originalElement
    ) {
        this(element, originalElement, null, null);
    }
}
