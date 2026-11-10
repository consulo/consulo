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

package consulo.ide.impl.idea.ide.projectView.impl;

import consulo.annotation.component.ExtensionImpl;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.language.editor.refactoring.TitledHandler;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.rename.RenameHandler;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.ModuleWithNameAlreadyExistsException;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.InputBoxBuilder;
import consulo.ui.InputProblem;
import consulo.ui.InputValidator;
import consulo.ui.TextBox;
import consulo.undoRedo.CommandProcessor;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * @author dsl
 */
@ExtensionImpl
public class RenameModuleHandler implements RenameHandler, TitledHandler {
    private static final Logger LOG = Logger.getInstance(RenameModuleHandler.class);

    @Override
    public boolean isAvailableOnDataContext(DataContext dataContext) {
        Module module = dataContext.getData(LangDataKeys.MODULE_CONTEXT);
        return module != null;
    }

    @Override
    public boolean isRenaming(DataContext dataContext) {
        return isAvailableOnDataContext(dataContext);
    }

    @Override
    @RequiredUIAccess
    public void invoke(Project project, Editor editor, PsiFile file, DataContext dataContext) {
        LOG.assertTrue(false);
    }

    @Override
    @RequiredUIAccess
    public void invoke(Project project, PsiElement[] elements, DataContext dataContext) {
        Module module = dataContext.getData(LangDataKeys.MODULE_CONTEXT);
        LOG.assertTrue(module != null);
        InputBoxBuilder.text()
            .asQuestion()
            .title(IdeLocalize.titleRenameModule())
            .text(IdeLocalize.promptEnterNewModuleName())
            .value(module.getName())
            .setupComponent(TextBox::selectAll)
            .validator(new ModuleNameValidator(project, module))
            .showAsync(project);
    }

    @Override
    public LocalizeValue getActionTitleValue() {
        return RefactoringLocalize.renameModuleTitle();
    }

    private static class ModuleNameValidator implements InputValidator<String> {
        private final Project myProject;
        private final Module myModule;

        public ModuleNameValidator(Project project, Module module) {
            myProject = project;
            myModule = module;
        }

        @Override
        @RequiredUIAccess
        public @Nullable InputProblem validate(String inputString) {
            return inputString.isEmpty() ? InputProblem.error(LocalizeValue.empty()) : null;
        }

        @Override
        @RequiredUIAccess
        public CompletableFuture<@Nullable InputProblem> confirm(String inputString) {
            String oldName = myModule.getName();
            ModifiableModuleModel modifiableModel = ModuleManager.getInstance(myProject).getModifiableModel();
            try {
                modifiableModel.renameModule(myModule, inputString);
            }
            catch (ModuleWithNameAlreadyExistsException moduleWithNameAlreadyExists) {
                return CompletableFuture.completedFuture(InputProblem.error(IdeLocalize.errorModuleAlreadyExists(inputString)));
            }
            CommandProcessor.getInstance().newCommand()
                .project(myProject)
                .name(IdeLocalize.commandRenamingModule(oldName))
                .inWriteAction()
                .run(() -> modifiableModel.commit());
            return CompletableFuture.completedFuture(null);
        }
    }
}
