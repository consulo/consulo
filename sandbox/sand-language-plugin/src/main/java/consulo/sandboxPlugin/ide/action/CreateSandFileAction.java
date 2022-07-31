/*
 * Copyright 2013-2022 consulo.io
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
package consulo.sandboxPlugin.ide.action;

import consulo.annotation.component.ActionImpl;
import consulo.annotation.component.ActionParentRef;
import consulo.annotation.component.ActionRef;
import consulo.ide.action.CreateFileFromTemplateAction;
import consulo.ide.action.CreateFileFromTemplateDialog;
import consulo.language.psi.PsiDirectory;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.SandFileType;

/**
 * @author VISTALL
 * @since 31-Jul-22
 */
@ActionImpl(id = "CreateSandFile", parents = @ActionParentRef(@ActionRef(id = "NewGroup")))
public class CreateSandFileAction extends CreateFileFromTemplateAction {
  public CreateSandFileAction() {
    super("Sand File", "", SandFileType.INSTANCE.getIcon());
  }

  @Override
  protected void buildDialog(Project project, PsiDirectory directory, CreateFileFromTemplateDialog.Builder builder) {
    builder.addKind("Sand File", SandFileType.INSTANCE.getIcon(), "Sand File");
  }

  @Override
  protected String getActionName(PsiDirectory directory, String newName, String templateName) {
    return "Sand File";
  }
}
