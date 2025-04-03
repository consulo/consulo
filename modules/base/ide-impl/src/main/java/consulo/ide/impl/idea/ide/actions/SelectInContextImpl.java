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

package consulo.ide.impl.idea.ide.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.dataContext.DataContext;
import consulo.document.Document;
import consulo.document.FileDocumentManager;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.FileEditorManager;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.impl.internal.OpenFileDescriptorImpl;
import consulo.fileEditor.internal.FileEditorManagerEx;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.language.template.TemplateLanguageFileViewProvider;
import consulo.navigation.Navigatable;
import consulo.project.Project;
import consulo.project.ui.view.SelectInContext;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.util.collection.ArrayUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.event.InputEvent;
import java.util.function.Supplier;

public abstract class SelectInContextImpl implements SelectInContext {
    protected final PsiFile myPsiFile;

    protected SelectInContextImpl(PsiFile psiFile) {
        myPsiFile = psiFile;
    }

    @Override
    @Nonnull
    public Project getProject() {
        return myPsiFile.getProject();
    }


    @Override
    @Nonnull
    public VirtualFile getVirtualFile() {
        VirtualFile vFile = myPsiFile.getVirtualFile();
        assert vFile != null;
        return vFile;
    }

    @Override
    public Object getSelectorInFile() {
        return myPsiFile;
    }

    @Nullable
    @RequiredReadAction
    public static SelectInContext createContext(AnActionEvent event) {
        DataContext dataContext = event.getDataContext();

        SelectInContext result = createEditorContext(dataContext);
        if (result != null) {
            return result;
        }

        JComponent sourceComponent = getEventComponent(event);
        if (sourceComponent == null) {
            return null;
        }

        SelectInContext selectInContext = dataContext.getData(SelectInContext.DATA_KEY);
        if (selectInContext == null) {
            selectInContext = createPsiContext(event);
        }

        if (selectInContext == null) {
            if (dataContext.getData(Navigatable.KEY) instanceof OpenFileDescriptorImpl openFileDescriptor) {
                VirtualFile file = openFileDescriptor.getFile();
                if (file.isValid()) {
                    Project project = dataContext.getData(Project.KEY);
                    selectInContext = OpenFileDescriptorContext.create(project, file);
                }
            }
        }

        if (selectInContext == null) {
            VirtualFile virtualFile = dataContext.getData(VirtualFile.KEY);
            Project project = dataContext.getData(Project.KEY);
            if (virtualFile != null && project != null) {
                return new VirtualFileSelectInContext(project, virtualFile);
            }
        }

        return selectInContext;
    }

    @Nullable
    @RequiredReadAction
    private static SelectInContext createEditorContext(DataContext dataContext) {
        Project project = dataContext.getData(Project.KEY);
        FileEditor editor = dataContext.getData(FileEditor.KEY);
        return createEditorContext(project, editor);
    }

    @RequiredReadAction
    public static SelectInContext createEditorContext(Project project, FileEditor editor) {
        if (project == null || editor == null) {
            return null;
        }
        VirtualFile file = FileEditorManagerEx.getInstanceEx(project).getFile(editor);
        if (file == null) {
            return null;
        }
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile == null) {
            return null;
        }
        if (editor instanceof TextEditor textEditor) {
            return new TextEditorContext(textEditor, psiFile);
        }
        else {
            return new SimpleSelectInContext(psiFile);
        }
    }

    @Nullable
    private static SelectInContext createPsiContext(AnActionEvent event) {
        DataContext dataContext = event.getDataContext();
        PsiElement psiElement = dataContext.getData(PsiElement.KEY);
        if (psiElement == null || !psiElement.isValid()) {
            return null;
        }
        PsiFile psiFile = psiElement.getContainingFile();
        if (psiFile == null) {
            return null;
        }
        return new SimpleSelectInContext(psiFile, psiElement);
    }

    @Nullable
    private static JComponent getEventComponent(AnActionEvent event) {
        InputEvent inputEvent = event.getInputEvent();
        if (inputEvent != null && inputEvent.getSource() instanceof JComponent jComponent) {
            return jComponent;
        }
        else {
            return safeCast(event.getDataContext().getData(UIExAWTDataKey.CONTEXT_COMPONENT), JComponent.class);
        }
    }

    @Nullable
    @SuppressWarnings({"unchecked"})
    private static <T> T safeCast(Object obj, Class<T> expectedClass) {
        if (expectedClass.isInstance(obj)) {
            return (T)obj;
        }
        return null;
    }

    private static class TextEditorContext extends SelectInContextImpl {
        private final TextEditor myEditor;

        public TextEditorContext(TextEditor editor, PsiFile psiFile) {
            super(psiFile);
            myEditor = editor;
        }

        @Override
        public Supplier<FileEditor> getFileEditorProvider() {
            return () -> myEditor;
        }

        @Override
        @RequiredReadAction
        public Object getSelectorInFile() {
            if (myPsiFile.getViewProvider() instanceof TemplateLanguageFileViewProvider) {
                return super.getSelectorInFile();
            }
            int offset = myEditor.getEditor().getCaretModel().getOffset();

            if (offset >= 0 && offset < myPsiFile.getTextLength()) {
                return myPsiFile.findElementAt(offset);
            }
            return super.getSelectorInFile();
        }
    }


    private static class OpenFileDescriptorContext extends SelectInContextImpl {
        public OpenFileDescriptorContext(PsiFile psiFile) {
            super(psiFile);
        }

        @Override
        public Supplier<FileEditor> getFileEditorProvider() {
            return () -> FileEditorManager.getInstance(getProject()).openFile(getVirtualFile(), false)[0];
        }

        @Nullable
        public static SelectInContext create(Project project, VirtualFile file) {
            Document document = FileDocumentManager.getInstance().getDocument(file);
            if (document == null) {
                return null;
            }
            PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document);
            if (psiFile == null) {
                return null;
            }
            return new OpenFileDescriptorContext(psiFile);
        }
    }

    private static class SimpleSelectInContext extends SelectInContextImpl {
        private final PsiElement myElementToSelect;

        public SimpleSelectInContext(PsiFile psiFile) {
            this(psiFile, psiFile);
        }

        @Override
        public Supplier<FileEditor> getFileEditorProvider() {
            return () -> {
                VirtualFile file = myElementToSelect.getContainingFile().getVirtualFile();
                if (file == null) {
                    return null;
                }
                return ArrayUtil.getFirstElement(FileEditorManager.getInstance(getProject()).openFile(file, false));
            };
        }

        public SimpleSelectInContext(PsiFile psiFile, PsiElement elementToSelect) {
            super(psiFile);
            myElementToSelect = elementToSelect;
        }
    }

    private static class VirtualFileSelectInContext implements SelectInContext {
        private final Project myProject;
        private final VirtualFile myVirtualFile;

        public VirtualFileSelectInContext(Project project, VirtualFile virtualFile) {
            myProject = project;
            myVirtualFile = virtualFile;
        }

        @Override
        @Nonnull
        public Project getProject() {
            return myProject;
        }

        @Override
        @Nonnull
        public VirtualFile getVirtualFile() {
            return myVirtualFile;
        }

        @Override
        @Nullable
        public Object getSelectorInFile() {
            return myVirtualFile;
        }
    }
}

