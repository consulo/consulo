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

package consulo.ide.impl.idea.ide.fileTemplates.actions;

import consulo.ide.IdeView;
import consulo.ide.action.CreateFromTemplateActionBase;
import consulo.ide.impl.idea.ide.actions.EditFileTemplatesAction;
import consulo.ide.impl.idea.ide.fileTemplates.ui.SelectTemplateDialog;
import consulo.application.dumb.DumbAware;
import consulo.dataContext.DataContext;
import consulo.fileTemplate.FileTemplate;
import consulo.fileTemplate.FileTemplateManager;
import consulo.fileTemplate.impl.internal.FileTemplateImplUtil;
import consulo.ide.IdeBundle;
import consulo.language.editor.CommonDataKeys;
import consulo.language.psi.PsiDirectory;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CreateFromTemplateGroup extends ActionGroup implements DumbAware {
  @RequiredUIAccess
  @Override
  public void update(@Nonnull AnActionEvent event) {
    Project project = event.getData(CommonDataKeys.PROJECT);
    Presentation presentation = event.getPresentation();
    if (project != null && !project.isDisposed()) {
      FileTemplate[] allTemplates = FileTemplateManager.getInstance(project).getAllTemplates();
      for (FileTemplate template : allTemplates) {
        if (canCreateFromTemplate(event, template)) {
          presentation.setEnabled(true);
          return;
        }
      }
    }
    presentation.setEnabled(false);
  }

  @Override
  @Nonnull
  @RequiredUIAccess
  public AnAction[] getChildren(@Nullable AnActionEvent e) {
    if (e == null) return EMPTY_ARRAY;
    Project project = e.getData(CommonDataKeys.PROJECT);
    if (project == null || project.isDisposed()) return EMPTY_ARRAY;
    FileTemplateManager manager = FileTemplateManager.getInstance(project);
    FileTemplate[] templates = manager.getAllTemplates();

    boolean showAll = templates.length <= FileTemplateManager.RECENT_TEMPLATES_SIZE;
    if (!showAll) {
      Collection<String> recentNames = manager.getRecentNames();
      templates = new FileTemplate[recentNames.size()];
      int i = 0;
      for (String name : recentNames) {
        templates[i] = FileTemplateManager.getInstance(project).getTemplate(name);
        i++;
      }
    }

    Arrays.sort(templates, (template1, template2) -> {
      // group by type
      int i = template1.getExtension().compareTo(template2.getExtension());
      if (i != 0) {
        return i;
      }

      // group by name if same type
      return template1.getName().compareTo(template2.getName());
    });
    List<AnAction> result = new ArrayList<>();

    for (FileTemplate template : templates) {
      if (canCreateFromTemplate(e, template)) {
        AnAction action = new CreateFromTemplateAction(template);
        result.add(action);
      }
    }

    if (!result.isEmpty()) {
      if (!showAll) {
        result.add(new CreateFromTemplatesAction(IdeBundle.message("action.from.file.template")));
      }

      result.add(AnSeparator.getInstance());
      result.add(new EditFileTemplatesAction(IdeBundle.message("action.edit.file.templates")));
    }

    return result.toArray(new AnAction[result.size()]);
  }

  static boolean canCreateFromTemplate(AnActionEvent e, FileTemplate template) {
    if (e == null) return false;
    DataContext dataContext = e.getDataContext();
    IdeView view = dataContext.getData(IdeView.KEY);
    if (view == null) return false;

    PsiDirectory[] dirs = view.getDirectories();
    if (dirs.length == 0) return false;

    return FileTemplateImplUtil.canCreateFromTemplate(dirs, template);
  }

  private static class CreateFromTemplatesAction extends CreateFromTemplateActionBase {

    public CreateFromTemplatesAction(String title) {
      super(title, null, null);
    }

    @Override
    protected FileTemplate getTemplate(final Project project, final PsiDirectory dir) {
      SelectTemplateDialog dialog = new SelectTemplateDialog(project, dir);
      dialog.show();
      return dialog.getSelectedTemplate();
    }
  }

}
