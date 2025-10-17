// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.codeInsight.generation.actions;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ActionImpl;
import consulo.codeEditor.Editor;
import consulo.ide.impl.idea.codeInsight.generation.surroundWith.SurroundWithHandler;
import consulo.language.Language;
import consulo.language.editor.action.CodeInsightActionHandler;
import consulo.language.editor.impl.action.BaseCodeInsightAction;
import consulo.language.editor.surroundWith.SurroundDescriptor;
import consulo.language.editor.template.TemplateManager;
import consulo.language.editor.template.context.TemplateActionContext;
import consulo.language.psi.PsiFile;
import consulo.language.psi.PsiUtilCore;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;

import jakarta.annotation.Nonnull;

@ActionImpl(id = "SurroundWith")
public class SurroundWithAction extends BaseCodeInsightAction {
    public SurroundWithAction() {
        super(ActionLocalize.actionSurroundwithText(), ActionLocalize.actionSurroundwithDescription());
        setEnabledInModalContext(true);
    }

    @Nonnull
    @Override
    protected CodeInsightActionHandler getHandler() {
        return new SurroundWithHandler();
    }

    @Override
    @RequiredReadAction
    protected boolean isValidForFile(@Nonnull Project project, @Nonnull Editor editor, @Nonnull PsiFile file) {
        Language language = file.getLanguage();
        if (!SurroundDescriptor.forLanguage(language).isEmpty()) {
            return true;
        }
        PsiFile baseFile = PsiUtilCore.getTemplateLanguageFile(file);
        //noinspection SimplifiableIfStatement
        if (baseFile != null && baseFile != file && !SurroundDescriptor.forLanguage(baseFile.getLanguage()).isEmpty()) {
            return true;
        }

        return !TemplateManager.getInstance(project)
            .listApplicableTemplateWithInsertingDummyIdentifier(TemplateActionContext.surrounding(file, editor))
            .isEmpty();
    }
}