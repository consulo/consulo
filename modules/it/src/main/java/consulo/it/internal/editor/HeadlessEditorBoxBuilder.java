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
package consulo.it.internal.editor;

import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilder;
import consulo.language.editor.ui.awt.TextCompletionProvider;
import consulo.language.editor.ui.awt.TextFieldCompletionProvider;
import consulo.language.editor.ui.awt.TextFieldWithAutoCompletionListProvider;
import consulo.language.plain.PlainTextFileType;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * @author VISTALL
 */
public class HeadlessEditorBoxBuilder implements EditorBoxBuilder {
    private final @Nullable Project myProject;
    private final EditorFactory myEditorFactory;

    private String myText = "";
    private @Nullable Document myDocument;
    private @Nullable FileType myFileType;
    private @Nullable Language myLanguage;
    private LocalizeValue myPlaceholder = LocalizeValue.empty();

    public HeadlessEditorBoxBuilder(@Nullable Project project, EditorFactory editorFactory) {
        myProject = project;
        myEditorFactory = editorFactory;
    }

    @Override
    public EditorBoxBuilder text(String text) {
        myText = text;
        return this;
    }

    @Override
    public EditorBoxBuilder document(Document document) {
        myDocument = document;
        return this;
    }

    @Override
    public EditorBoxBuilder fileType(FileType fileType) {
        myFileType = fileType;
        return this;
    }

    @Override
    public EditorBoxBuilder language(Language language) {
        myLanguage = language;
        return this;
    }

    @Override
    public EditorBoxBuilder completion(TextFieldCompletionProvider completionProvider) {
        return this;
    }

    @Override
    public EditorBoxBuilder completion(TextCompletionProvider completionProvider) {
        return this;
    }

    @Override
    public EditorBoxBuilder completion(TextFieldWithAutoCompletionListProvider<?> completionProvider) {
        return this;
    }

    @Override
    public EditorBoxBuilder customizePsiFile(Consumer<PsiFile> customization) {
        return this;
    }

    @Override
    public EditorBoxBuilder multiline() {
        return this;
    }

    @Override
    public EditorBoxBuilder viewer() {
        return this;
    }

    @Override
    public EditorBoxBuilder editorFont() {
        return this;
    }

    @Override
    public EditorBoxBuilder placeholder(LocalizeValue placeholder) {
        myPlaceholder = placeholder;
        return this;
    }

    @Override
    public EditorBoxBuilder customize(Consumer<EditorEx> customization) {
        return this;
    }

    @RequiredUIAccess
    @Override
    public EditorBox build() {
        Document document = myDocument != null ? myDocument : myEditorFactory.createDocument(myText);
        return new HeadlessEditorBox(myProject, document, getFileType(), myPlaceholder);
    }

    private FileType getFileType() {
        if (myFileType != null) {
            return myFileType;
        }

        if (myLanguage != null) {
            FileType fileType = myLanguage.getAssociatedFileType();
            if (fileType != null) {
                return fileType;
            }
        }

        return PlainTextFileType.INSTANCE;
    }
}
