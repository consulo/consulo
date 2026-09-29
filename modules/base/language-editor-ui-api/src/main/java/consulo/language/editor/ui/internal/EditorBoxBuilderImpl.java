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
package consulo.language.editor.ui.internal;

import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.document.Document;
import consulo.language.Language;
import consulo.language.editor.ui.EditorBox;
import consulo.language.editor.ui.EditorBoxBuilder;
import consulo.language.editor.ui.awt.TextCompletionProvider;
import consulo.language.editor.ui.awt.TextCompletionUtil;
import consulo.language.editor.ui.awt.TextFieldCompletionProvider;
import consulo.language.editor.ui.awt.TextFieldWithAutoCompletionContributor;
import consulo.language.editor.ui.awt.TextFieldWithAutoCompletionListProvider;
import consulo.language.plain.PlainTextFileType;
import consulo.language.plain.PlainTextLanguage;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class EditorBoxBuilderImpl implements EditorBoxBuilder {
    private final @Nullable Project myProject;
    private final EditorFactory myEditorFactory;
    private final Function<EditorBoxOptions, EditorBox> myBoxFactory;

    private String myText = "";
    private @Nullable Document myDocument;
    private @Nullable FileType myFileType;
    private @Nullable Language myLanguage;
    private final List<Consumer<PsiFile>> myCompletionInstallers = new ArrayList<>();
    private final List<Consumer<PsiFile>> myPsiFileCustomizations = new ArrayList<>();
    private boolean myEmbeddedIntoDialog;
    private boolean myMultiline;
    private boolean myViewer;
    private boolean myEditorFont;
    private LocalizeValue myPlaceholder = LocalizeValue.empty();
    private final List<Consumer<EditorEx>> myCustomizations = new ArrayList<>();

    public EditorBoxBuilderImpl(@Nullable Project project, EditorFactory editorFactory, Function<EditorBoxOptions, EditorBox> boxFactory) {
        myProject = project;
        myEditorFactory = editorFactory;
        myBoxFactory = boxFactory;
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
        myEmbeddedIntoDialog = true;
        return this;
    }

    @Override
    public EditorBoxBuilder completion(TextFieldCompletionProvider completionProvider) {
        myCompletionInstallers.add(completionProvider::installTo);
        return this;
    }

    @Override
    public EditorBoxBuilder completion(TextCompletionProvider completionProvider) {
        myCompletionInstallers.add(psiFile -> TextCompletionUtil.installProvider(psiFile, completionProvider, true));
        myCustomizations.add(editor -> TextCompletionUtil.customizeEditor(editor, false, false));
        myEmbeddedIntoDialog = true;
        return this;
    }

    @Override
    public EditorBoxBuilder completion(TextFieldWithAutoCompletionListProvider<?> completionProvider) {
        myCompletionInstallers.add(psiFile -> TextFieldWithAutoCompletionContributor.installCompletion(psiFile, completionProvider, true));
        myEmbeddedIntoDialog = true;
        return this;
    }

    @Override
    public EditorBoxBuilder customizePsiFile(Consumer<PsiFile> customization) {
        myPsiFileCustomizations.add(customization);
        return this;
    }

    @Override
    public EditorBoxBuilder multiline() {
        myMultiline = true;
        return this;
    }

    @Override
    public EditorBoxBuilder viewer() {
        myViewer = true;
        return this;
    }

    @Override
    public EditorBoxBuilder editorFont() {
        myEditorFont = true;
        return this;
    }

    @Override
    public EditorBoxBuilder placeholder(LocalizeValue placeholder) {
        myPlaceholder = placeholder;
        return this;
    }

    @Override
    public EditorBoxBuilder customize(Consumer<EditorEx> customization) {
        myCustomizations.add(customization);
        return this;
    }

    @RequiredUIAccess
    @Override
    public EditorBox build() {
        List<Consumer<EditorEx>> customizations = new ArrayList<>(myCustomizations);
        if (myEmbeddedIntoDialog) {
            customizations.add(editor -> editor.setEmbeddedIntoDialogWrapper(true));
        }

        EditorBoxOptions options = new EditorBoxOptions(
            myProject,
            createDocument(),
            getFileType(),
            !myMultiline,
            myViewer,
            myEditorFont,
            myPlaceholder,
            List.copyOf(customizations)
        );
        return myBoxFactory.apply(options);
    }

    private Document createDocument() {
        if (myDocument != null) {
            return myDocument;
        }

        Project project = myProject;
        if (project != null && (myLanguage != null || !myCompletionInstallers.isEmpty() || !myPsiFileCustomizations.isEmpty())) {
            Language psiLanguage = myLanguage == null || !myCompletionInstallers.isEmpty() ? PlainTextLanguage.INSTANCE : myLanguage;
            FileType fileType = psiLanguage.getAssociatedFileType();
            if (fileType != null) {
                return TextFieldDocuments.create(project, fileType, myText, psiFile -> {
                    myCompletionInstallers.forEach(installer -> installer.accept(psiFile));
                    myPsiFileCustomizations.forEach(customization -> customization.accept(psiFile));
                });
            }
        }

        return myEditorFactory.createDocument(myText);
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
