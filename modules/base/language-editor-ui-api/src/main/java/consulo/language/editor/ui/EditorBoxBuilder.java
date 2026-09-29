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
package consulo.language.editor.ui;

import consulo.codeEditor.EditorEx;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.editor.ui.awt.TextCompletionProvider;
import consulo.language.editor.ui.awt.TextFieldCompletionProvider;
import consulo.language.editor.ui.awt.TextFieldWithAutoCompletionListProvider;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.fileType.FileType;

import java.util.function.Consumer;

/**
 * Builds an {@link EditorBox}, created by {@link EditorBoxBuilderFactory}.
 * <p/>
 * A {@link #document(Document)} is shown as it is. Otherwise the box makes a document of its own from
 * {@link #text(String)}: a psi file of the {@link #language(Language)}, or of plain text when a completion provider is
 * given - completion is offered on plain text only, so the language then decides the highlighting alone. Without a
 * project, or with none of a language, a completion provider and a {@link #customizePsiFile(Consumer) psi
 * customization}, the document is a plain one without psi.
 *
 * @author VISTALL
 * @since 2026-09-29
 */
public interface EditorBoxBuilder {
    EditorBoxBuilder text(String text);

    EditorBoxBuilder document(Document document);

    EditorBoxBuilder fileType(FileType fileType);

    EditorBoxBuilder language(Language language);

    EditorBoxBuilder completion(TextFieldCompletionProvider completionProvider);

    EditorBoxBuilder completion(TextCompletionProvider completionProvider);

    EditorBoxBuilder completion(TextFieldWithAutoCompletionListProvider<?> completionProvider);

    EditorBoxBuilder customizePsiFile(Consumer<PsiFile> customization);

    EditorBoxBuilder multiline();

    EditorBoxBuilder viewer();

    EditorBoxBuilder editorFont();

    EditorBoxBuilder placeholder(LocalizeValue placeholder);

    EditorBoxBuilder customize(Consumer<EditorEx> customization);

    @RequiredUIAccess
    EditorBox build();
}
