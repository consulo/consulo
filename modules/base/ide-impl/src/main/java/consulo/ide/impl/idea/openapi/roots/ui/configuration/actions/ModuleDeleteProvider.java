/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package consulo.ide.impl.idea.openapi.roots.ui.configuration.actions;

import consulo.application.ApplicationManager;
import consulo.dataContext.DataContext;
import consulo.ide.impl.idea.ide.TitledHandler;
import consulo.util.lang.StringUtil;
import consulo.ide.impl.idea.util.ArrayUtilRt;
import consulo.language.editor.LangDataKeys;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.ModuleManager;
import consulo.module.content.ModifiableModelCommitter;
import consulo.module.content.ModuleRootManager;
import consulo.module.content.layer.ModifiableRootModel;
import consulo.module.content.layer.orderEntry.ModuleOrderEntry;
import consulo.module.content.layer.orderEntry.OrderEntry;
import consulo.project.Project;
import consulo.project.ProjectBundle;
import consulo.ui.ex.DeleteProvider;
import consulo.ui.ex.awt.Messages;
import consulo.undoRedo.CommandProcessor;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ModuleDeleteProvider  implements DeleteProvider, TitledHandler  {
  @Override
  public boolean canDeleteElement(@Nonnull DataContext dataContext) {
    final Module[] modules = dataContext.getData(LangDataKeys.MODULE_CONTEXT_ARRAY);
    return modules != null;
  }

  @Override
  public void deleteElement(@Nonnull DataContext dataContext) {
    final Module[] modules = dataContext.getData(LangDataKeys.MODULE_CONTEXT_ARRAY);
    assert modules != null;
    final Project project = dataContext.getData(Project.KEY);
    String names = StringUtil.join(Arrays.asList(modules), module -> "\'" + module.getName() + "\'", ", ");
    int ret = Messages.showOkCancelDialog(getConfirmationText(modules, names), getActionTitle(), Messages.getQuestionIcon());
    if (ret != 0) return;
    CommandProcessor.getInstance().executeCommand(project, () -> {
      final Runnable action = () -> {
        final ModuleManager moduleManager = ModuleManager.getInstance(project);
        final Module[] currentModules = moduleManager.getModules();
        final ModifiableModuleModel modifiableModuleModel = moduleManager.getModifiableModel();
        final Map<Module, ModifiableRootModel> otherModuleRootModels = new HashMap<>();
        for (final Module module : modules) {
          final ModifiableRootModel modifiableModel = ModuleRootManager.getInstance(module).getModifiableModel();
          for (final Module otherModule : currentModules) {
            if (otherModule == module || ArrayUtilRt.find(modules, otherModule) != -1) continue;
            if (!otherModuleRootModels.containsKey(otherModule)) {
              otherModuleRootModels.put(otherModule, ModuleRootManager.getInstance(otherModule).getModifiableModel());
            }
          }
          removeModule(module, modifiableModel, otherModuleRootModels.values(), modifiableModuleModel);
        }
        final ModifiableRootModel[] modifiableRootModels = otherModuleRootModels.values().toArray(new ModifiableRootModel[otherModuleRootModels.size()]);
        ModifiableModelCommitter.getInstance(project).multiCommit(modifiableRootModels, modifiableModuleModel);
      };
      ApplicationManager.getApplication().runWriteAction(action);
    }, ProjectBundle.message("module.remove.command"), null);
  }

  private static String getConfirmationText(Module[] modules, String names) {
    return ProjectBundle.message("module.remove.confirmation.prompt", names, modules.length);
  }

  @Override
  public String getActionTitle() {
    return "Remove Module";
  }

  public static void removeModule(@Nonnull final Module moduleToRemove,
                                   @Nullable ModifiableRootModel modifiableRootModelToRemove,
                                   @Nonnull Collection<ModifiableRootModel> otherModuleRootModels,
                                   @Nonnull final ModifiableModuleModel moduleModel) {
    // remove all dependencies on the module that is about to be removed
    for (final ModifiableRootModel modifiableRootModel : otherModuleRootModels) {
      final OrderEntry[] orderEntries = modifiableRootModel.getOrderEntries();
      for (final OrderEntry orderEntry : orderEntries) {
        if (orderEntry instanceof ModuleOrderEntry && orderEntry.isValid()) {
          final Module orderEntryModule = ((ModuleOrderEntry)orderEntry).getModule();
          if (orderEntryModule != null && orderEntryModule.equals(moduleToRemove)) {
            modifiableRootModel.removeOrderEntry(orderEntry);
          }
        }
      }
    }
    // destroyProcess editor
    if (modifiableRootModelToRemove != null) {
      modifiableRootModelToRemove.dispose();
    }
    // destroyProcess module
    moduleModel.disposeModule(moduleToRemove);
  }
}
