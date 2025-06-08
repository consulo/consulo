// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.codeEditor.EditorFactory;
import consulo.codeEditor.PersistentEditorSettings;
import consulo.language.editor.DaemonCodeAnalyzer;
import consulo.language.editor.inlay.InlayParameterHintsProvider;
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

    @RequiredUIAccess
    @Override
    public void update(AnActionEvent e) {
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
        
        boolean isHintsShownNow = PersistentEditorSettings.getInstance().isShowParameterNameHints();
        presentation.setTextValue(isHintsShownNow ? CodeInsightLocalize.inlayHintsDisableActionText() : CodeInsightLocalize.inlayHintsEnableActionText());
        presentation.setEnabledAndVisible(true);
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        PsiFile file = e.getData(PsiFile.KEY);
        Project project = e.getData(Project.KEY);
        if (file == null || project == null) {
            return;
        }
        PersistentEditorSettings settings = PersistentEditorSettings.getInstance();
        boolean isHintsShownNow = settings.isShowParameterNameHints();

        settings.setShowParameterNameHints(!isHintsShownNow);

        EditorFactory.getInstance().refreshAllEditors();
        DaemonCodeAnalyzer.getInstance(project).restart();
    }
}
