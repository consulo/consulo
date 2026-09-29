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

import consulo.codeEditor.CaretModel;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorEx;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.EditorSettings;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.document.Document;
import consulo.document.event.DocumentEvent;
import consulo.document.event.DocumentListener;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.highlight.EditorHighlighterFactory;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.project.ProjectManager;
import consulo.project.event.ProjectManagerListener;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.CommandProcessor;
import consulo.virtualFileSystem.fileType.FileType;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import java.util.List;
import java.util.function.Consumer;

public final class EditorBoxSupport {
    private final EditorFactory myEditorFactory;
    private final ProjectManager myProjectManager;
    private final @Nullable Project myProject;
    private final boolean myOneLine;
    private final boolean myViewer;
    private final List<Consumer<EditorEx>> myCustomizations;
    private final Runnable myValueChanged;
    private final Consumer<EditorEx> myEditorReleasing;

    private final DocumentListener myDocumentListener = new DocumentListener() {
        @Override
        public void documentChanged(DocumentEvent event) {
            myValueChanged.run();
        }
    };

    private Document myDocument;
    private FileType myFileType;
    private LocalizeValue myPlaceholder;
    private boolean myEnabled = true;
    private boolean myWholeTextSelected;

    private @Nullable EditorEx myEditor;
    private @Nullable Disposable myEditorDisposable;

    public EditorBoxSupport(
        EditorBoxOptions options,
        EditorFactory editorFactory,
        ProjectManager projectManager,
        Runnable valueChanged,
        Consumer<EditorEx> editorReleasing
    ) {
        myEditorFactory = editorFactory;
        myProjectManager = projectManager;
        myProject = options.project();
        myOneLine = options.oneLine();
        myViewer = options.viewer();
        myCustomizations = options.customizations();
        myValueChanged = valueChanged;
        myEditorReleasing = editorReleasing;

        myDocument = options.document();
        myFileType = options.fileType();
        myPlaceholder = options.placeholder();

        myDocument.addDocumentListener(myDocumentListener);
    }

    public Document getDocument() {
        return myDocument;
    }

    public @Nullable EditorEx getEditor() {
        return myEditor;
    }

    public boolean isOneLine() {
        return myOneLine;
    }

    public boolean isEnabled() {
        return myEnabled;
    }

    public void setEnabled(boolean enabled) {
        myEnabled = enabled;
    }

    public String getText() {
        return myDocument.getText();
    }

    @RequiredUIAccess
    public void setText(@Nullable String text) {
        replaceText(myProject, myDocument, myEditor, text);
    }

    public void setDocument(Document document, FileType fileType) {
        myDocument.removeDocumentListener(myDocumentListener);

        myDocument = document;
        myFileType = fileType;

        myDocument.addDocumentListener(myDocumentListener);
    }

    public void setPlaceholder(LocalizeValue placeholder) {
        myPlaceholder = placeholder;

        EditorEx editor = myEditor;
        if (editor != null) {
            editor.setPlaceholder(placeholder.getNullIfEmpty());
        }
    }

    public void selectAll() {
        EditorEx editor = myEditor;
        if (editor != null) {
            selectAll(editor);
        }
        else {
            myWholeTextSelected = true;
        }
    }

    @RequiredUIAccess
    public EditorEx createEditor() {
        releaseEditor();

        boolean viewer = myViewer || !myEnabled;

        EditorEx editor = (EditorEx) (viewer
            ? myEditorFactory.createViewer(myDocument, myProject)
            : myEditorFactory.createEditor(myDocument, myProject));

        configureEditor(editor, myProject, myFileType, viewer, myOneLine);

        editor.setPlaceholder(myPlaceholder.getNullIfEmpty());

        if (viewer) {
            editor.getSelectionModel().removeSelection();
        }
        else if (myWholeTextSelected) {
            selectAll(editor);
            myWholeTextSelected = false;
        }

        for (Consumer<EditorEx> customization : myCustomizations) {
            customization.accept(editor);
        }

        editor.getUIComponent().putUserData(UiDataProvider.KEY, sink -> sink.set(Editor.KEY, editor));

        Disposable editorDisposable = Disposable.newDisposable("EditorBox");
        Project project = myProject;
        if (project != null) {
            ProjectManagerListener listener = new ProjectManagerListener() {
                @Override
                public void projectClosing(Project closingProject) {
                    project.getUIAccess().giveAndWaitIfNeed(() -> releaseEditor());
                }
            };
            myProjectManager.addProjectManagerListener(project, listener);
            Disposer.register(editorDisposable, () -> myProjectManager.removeProjectManagerListener(project, listener));
        }

        myEditor = editor;
        myEditorDisposable = editorDisposable;
        return editor;
    }

