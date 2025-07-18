/*
 * Copyright 2013-2021 consulo.io
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

import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.document.FileDocumentManager;
import consulo.ide.impl.idea.openapi.actionSystem.impl.SimpleDataContext;
import consulo.ide.impl.idea.ui.tabs.impl.TabLabel;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiFileSystemItem;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import consulo.ui.ex.awt.CopyPasteManager;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.datatransfer.StringSelection;
import java.util.List;
import java.util.Optional;

public class CopyPathProvider extends DumbAwareAction {
    public static final Key<String> QUALIFIED_NAME = Key.of("QUALIFIED_NAME");

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        Project project = e.getRequiredData(Project.KEY);
        Editor editor = e.getData(Editor.KEY);

        DataContext customDataContext = createCustomDataContext(e.getDataContext());

        List<PsiElement> elements = CopyReferenceUtil.getElementsToCopy(editor, customDataContext);
        String copy = getQualifiedName(project, elements, editor, customDataContext);
        CopyPasteManager.getInstance().setContents(new StringSelection(copy));

        CopyReferenceUtil.highlight(editor, project, elements);
    }

    private DataContext createCustomDataContext(DataContext dataContext) {
        if (!(dataContext.getData(UIExAWTDataKey.CONTEXT_COMPONENT) instanceof TabLabel tabLabel)) {
            return dataContext;
        }

        Object file = tabLabel.getInfo().getObject();
        if (!(file instanceof VirtualFile)) {
            return dataContext;
        }

        return SimpleDataContext.builder().setParent(dataContext)
            .add(VirtualFile.KEY, (VirtualFile) file)
            .add(VirtualFile.KEY_OF_ARRAY, new VirtualFile[]{(VirtualFile) file})
            .build();
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Editor editor = e.getData(Editor.KEY);
        Project project = e.getData(Project.KEY);

        DataContext dataContext = e.getDataContext();
        String qualifiedName = project != null
            ? getQualifiedName(project, CopyReferenceUtil.getElementsToCopy(editor, dataContext), editor, dataContext)
            : null;

        Presentation presentation = e.getPresentation();
        presentation.putClientProperty(QUALIFIED_NAME, qualifiedName);
        presentation.setEnabledAndVisible(qualifiedName != null);
    }

    @Nullable
    public String getQualifiedName(Project project, List<PsiElement> elements, Editor editor, DataContext dataContext) {
        if (elements.isEmpty()) {
            VirtualFile file = Optional.ofNullable(editor)
                .map(Editor::getDocument)
                .map(it -> FileDocumentManager.getInstance().getFile(it))
                .orElse(null);
            return getPathToElement(project, file, editor);
        }
        else {
            List<VirtualFile> files = ContainerUtil.mapNotNull(
                elements,
                it -> {
                    if (it instanceof PsiFileSystemItem psiFileSystemItem) {
                        return psiFileSystemItem.getVirtualFile();
                    }
                    else {
                        PsiFile containingFile = it.getContainingFile();
                        return containingFile == null ? null : containingFile.getVirtualFile();
                    }
                }
            );

            if (files.isEmpty()) {
                VirtualFile[] contextFiles = dataContext.getData(VirtualFile.KEY_OF_ARRAY);
                if (contextFiles != null && contextFiles.length > 0) {
                    files = List.of(contextFiles);
                }
            }

            if (files.isEmpty()) {
                return null;
            }

            List<String> paths = ContainerUtil.mapNotNull(files, file -> getPathToElement(project, file, editor));
            if (paths.isEmpty()) {
                return null;
            }
            return String.join("\n", paths);
        }
    }

    @Nullable
    public String getPathToElement(Project project, @Nullable VirtualFile virtualFile, @Nullable Editor editor) {
        return null;
    }
}
