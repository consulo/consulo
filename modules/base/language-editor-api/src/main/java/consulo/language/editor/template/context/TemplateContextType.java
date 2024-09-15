/*
 * Copyright 2013-2024 consulo.io
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
package consulo.language.editor.template.context;

import consulo.annotation.component.ComponentScope;
import consulo.annotation.component.ExtensionAPI;
import consulo.codeEditor.EditorFactory;
import consulo.component.extension.ExtensionPointName;
import consulo.document.Document;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Implement this class to describe some particular context that the user may associate with a live template, e.g., "Java String Start".
 * Contexts are available for the user in the Live Template management UI.
 *
 * @author VISTALL
 * @see BaseTemplateContextType
 * @since 2024-09-15
 */
@ExtensionAPI(ComponentScope.APPLICATION)
public interface TemplateContextType {
    public static final ExtensionPointName<TemplateContextType> EP_NAME = ExtensionPointName.create(TemplateContextType.class);

    @Nonnull
    LocalizeValue getPresentableName();

    @Nonnull
    String getContextId();

    default boolean isInContext(@Nonnull TemplateActionContext templateActionContext) {
        return isInContext(templateActionContext.getFile(), templateActionContext.getStartOffset());
    }

    default boolean isInContext(@Nonnull PsiFile file, int offset) {
        throw new RuntimeException("Please, implement isInContext(TemplateActionContext) method and don't invoke this method directly");
    }

    /**
     * @return whether an abbreviation of this context's template can be entered in editor
     * and expanded from there by Insert Live Template action
     */
    default boolean isExpandableFromEditor() {
        return true;
    }

    /**
     * @return syntax highlighter that going to be used in live template editor for template with context type enabled. If several context
     * types are enabled - first registered wins.
     */
    @Nullable
    default SyntaxHighlighter createHighlighter() {
        return null;
    }

    /**
     * @return parent context type. Parent context serves two purposes:
     * <ol>
     * <li>Context types hierarchy shown as a tree in template editor</li>
     * <li>When template applicability is computed, IDE finds all deepest applicable context types for the current {@link TemplateActionContext}
     * and excludes checking of all of their parent contexts. Then, IDE checks that at least one of these deepest applicable contexts is
     * enabled for the template.</li>
     * </ol>
     */
    @Nullable
    default TemplateContextType getBaseContextType() {
        return null;
    }

    /**
     * @return document for live template editor. Used for live templates with this context type enabled. If several context types are enabled -
     * first registered wins.
     */
    default Document createDocument(CharSequence text, Project project) {
        return EditorFactory.getInstance().createDocument(text);
    }
}
