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

package consulo.ide.impl.idea.ide.fileTemplates.actions;

import consulo.fileTemplate.FileTemplate;
import consulo.fileTemplate.FileTemplateUtil;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.Presentation;
import consulo.project.Project;
import consulo.language.psi.PsiDirectory;

public class CreateFromTemplateAction extends CreateFromTemplateActionBase {

  private final FileTemplate myTemplate;

  public CreateFromTemplateAction(FileTemplate template){
    super(template.getName(), null, FileTemplateUtil.getIcon(template));
    myTemplate = template;
  }

  @Override
  protected FileTemplate getTemplate(final Project project, final PsiDirectory dir) {
    return myTemplate;
  }

  @Override
  public void update(AnActionEvent e){
    super.update(e);
    Presentation presentation = e.getPresentation();
    boolean isEnabled = CreateFromTemplateGroup.canCreateFromTemplate(e, myTemplate);
    presentation.setEnabled(isEnabled);
    presentation.setVisible(isEnabled);
  }

  public FileTemplate getTemplate() {
    return myTemplate;
  }
}
