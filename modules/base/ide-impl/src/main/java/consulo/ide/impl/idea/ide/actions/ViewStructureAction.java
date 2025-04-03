/*
 * Copyright 2000-2015 JetBrains s.r.o.
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
import consulo.codeEditor.Editor;
import consulo.codeEditor.internal.InternalEditorKeys;
import consulo.disposer.Disposer;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.fileEditor.FileEditor;
import consulo.fileEditor.TextEditor;
import consulo.fileEditor.structureView.StructureView;
import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.fileEditor.structureView.StructureViewModel;
import consulo.fileEditor.structureView.TreeBasedStructureViewBuilder;
import consulo.ide.impl.idea.ide.util.FileStructurePopup;
import consulo.ide.impl.idea.ide.util.treeView.smartTree.TreeStructureUtil;
import consulo.ide.impl.idea.openapi.editor.ex.util.EditorUtil;
import consulo.language.editor.structureView.StructureViewComposite;
import consulo.language.editor.structureView.StructureViewCompositeModel;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiManager;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.PlaceHolder;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.util.lang.ObjectUtil;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.Arrays;

public class ViewStructureAction extends DumbAwareAction {
    public ViewStructureAction() {
        setEnabledInModalContext(true);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }
        FileEditor fileEditor = e.getData(FileEditor.KEY);
        if (fileEditor == null) {
            return;
        }

        VirtualFile virtualFile = fileEditor.getFile();
        Editor editor = fileEditor instanceof TextEditor ? ((TextEditor)fileEditor).getEditor() : e.getData(Editor.KEY);
        if (editor != null) {
            PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
        }

        FeatureUsageTracker.getInstance().triggerFeatureUsed("navigation.popup.file.structure");

        FileStructurePopup popup = createPopup(project, fileEditor);
        if (popup == null) {
            return;
        }

        String title = virtualFile == null ? fileEditor.getName() : virtualFile.getName();
        popup.setTitle(title);
        popup.show();
    }

    @Nullable
    @RequiredReadAction
    public static FileStructurePopup createPopup(@Nonnull Project project, @Nonnull FileEditor fileEditor) {
        PsiDocumentManager.getInstance(project).commitAllDocuments();
        StructureViewBuilder builder = fileEditor.getStructureViewBuilder();
        if (builder == null) {
            return null;
        }
        StructureView structureView;
        StructureViewModel treeModel;
        if (builder instanceof TreeBasedStructureViewBuilder) {
            structureView = null;
            treeModel = ((TreeBasedStructureViewBuilder)builder).createStructureViewModel(EditorUtil.getEditorEx(fileEditor));
        }
        else {
            structureView = builder.createStructureView(fileEditor, project);
            treeModel = createStructureViewModel(project, fileEditor, structureView);
        }
        if (treeModel instanceof PlaceHolder) {
            //noinspection unchecked
            ((PlaceHolder)treeModel).setPlace(TreeStructureUtil.PLACE);
        }
        FileStructurePopup popup = new FileStructurePopup(project, fileEditor, treeModel);
        if (structureView != null) {
            Disposer.register(popup, structureView);
        }
        return popup;
    }

    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            e.getPresentation().setEnabled(false);
            return;
        }

        FileEditor fileEditor = e.getData(FileEditor.KEY);
        Editor editor = fileEditor instanceof TextEditor textEditor ? textEditor.getEditor() : e.getData(Editor.KEY);

        boolean enabled =
            fileEditor != null && (!Boolean.TRUE.equals(InternalEditorKeys.SUPPLEMENTARY_KEY.get(editor))) && fileEditor.getStructureViewBuilder() != null;
        e.getPresentation().setEnabled(enabled);
    }

    @Nonnull
    @RequiredReadAction
    public static StructureViewModel createStructureViewModel(
        @Nonnull Project project,
        @Nonnull FileEditor fileEditor,
        @Nonnull StructureView structureView
    ) {
        StructureViewModel treeModel;
        VirtualFile virtualFile = fileEditor.getFile();
        if (structureView instanceof StructureViewComposite && virtualFile != null) {
            StructureViewComposite.StructureViewDescriptor[] views = ((StructureViewComposite)structureView).getStructureViews();
            PsiFile psiFile = ObjectUtil.notNull(PsiManager.getInstance(project).findFile(virtualFile));
            treeModel = new StructureViewCompositeModel(psiFile, EditorUtil.getEditorEx(fileEditor), Arrays.asList(views));
            Disposer.register(structureView, treeModel);
        }
        else {
            treeModel = structureView.getTreeModel();
        }
        return treeModel;
    }
}
