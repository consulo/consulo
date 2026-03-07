// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.comment;

import consulo.application.ReadAction;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.document.Document;
import consulo.document.event.BulkAwareDocumentListener;
import consulo.document.event.DocumentEvent;
import consulo.document.util.TextRange;
import consulo.language.editor.FileHighlightingSetting;
import consulo.language.editor.highlight.HighlightLevelUtil;
import consulo.language.editor.ui.awt.EditorTextField;
import consulo.language.inject.InjectedLanguageManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileFactory;
import consulo.project.Project;
import consulo.util.dataholder.Key;
import consulo.util.lang.LocalTimeCounter;
import consulo.virtualFileSystem.fileType.FileTypeRegistry;
import jakarta.annotation.Nonnull;

import java.awt.*;

final class CodeReviewMarkdownEditor {
    private static final Key<Boolean> INJECTION_PROCESSED = Key.create("CODEREVIEW_INJECTION_PROCESSED");

    private CodeReviewMarkdownEditor() {
    }

    static @Nonnull Editor create(@Nonnull Project project) {
        return create(project, false, false);
    }

    static @Nonnull Editor create(@Nonnull Project project, boolean inline, boolean oneLine) {
        InjectedLanguageManager injectedLanguageManager = InjectedLanguageManager.getInstance(project);
        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);

        // setup markdown only if plugin is enabled
        var fileType = FileTypeRegistry.getInstance().getFileTypeByExtension("md");
        if (fileType == FileTypes.UNKNOWN) {
            fileType = FileTypes.PLAIN_TEXT;
        }

        PsiFile psiFile = PsiFileFactory.getInstance(project)
            .createFileFromText("Dummy.md", fileType, "", LocalTimeCounter.currentTime(), true, false);

        EditorFactory editorFactory = EditorFactory.getInstance();
        Document document = ReadAction.compute(() -> psiDocumentManager.getDocument(psiFile));
        if (document == null) {
            document = editorFactory.createDocument("");
        }

        document.addDocumentListener(new BulkAwareDocumentListener.Simple() {
            @Override
            public void documentChangedNonBulk(@Nonnull DocumentEvent event) {
                for (Document doc : injectedLanguageManager.getCachedInjectedDocumentsInRange(
                    psiFile, new TextRange(event.getOffset(), event.getOffset() + event.getNewLength()))) {
                    if (Boolean.TRUE.equals(doc.getUserData(INJECTION_PROCESSED))) {
                        continue;
                    }
                    PsiFile injectedPsi = psiDocumentManager.getCachedPsiFile(doc);
                    if (injectedPsi == null) {
                        continue;
                    }
                    HighlightLevelUtil.forceRootHighlighting(injectedPsi, FileHighlightingSetting.ESSENTIAL);
                    doc.putUserData(INJECTION_PROCESSED, true);
                }
            }
        });

        EditorEx editorEx = (EditorEx) editorFactory.createEditor(document, project, fileType, false);
        EditorTextField.setupTextFieldEditor(editorEx);

        editorEx.getSettings().setCaretRowShown(false);
        editorEx.getSettings().setUseSoftWraps(true);

        editorEx.putUserData(ZoomIndicatorManager.SUPPRESS_ZOOM_INDICATOR, true);
        editorEx.putUserData(IncrementalFindAction.SEARCH_DISABLED, true);
        editorEx.getColorsScheme().setLineSpacing(1f);
        editorEx.setEmbeddedIntoDialogWrapper(true);
        editorEx.setOneLineMode(oneLine);

        editorEx.getComponent().addPropertyChangeListener("font", evt -> setEditorFontFromComponent(editorEx));
        setEditorFontFromComponent(editorEx);

        editorEx.setBorder(null);
        if (!inline) {
            editorEx.getComponent().setBorder(new EditorFocusBorder());
        }
        else if (!oneLine) {
            editorEx.setVerticalScrollbarVisible(true);
        }
        editorEx.getContentComponent().setFocusCycleRoot(false);

        return editorEx;
    }

    private static void setEditorFontFromComponent(@Nonnull EditorEx editor) {
        Font font = editor.getComponent().getFont();
        editor.getColorsScheme().setEditorFontName(font.getName());
        editor.getColorsScheme().setEditorFontSize(font.getSize());
    }
}

