/*
 * Copyright 2000-2011 JetBrains s.r.o.
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
package consulo.ide.impl.idea.codeInsight.navigation.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.codeInsight.navigation.MethodDownHandler;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.language.psi.PsiFile;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "MethodDown")
public class MethodDownAction extends BaseCodeInsightAction {
    public MethodDownAction() {
        super(
            ActionLocalize.actionMethoddownText(),
            ActionLocalize.actionMethoddownDescription(),
            PlatformIconGroup.actionsNextoccurence()
        );
    }

    @Nonnull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return new MethodDownHandler();
    }

    @Override
    @RequiredReadAction
    protected boolean isValidForLookup() {
        return true;
    }

    @Override
    @RequiredReadAction
    protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        return MethodUpAction.checkValidForFile(file);
    }
}