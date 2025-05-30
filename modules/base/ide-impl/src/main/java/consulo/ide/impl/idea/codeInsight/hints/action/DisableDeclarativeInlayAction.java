// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.ide.impl.idea.codeInsight.hints.DeclarativeInlayHintsPassFactory;
import consulo.ide.impl.idea.codeInsight.hints.settings.DeclarativeInlayHintsSettings;
import consulo.language.editor.CommonDataKeys;
import consulo.language.editor.inlay.DeclarativeInlayHintsProvider;
import consulo.localize.LocalizeValue;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionUpdateThread;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import jakarta.annotation.Nonnull;

@ActionImpl(id = "DisableDeclarativeInlayAction")
public class DisableDeclarativeInlayAction extends AnAction implements DumbAware {
    public DisableDeclarativeInlayAction() {
        super(CodeEditorLocalize.inlayHintsDeclarativeDisableActionNoProviderText());
    }

    @Nonnull
    @Override
    public ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
    }

    @RequiredUIAccess
    @Override
    public void update(AnActionEvent e) {
        LocalizeValue providerName = e.getData(DeclarativeInlayHintsProvider.PROVIDER_NAME);
        if (providerName == null) {
            e.getPresentation().setEnabledAndVisible(false);
            e.getPresentation().setTextValue(CodeEditorLocalize.inlayHintsDeclarativeDisableActionNoProviderText());
            return;
        }
        e.getPresentation().setEnabledAndVisible(true);
        e.getPresentation().setTextValue(CodeEditorLocalize.inlayHintsDeclarativeDisableActionText(providerName));
    }

    @RequiredUIAccess
    @Override
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) return;

        Editor editor = e.getData(CommonDataKeys.EDITOR);
        if (editor == null) return;

        String providerId = e.getData(DeclarativeInlayHintsProvider.PROVIDER_ID);
        if (providerId == null) return;

        DeclarativeInlayHintsSettings settings = DeclarativeInlayHintsSettings.getInstance();
        settings.setProviderEnabled(providerId, false);

        DeclarativeInlayHintsPassFactory.scheduleRecompute(editor, project);
    }
}
