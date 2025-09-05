/*
 * Copyright 2013-2025 consulo.io
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
package consulo.diff.impl.internal;

import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorHighlighter;
import consulo.codeEditor.HighlighterColors;
import consulo.colorScheme.EditorColorsManager;
import consulo.diff.content.DocumentContent;
import consulo.language.Language;
import consulo.language.codeStyle.CodeStyle;
import consulo.language.editor.highlight.EditorHighlighterFactory;
import consulo.language.editor.highlight.EmptyEditorHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighter;
import consulo.language.editor.highlight.SyntaxHighlighterFactory;
import consulo.language.file.light.LightVirtualFile;
import consulo.language.plain.PlainTextFileType;
import consulo.project.Project;
import consulo.virtualFileSystem.VirtualFile;
import consulo.virtualFileSystem.fileType.FileType;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * @author VISTALL
 * @since 2025-09-04
 */
public class DiffLanguageUtil {
    public static void configureEditor(@Nonnull EditorEx editor, @Nonnull DocumentContent content, @Nullable Project project) {
        setEditorHighlighter(project, editor, content);
        setEditorCodeStyle(project, editor, content.getContentType());
        editor.reinitSettings();
    }

    @Nullable
    public static EditorHighlighter initEditorHighlighter(
        @Nullable Project project,
        @Nonnull DocumentContent content,
        @Nonnull CharSequence text
    ) {
        EditorHighlighter highlighter = createEditorHighlighter(project, content);
        if (highlighter == null) {
            return null;
        }
        highlighter.setText(text);
        return highlighter;
    }

    @Nonnull
    public static EditorHighlighter initEmptyEditorHighlighter(@Nonnull CharSequence text) {
        EditorHighlighter highlighter = createEmptyEditorHighlighter();
        highlighter.setText(text);
        return highlighter;
    }

    @Nullable
    private static EditorHighlighter createEditorHighlighter(@Nullable Project project, @Nonnull DocumentContent content) {
        FileType type = content.getContentType();
        VirtualFile file = content.getHighlightFile();
        Language language = content.getUserData(Language.KEY);

        EditorHighlighterFactory highlighterFactory = EditorHighlighterFactory.getInstance();
        if (language != null) {
            SyntaxHighlighter syntaxHighlighter = SyntaxHighlighterFactory.getSyntaxHighlighter(language, project, file);
            return highlighterFactory.createEditorHighlighter(syntaxHighlighter, EditorColorsManager.getInstance().getGlobalScheme());
        }
        if (file != null) {
            if (type == null || type == PlainTextFileType.INSTANCE || file.getFileType() == type || file instanceof LightVirtualFile) {
                return highlighterFactory.createEditorHighlighter(project, file);
            }
        }
        if (type != null) {
            return highlighterFactory.createEditorHighlighter(project, type);
        }
        return null;
    }

    @Nonnull
    private static EditorHighlighter createEmptyEditorHighlighter() {
        return new EmptyEditorHighlighter(EditorColorsManager.getInstance().getGlobalScheme().getAttributes(HighlighterColors.TEXT));
    }

    public static void setEditorHighlighter(@Nullable Project project, @Nonnull EditorEx editor, @Nonnull DocumentContent content) {
        EditorHighlighter highlighter = createEditorHighlighter(project, content);
        if (highlighter != null) {
            editor.setHighlighter(highlighter);
        }
    }

    public static void setEditorCodeStyle(@Nullable Project project, @Nonnull EditorEx editor, @Nullable FileType fileType) {
        if (project != null && fileType != null) {
            editor.getSettings().setTabSize(CodeStyle.getProjectOrDefaultSettings(project).getTabSize(fileType));
            editor.getSettings().setUseTabCharacter(CodeStyle.getProjectOrDefaultSettings(project).useTabCharacter(fileType));
        }
        editor.getSettings().setCaretRowShown(false);
        editor.reinitSettings();
    }
}
