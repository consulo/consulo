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
import consulo.ide.impl.idea.ide.TitledHandler;
import consulo.ide.localize.IdeLocalize;
import consulo.language.editor.LangDataKeys;
import consulo.language.editor.refactoring.localize.RefactoringLocalize;
import consulo.language.editor.refactoring.rename.RenameHandler;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.logging.Logger;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.ModuleWithNameAlreadyExistsException;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.InputValidator;
import consulo.ui.ex.awt.Messages;
import consulo.ui.ex.awt.UIUtil;
import consulo.undoRedo.CommandProcessor;
import consulo.util.lang.ref.Ref;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

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
  public void invoke(@Nonnull Project project, Editor editor, PsiFile file, DataContext dataContext) {
    LOG.assertTrue(false);
  }

  @Override
  @RequiredUIAccess
  public void invoke(@Nonnull final Project project, @Nonnull PsiElement[] elements, @Nonnull DataContext dataContext) {
    final Module module = dataContext.getData(LangDataKeys.MODULE_CONTEXT);
    LOG.assertTrue(module != null);
    Messages.showInputDialog(
      project,
      IdeLocalize.promptEnterNewModuleName().get(),
      IdeLocalize.titleRenameModule().get(),
      UIUtil.getQuestionIcon(),
      module.getName(),
      new MyInputValidator(project, module)
    );
  }

  @Nonnull
  @Override
  public String getActionTitle() {
    return RefactoringLocalize.renameModuleTitle().get();
  }

  private static class MyInputValidator implements InputValidator {
    private final Project myProject;
    private final Module myModule;
    public MyInputValidator(Project project, Module module) {
      myProject = project;
      myModule = module;
    }

    @Override
    @RequiredUIAccess
    public boolean checkInput(String inputString) {
      return inputString != null && inputString.length() > 0;
    }

    @Override
    @RequiredUIAccess
    public boolean canClose(final String inputString) {
      final String oldName = myModule.getName();
      final ModifiableModuleModel modifiableModel = renameModule(inputString);
      if (modifiableModel == null) return false;
      final Ref<Boolean> success = Ref.create(Boolean.TRUE);
      CommandProcessor.getInstance().executeCommand(
        myProject,
        () -> myProject.getApplication().runWriteAction(() -> modifiableModel.commit()),
        IdeLocalize.commandRenamingModule(oldName).get(),
        null
      );
      return success.get();
    }

    @Nullable
    @RequiredUIAccess
    private ModifiableModuleModel renameModule(String inputString) {
      final ModifiableModuleModel modifiableModel = ModuleManager.getInstance(myProject).getModifiableModel();
      try {
        modifiableModel.renameModule(myModule, inputString);
      }
      catch (ModuleWithNameAlreadyExistsException moduleWithNameAlreadyExists) {
        Messages.showErrorDialog(
          myProject,
          IdeLocalize.errorModuleAlreadyExists(inputString).get(),
          IdeLocalize.titleRenameModule().get()
        );
        return null;
      }
      return modifiableModel;
    }
  }

}
