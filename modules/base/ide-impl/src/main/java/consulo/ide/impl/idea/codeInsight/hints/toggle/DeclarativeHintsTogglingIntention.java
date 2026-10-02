// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.codeInsight.hints.toggle;

import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.localize.CodeEditorLocalize;
import consulo.ide.impl.idea.codeInsight.hints.DeclarativeInlayHintsPassFactory;
import consulo.language.editor.inlay.DeclarativeInlayHintsSettings;
import consulo.language.editor.intention.IntentionAction;
import consulo.language.editor.intention.LowPriorityAction;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.project.Project;

public class DeclarativeHintsTogglingIntention implements IntentionAction, LowPriorityAction, DumbAware {
    private final String myProviderId;
    private final LocalizeValue myProviderName;
    private final boolean myProviderEnabledNow;

    public DeclarativeHintsTogglingIntention(String providerId, LocalizeValue providerName, boolean providerEnabledNow) {
        myProviderId = providerId;
        myProviderName = providerName;
        myProviderEnabledNow = providerEnabledNow;
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    public LocalizeValue getText() {
        if (myProviderEnabledNow) {
            return CodeEditorLocalize.inlayHintsDeclarativeDisableActionText(myProviderName);
        }
        else {
            return CodeEditorLocalize.inlayHintsDeclarativeEnableActionText(myProviderName);
        }
    }

    @Override
    public boolean isAvailable(Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke(Project project, Editor editor, PsiFile file) {
        DeclarativeInlayHintsSettings settings = DeclarativeInlayHintsSettings.getInstance();
        settings.setProviderEnabled(myProviderId, !myProviderEnabledNow);
        DeclarativeInlayHintsPassFactory.scheduleRecompute(editor, project);
    }
}
