// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints.action;

import consulo.annotation.component.ActionImpl;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.ide.impl.idea.codeInsight.hints.DeclarativeInlayHintsPassFactory;
import consulo.language.editor.impl.internal.inlay.setting.DeclarativeInlayHintsSettings;
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

    @Override
    public void update(@Nonnull AnActionEvent e) {
        LocalizeValue providerName = e.getData(DeclarativeInlayHintsProvider.PROVIDER_NAME);
        e.getPresentation().setEnabledAndVisible(providerName != null);
        e.getPresentation().setTextValue(
            providerName != null
                ? CodeEditorLocalize.inlayHintsDeclarativeDisableActionText(providerName)
                : CodeEditorLocalize.inlayHintsDeclarativeDisableActionNoProviderText()
        );
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(AnActionEvent e) {
        Project project = e.getData(Project.KEY);
        if (project == null) {
            return;
        }

        Editor editor = e.getData(Editor.KEY);
        if (editor == null) {
            return;
        }

        String providerId = e.getData(DeclarativeInlayHintsProvider.PROVIDER_ID);
        if (providerId == null) {
            return;
        }

        DeclarativeInlayHintsSettings settings = DeclarativeInlayHintsSettings.getInstance();
        settings.setProviderEnabled(providerId, false);

        DeclarativeInlayHintsPassFactory.scheduleRecompute(editor, project);
    }
}
