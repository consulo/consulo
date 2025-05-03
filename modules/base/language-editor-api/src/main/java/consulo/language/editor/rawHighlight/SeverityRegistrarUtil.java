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
package consulo.language.editor.rawHighlight;

import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.colorScheme.TextAttributesScheme;
import consulo.language.psi.PsiElement;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 11/10/2022
 */
public class SeverityRegistrarUtil {
    public static TextAttributes getAttributesByType(
        @Nullable PsiElement element,
        @Nonnull HighlightInfoType type,
        @Nonnull TextAttributesScheme colorsScheme
    ) {
        SeverityRegistrar severityRegistrar = SeverityRegistrar.getSeverityRegistrar(element != null ? element.getProject() : null);
        TextAttributes textAttributes = severityRegistrar.getTextAttributesBySeverity(type.getSeverity(element));
        if (textAttributes != null) {
            return textAttributes;
        }
        TextAttributesKey key = type.getAttributesKey();
        return colorsScheme.getAttributes(key);
    }
}
