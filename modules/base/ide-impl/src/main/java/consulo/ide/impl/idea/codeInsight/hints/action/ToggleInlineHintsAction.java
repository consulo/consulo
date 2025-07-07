// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.codeEditor.EditorFactory;
import consulo.ide.impl.idea.codeInsight.hints.ParameterHintsPassFactory;
import consulo.language.Language;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.impl.internal.inlay.param.HintUtils;
import consulo.language.editor.inlay.InlayParameterHintsProvider;
import consulo.language.editor.internal.ParameterNameHintsSettings;
import consulo.language.editor.localize.CodeInsightLocalize;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DumbAwareAction;
import consulo.ui.ex.action.Presentation;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

@ActionImpl(id = "ToggleInlineHintsAction")
public class ToggleInlineHintsAction extends DumbAwareAction {
    private final Application myApplication;

    @Inject
    public ToggleInlineHintsAction(Application application) {
        super(CodeInsightLocalize.actionToggleinlinehintsactionText(), CodeInsightLocalize.actionToggleinlinehintsactionDescription());
        myApplication = application;
    }

    @Nonnull
    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @Override
    public void update(@Nonnull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        if (!myApplication.getExtensionPoint(InlayParameterHintsProvider.class).hasAnyExtensions()) {
            presentation.setEnabledAndVisible(false);
            return;
        }
        PsiFile file = e.getData(PsiFile.KEY);
        if (file == null) {
            presentation.setEnabledAndVisible(false);
            return;
        }

        Language language = file.getLanguage();

        boolean isHintsShownNow = HintUtils.isParameterHintsEnabledForLanguage(language);
        presentation.setTextValue(isHintsShownNow ? CodeInsightLocalize.inlayHintsDisableActionText() : CodeInsightLocalize.inlayHintsEnableActionText());
        presentation.setEnabledAndVisible(true);
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        PsiFile file = e.getData(PsiFile.KEY);
        Project project = e.getData(Project.KEY);
        if (file == null || project == null) {
            return;
        }

        Language language = file.getLanguage();

        boolean isHintsShownNow = HintUtils.isParameterHintsEnabledForLanguage(language);

        ParameterNameHintsSettings.getInstance().setEnabledForLanguage(!isHintsShownNow, HintUtils.getLanguageForSettingKey(language));

        Editor editor = e.getData(Editor.KEY);
        if (editor != null) {
            ParameterHintsPassFactory.forceHintsUpdateOnNextPass(editor);
        }

        EditorFactory.getInstance().refreshAllEditors();

        DaemonCodeAnalyzer.getInstance(project).restart();
    }
}
