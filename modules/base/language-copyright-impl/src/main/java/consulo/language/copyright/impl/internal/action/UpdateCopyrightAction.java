/*
 * Copyright 2000-2013 JetBrains s.r.o.
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
package consulo.language.copyright.impl.internal.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.codeEditor.Editor;
import consulo.language.copyright.UpdateCopyrightsProvider;
import consulo.language.copyright.config.CopyrightManager;
import consulo.language.copyright.impl.internal.pattern.FileUtil;
import consulo.language.copyright.localize.LanguageCopyrightLocalize;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.PlatformDataKeys;
import consulo.language.editor.impl.action.BaseAnalysisAction;
import consulo.language.editor.scope.AnalysisScope;
import consulo.language.psi.*;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.AnActionEvent;
import consulo.virtualFileSystem.VirtualFile;
import jakarta.annotation.Nonnull;

@ActionImpl(
    id = "UpdateCopyright",
    parents = {
        @ActionParentRef(@ActionRef(id = "ProjectViewPopupMenu")),
        @ActionParentRef(@ActionRef(id = "CodeMenu")),
        @ActionParentRef(@ActionRef(id = "NavbarPopupMenu"))
    }
)
public class UpdateCopyrightAction extends BaseAnalysisAction {
    public UpdateCopyrightAction() {
        super(
            LanguageCopyrightLocalize.actionUpdateCopyrightText(),
            LanguageCopyrightLocalize.actionUpdateCopyrightDescription(),
            LanguageCopyrightLocalize.actionUpdateCopyrightText(),
            LanguageCopyrightLocalize.actionUpdateCopyrightText()
        );
    }

    @Override
    public void update(@Nonnull AnActionEvent event) {
        boolean enabled = isEnabled(event);
        event.getPresentation().setEnabled(enabled);
        if (ActionPlaces.isPopupPlace(event.getPlace())) {
            event.getPresentation().setVisible(enabled);
        }
    }

    private static boolean isEnabled(@Nonnull AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return false;
        }

        if (!CopyrightManager.getInstance(project).hasAnyCopyrights()) {
            return false;
        }
        VirtualFile[] files = e.getData(VirtualFile.KEY_OF_ARRAY);
        Editor editor = e.getData(Editor.KEY);
        if (editor != null) {
            PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            if (file == null || !UpdateCopyrightsProvider.hasExtension(file)) {
                return false;
            }
        }
        else if (files != null && FileUtil.areFiles(files)) {
            boolean copyrightEnabled = false;
            for (VirtualFile vfile : files) {
                if (UpdateCopyrightsProvider.hasExtension(vfile)) {
                    copyrightEnabled = true;
                    break;
                }
            }
            if (!copyrightEnabled) {
                return false;
            }

        }
        else if ((files == null || files.length != 1)
            && !e.hasData(LangDataKeys.MODULE_CONTEXT)
            && !e.hasData(LangDataKeys.MODULE_CONTEXT_ARRAY)
            && !e.hasData(PlatformDataKeys.PROJECT_CONTEXT)) {
            PsiElement[] elems = e.getData(PsiElement.KEY_OF_ARRAY);
            if (elems != null) {
                boolean copyrightEnabled = false;
                for (PsiElement elem : elems) {
                    if (elem instanceof PsiDirectory) {
                        continue;
                    }
                    PsiFile file = elem.getContainingFile();
                    if (file == null || !UpdateCopyrightsProvider.hasExtension(file.getVirtualFile())) {
                        copyrightEnabled = true;
                        break;
                    }
                }
                if (!copyrightEnabled) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected void analyze(@Nonnull final Project project, @Nonnull AnalysisScope scope) {
        if (scope.checkScopeWritable(project)) {
            return;
        }
        scope.accept(new PsiElementVisitor() {
            @Override
            @RequiredUIAccess
            public void visitFile(PsiFile file) {
                new UpdateCopyrightProcessor(project, file.getModule(), file).run();
            }
        });
    }
}