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
package consulo.language.editor.codeBlock;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ServiceAPI;
import consulo.colorScheme.TextAttributes;
import consulo.colorScheme.TextAttributesKey;
import consulo.language.Language;
import consulo.project.Project;
import org.jspecify.annotations.Nullable;

/**
 * Turns a block of code into html colored as the editor colors it - for a code block of a document preview, a documentation or
 * anything else which shows code as html. The html has inline styles only, so any html view shows it.
 */
@ServiceAPI(ComponentScope.APPLICATION)
public interface CodeBlockHighlighter {
    /**
     * Finds the language a code block names: by a mime type, an id or a display name of a language, or an extension of a file of
     * it, ignoring case - {@code java}, {@code text/x-java}, {@code JavaScript} or {@code js}
     */
    @Nullable Language guessLanguage(@Nullable String languageName);

    /**
     * @param code the code without any escaping
     */
    CodeBlockHtmlBuilder newHtmlBuilder(Project project, Language language, String code);

    /**
     * Appends {@code value} in a span styled by the attributes of {@code attributesKey} in the global color scheme
     *
     * @param value            html, which is appended as is
     * @param saturationFactor see {@link CodeBlockHtmlBuilder#saturationFactor(float)}
     */
    StringBuilder appendStyledSpan(StringBuilder buffer, TextAttributesKey attributesKey, @Nullable String value, float saturationFactor);

    /**
     * Appends {@code value} in a span styled by {@code attributes}
     *
     * @param value            html, which is appended as is
     * @param saturationFactor see {@link CodeBlockHtmlBuilder#saturationFactor(float)}
     */
    StringBuilder appendStyledSpan(StringBuilder buffer, TextAttributes attributes, @Nullable String value, float saturationFactor);
}
