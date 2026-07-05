/*
 * Copyright 2000-2009 JetBrains s.r.o.
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
package consulo.language.editor.impl.action;

import consulo.codeEditor.Editor;
import consulo.annotation.access.RequiredReadAction;
import consulo.language.psi.PsiElement;
import consulo.language.inject.InjectedLanguageManager;
import consulo.document.DocumentWindow;
import consulo.document.Document;
import consulo.codeEditor.imaginary.ImaginaryEditor;
import consulo.language.editor.inject.ImaginaryEditorWindow;
import consulo.dataContext.DataContext;
import consulo.language.editor.action.CodeInsightAction;
import consulo.language.editor.completion.lookup.Lookup;
import consulo.language.editor.completion.lookup.LookupManager;
import consulo.language.editor.inject.InjectedEditorManager;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.ui.image.Image;
import org.jspecify.annotations.Nullable;

public abstract class BaseCodeInsightAction extends CodeInsightAction {
    private final boolean myLookForInjectedEditor;

    protected BaseCodeInsightAction() {
        this(true);
    }

    protected BaseCodeInsightAction(boolean lookForInjectedEditor) {
        myLookForInjectedEditor = lookForInjectedEditor;
    }

    protected BaseCodeInsightAction(LocalizeValue text, LocalizeValue description) {
        this(text, description, true);
    }

    protected BaseCodeInsightAction(LocalizeValue text, LocalizeValue description, boolean lookForInjectedEditor) {
        super(text, description);
        myLookForInjectedEditor = lookForInjectedEditor;
    }

    protected BaseCodeInsightAction(LocalizeValue text, LocalizeValue description, Image icon) {
        this(text, description, icon, true);
    }

    protected BaseCodeInsightAction(
        LocalizeValue text,
        LocalizeValue description,
        Image icon,
        boolean lookForInjectedEditor
    ) {
        super(text, description, icon);
        myLookForInjectedEditor = lookForInjectedEditor;
    }

    @Override
    protected @Nullable Editor getEditor(DataContext dataContext, Project project, boolean forUpdate) {
        Editor editor = getBaseEditor(dataContext, project, forUpdate);
        if (!myLookForInjectedEditor) {
            return editor;
        }
        if (editor == null) {
            return null;
        }
        // read the caret offset once here (plain captured data on the snapshot editor)
        // and pass it down as a parameter - no editor calls below this point
        int caretOffset = editor.getCaretModel().getOffset();
        return getInjectedEditor(project, editor, !forUpdate, caretOffset);
    }

    @RequiredUIAccess
    public static Editor getInjectedEditor(Project project, Editor editor) {
        return getInjectedEditor(project, editor, true);
    }

    public static Editor getInjectedEditor(Project project, Editor editor, boolean commit) {
        if (editor == null) {
            return null;
        }
        // perform path/UI thread: the live caret is the source of the offset
        return getInjectedEditor(project, editor, commit, editor.getCaretModel().getOffset());
    }

    @RequiredReadAction
    public static Editor getInjectedEditor(Project project, Editor editor, boolean commit, int caretOffset) {
        Editor injectedEditor = editor;
        if (editor != null) {
            PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
            PsiFile psiFile = documentManager.getCachedPsiFile(editor.getDocument());
            if (psiFile != null) {
                if (commit) {
                    documentManager.commitAllDocuments();
                }
                if (editor instanceof ImaginaryEditor imaginaryEditor) {
                    return getInjectedImaginaryEditor(project, imaginaryEditor, psiFile, caretOffset);
                }
                injectedEditor = InjectedEditorManager.getInstance(project).getEditorForInjectedLanguageNoCommit(editor, psiFile, caretOffset);
            }
        }
        return injectedEditor;
    }

    /**
     * The real {@code EditorWindow} machinery requires a real host editor, so for the imaginary
     * snapshot (background update path) an {@link ImaginaryEditorWindow} is built instead -
     * same injected file, injected document window and injected-space caret offset, no checks.
     */
    private static Editor getInjectedImaginaryEditor(Project project, ImaginaryEditor hostEditor, PsiFile hostFile, int hostOffset) {
        PsiElement injectedElement = InjectedLanguageManager.getInstance(project).findInjectedElementAt(hostFile, hostOffset);
        PsiFile injectedFile = injectedElement != null ? injectedElement.getContainingFile() : null;
        if (injectedFile == null) {
            return hostEditor;
        }
        Document injectedDocument = PsiDocumentManager.getInstance(project).getDocument(injectedFile);
        if (!(injectedDocument instanceof DocumentWindow documentWindow) || !documentWindow.isValid()) {
            return hostEditor;
        }
        ImaginaryEditorWindow injectedEditor = new ImaginaryEditorWindow(project, hostEditor, injectedFile, documentWindow);
        injectedEditor.getCaretModel().moveToOffset(documentWindow.hostToInjected(hostOffset));
        return injectedEditor;
    }

    protected @Nullable Editor getBaseEditor(DataContext dataContext, Project project, boolean forUpdate) {
        return super.getEditor(dataContext, project, forUpdate);
    }

    @Override
    public void update(AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        Project project = event.getData(Project.KEY);
        if (project == null) {
            presentation.setEnabled(false);
            return;
        }

        Lookup activeLookup = LookupManager.getInstance(project).getActiveLookup();
        if (activeLookup != null) {
            presentation.setEnabled(isValidForLookup());
        }
        else {
            super.update(event);
        }
    }

    protected boolean isValidForLookup() {
        return false;
    }
}