    @RequiredUIAccess
    public void releaseEditor() {
        EditorEx editor = myEditor;
        Disposable editorDisposable = myEditorDisposable;

        myEditor = null;
        myEditorDisposable = null;

        if (editorDisposable != null) {
            Disposer.dispose(editorDisposable);
        }

        if (editor == null) {
            return;
        }

        myEditorReleasing.accept(editor);

        if (editor.isViewer()) {
            restoreHighlighting(myProject, editor);
        }

        if (!editor.isDisposed()) {
            myEditorFactory.releaseEditor(editor);
        }
    }

    public static void configureEditor(EditorEx editor, @Nullable Project project, @Nullable FileType fileType, boolean viewer, boolean oneLine) {
        EditorSettings settings = editor.getSettings();
        settings.setAdditionalLinesCount(0);
        settings.setAdditionalColumnsCount(1);
        settings.setRightMarginShown(false);
        settings.setRightMargin(-1);
        settings.setFoldingOutlineShown(false);
        settings.setLineNumbersShown(false);
        settings.setLineMarkerAreaShown(false);
        settings.setIndentGuidesShown(false);
        settings.setVirtualSpace(false);
        settings.setWheelFontChangeEnabled(false);
        settings.setAdditionalPageAtBottom(false);
        settings.setLineCursorWidth(1);
        settings.setCaretRowShown(false);

        editor.setHorizontalScrollbarVisible(false);
        editor.setVerticalScrollbarVisible(false);
        editor.setCaretEnabled(!viewer);

        if (project != null) {
            PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            if (psiFile != null) {
                DaemonCodeAnalyzer.getInstance(project).setHighlightingEnabled(psiFile, !viewer);
            }

            if (fileType != null) {
                editor.setHighlighter(EditorHighlighterFactory.getInstance().createEditorHighlighter(project, fileType));
            }
        }

        editor.setOneLineMode(oneLine);
        if (editor.isScrollPaneAvailable()) {
            editor.getScrollPane().setInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT, null);
        }
        editor.getCaretModel().moveToOffset(editor.getDocument().getTextLength());
    }

    public static void restoreHighlighting(@Nullable Project project, Editor editor) {
        if (project == null || project.isDisposed()) {
            return;
        }

        PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
        if (psiFile != null) {
            DaemonCodeAnalyzer.getInstance(project).setHighlightingEnabled(psiFile, true);
        }
    }

    @RequiredUIAccess
    public static void replaceText(@Nullable Project project, Document document, @Nullable Editor editor, @Nullable String text) {
        CommandProcessor.getInstance().newCommand()
            .project(project)
            .document(document)
            .inWriteAction()
            .run(() -> {
                document.replaceString(0, document.getTextLength(), text == null ? "" : text);
                if (editor != null) {
                    CaretModel caretModel = editor.getCaretModel();
                    if (caretModel.getOffset() >= document.getTextLength()) {
                        caretModel.moveToOffset(document.getTextLength());
                    }
                }
            });
    }

    public static void selectAll(Editor editor) {
        editor.getCaretModel().removeSecondaryCarets();
        editor.getCaretModel().getPrimaryCaret().setSelection(0, editor.getDocument().getTextLength(), false);
    }
}
