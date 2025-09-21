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
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.sandboxPlugin.lang.SandFileType;
import consulo.ui.ex.action.IdeActions;
import jakarta.annotation.Nonnull;

/**
 * @author VISTALL
 * @since 2022-07-31
 */
@ActionImpl(id = "CreateSandFile", parents = @ActionParentRef(@ActionRef(id = IdeActions.GROUP_NEW)))
public class CreateSandFileAction extends CreateFileFromTemplateAction {
    public CreateSandFileAction() {
        super("Sand File", "", SandFileType.INSTANCE.getIcon());
    }

    @Override
    protected void buildDialog(Project project, PsiDirectory directory, CreateFileFromTemplateDialog.Builder builder) {
        builder.addKind(LocalizeValue.localizeTODO("Sand File"), SandFileType.INSTANCE.getIcon(), "Sand File");
    }

    @Override
    @Nonnull
    protected LocalizeValue getActionName(PsiDirectory directory, String newName, String templateName) {
        return LocalizeValue.localizeTODO("Sand File");
    }
}
