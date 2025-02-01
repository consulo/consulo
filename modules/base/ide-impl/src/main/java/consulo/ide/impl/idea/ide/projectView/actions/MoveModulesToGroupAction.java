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

/**
 * @author cdr
 */
package consulo.ide.impl.idea.ide.projectView.actions;

import consulo.dataContext.DataContext;
import consulo.ide.IdeBundle;
import consulo.project.ui.view.tree.ModuleGroup;
import consulo.ide.impl.idea.openapi.roots.ui.configuration.projectRoot.ModuleStructureConfigurable;
import consulo.configurable.Settings;
import consulo.language.editor.LangDataKeys;
import consulo.module.ModifiableModuleModel;
import consulo.module.Module;
import consulo.module.impl.internal.ModuleManagerImpl;
import consulo.project.Project;
import consulo.project.ui.view.ProjectView;
import consulo.project.ui.view.ProjectViewPane;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;

import jakarta.annotation.Nullable;

public class MoveModulesToGroupAction extends AnAction {
  protected final ModuleGroup myModuleGroup;

  public MoveModulesToGroupAction(ModuleGroup moduleGroup, String title) {
    super(title);
    myModuleGroup = moduleGroup;
  }

  @Override
  public void update(AnActionEvent e) {
    Presentation presentation = getTemplatePresentation();
    final DataContext dataContext = e.getDataContext();
    final Module[] modules = dataContext.getData(LangDataKeys.MODULE_CONTEXT_ARRAY);

    String description = IdeBundle.message("message.move.modules.to.group", whatToMove(modules), myModuleGroup.presentableText());
    presentation.setDescription(description);
  }

  protected static String whatToMove(Module[] modules) {
    return modules.length == 1 ? IdeBundle.message("message.module", modules[0].getName()) : IdeBundle.message("message.modules");
  }

  @Override
  public void actionPerformed(AnActionEvent e) {
    final DataContext dataContext = e.getDataContext();
    final Module[] modules = dataContext.getData(LangDataKeys.MODULE_CONTEXT_ARRAY);
    doMove(modules, myModuleGroup, dataContext);
  }

  public static void doMove(final Module[] modules, final ModuleGroup group, @Nullable final DataContext dataContext) {
    Project project = modules[0].getProject();
    for (final Module module : modules) {
      ModifiableModuleModel model = dataContext != null
                                    ? dataContext.getData(LangDataKeys.MODIFIABLE_MODULE_MODEL)
                                    : null;
      if (model != null){
        model.setModuleGroupPath(module, group == null ? null : group.getGroupPath());
      } else {
        ModuleManagerImpl.getInstanceImpl(project).setModuleGroupPath(module, group == null ? null : group.getGroupPath());
      }
    }

    ProjectViewPane pane = ProjectView.getInstance(project).getCurrentProjectViewPane();
    if (pane != null) {
      pane.updateFromRoot(true);
    }

    boolean processedInsideSettings = false;

    if (dataContext != null) {
      Settings settings = dataContext.getData(Settings.KEY);
      if(settings != null) {
        ModuleStructureConfigurable configurable = settings.findConfigurable(ModuleStructureConfigurable.class);
        if(configurable != null) {
          processedInsideSettings = ModuleStructureConfigurable.processModulesMoved(configurable, modules, group);
        }
      }
    }

    if (!processedInsideSettings && pane != null) {
      if (group != null) {
        pane.selectModuleGroup(group, true);
      }
      else {
        pane.selectModule(modules[0], true);
      }
    }
  }
}
