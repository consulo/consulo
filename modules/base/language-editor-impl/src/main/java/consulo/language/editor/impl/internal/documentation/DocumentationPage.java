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
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

import java.util.Map;

/**
 * @author VISTALL
 * @since 2026-10-09
 */
public record DocumentationPage(
    @Nullable SmartPsiElementPointer<? extends PsiElement> element,
    String html,
    Map<String, Image> images,
    @Nullable String anchor,
    String title,
    @Nullable String externalUrl,
    boolean externalDocumentation,
    boolean editableSource
) {
    public static DocumentationPage message(String html) {
        return new DocumentationPage(null, html, Map.of(), null, "", null, false, false);
    }
}
