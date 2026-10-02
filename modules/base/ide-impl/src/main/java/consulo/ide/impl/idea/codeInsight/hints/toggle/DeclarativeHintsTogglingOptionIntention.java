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

import java.util.function.BiFunction;

public class DeclarativeHintsTogglingOptionIntention implements IntentionAction, LowPriorityAction, DumbAware {
    private final String myOptionId;
    private final String myProviderId;
    private final LocalizeValue myProviderName;
    private final LocalizeValue myOptionName;
    private final Mode myMode;

    public DeclarativeHintsTogglingOptionIntention(
        String optionId,
        String providerId,
        LocalizeValue providerName,
        LocalizeValue optionName,
        Mode mode
    ) {
        myOptionId = optionId;
        myProviderId = providerId;
        myProviderName = providerName;
        myOptionName = optionName;
        myMode = mode;
    }

    @Override
    public boolean startInWriteAction() {
        return false;
    }

    @Override
    public LocalizeValue getText() {
        return myMode.getText(myOptionName, myProviderName);
    }

    @Override
    public boolean isAvailable(Project project, Editor editor, PsiFile file) {
        return true;
    }

    @Override
    public void invoke(Project project, Editor editor, PsiFile file) {
        DeclarativeInlayHintsSettings settings = DeclarativeInlayHintsSettings.getInstance();

        switch (myMode) {
            case ENABLE_PROVIDER_AND_OPTION -> {
                settings.setProviderEnabled(myProviderId, true);
                settings.setOptionEnabled(myOptionId, myProviderId, true);
            }
            case ENABLE_OPTION -> settings.setOptionEnabled(myOptionId, myProviderId, true);
            case DISABLE_OPTION -> settings.setOptionEnabled(myOptionId, myProviderId, false);
        }
        DeclarativeInlayHintsPassFactory.scheduleRecompute(editor, project);
    }

    public enum Mode {
        ENABLE_PROVIDER_AND_OPTION(CodeEditorLocalize::inlayHintsDeclarativeEnableOptionActionText),
        ENABLE_OPTION(CodeEditorLocalize::inlayHintsDeclarativeEnableOptionActionText),
        DISABLE_OPTION(CodeEditorLocalize::inlayHintsDeclarativeDisableOptionActionText);

        private final BiFunction<Object, Object, LocalizeValue> myMessage;

        Mode(BiFunction<Object, Object, LocalizeValue> message) {
            myMessage = message;
        }

        public LocalizeValue getText(LocalizeValue optionName, LocalizeValue providerName) {
            return myMessage.apply(optionName, providerName);
        }
    }
}
