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
import consulo.fileEditor.structureView.StructureViewBuilder;
import consulo.fileEditor.structureView.TreeBasedStructureViewBuilder;
import consulo.ide.impl.idea.codeInsight.navigation.MethodUpHandler;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.language.editor.structureView.PsiStructureViewFactory;
import consulo.language.editor.structureView.TemplateLanguageStructureViewBuilder;
import consulo.language.psi.PsiFile;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "MethodUp")
public class MethodUpAction extends BaseCodeInsightAction {
    public MethodUpAction() {
        super(
            ActionLocalize.actionMethodupText(),
            ActionLocalize.actionMethodupDescription(),
            PlatformIconGroup.actionsPreviousoccurence()
        );
    }

    @Nonnull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return new MethodUpHandler();
    }

    @Override
    protected boolean isValidForLookup() {
        return true;
    }

    @Override
    @RequiredReadAction
    protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        return checkValidForFile(file);
    }

    @RequiredReadAction
    static boolean checkValidForFile(PsiFile file) {
        StructureViewBuilder structureViewBuilder = PsiStructureViewFactory.createBuilderForFile(file);
        return structureViewBuilder instanceof TreeBasedStructureViewBuilder || structureViewBuilder instanceof TemplateLanguageStructureViewBuilder;
    }
}