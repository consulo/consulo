// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.refactoring.suggested;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ExtensionImpl;
import consulo.application.Application;
import consulo.codeEditor.Editor;
import consulo.document.Document;
import consulo.document.util.TextRange;
import consulo.language.editor.inspection.PriorityAction;
import consulo.language.editor.intention.SyntheticIntentionAction;
import consulo.language.editor.internal.intention.IntentionActionDescriptor;
import consulo.language.editor.internal.intention.IntentionMenuContributor;
import consulo.language.editor.internal.intention.IntentionsInfo;
import consulo.language.editor.refactoring.suggested.SuggestedChangeSignatureData;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringData;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringState;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringState.ErrorLevel;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringSupport;
import consulo.language.editor.refactoring.suggested.SuggestedRenameData;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionPlaces;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

@ExtensionImpl
public class SuggestedRefactoringIntentionContributor implements IntentionMenuContributor {
    static final Key<SuggestedRefactoringData> REFACTORING_DATA_KEY = Key.create("suggested.refactoring.data");

    @Override
    @RequiredReadAction
    public void collectActions(Editor hostEditor, PsiFile hostFile, IntentionsInfo intentions, int passIdToShowIntentionsFor, int offset) {
        MyIntention intention = suggestRefactoringIntention(hostFile, offset);

        if (intention == null) {
            hostFile.putUserData(REFACTORING_DATA_KEY, null);
            return;
        }

        // we add it into 'errorFixesToShow' if it's not empty to always be at the top of the list
        // we don't add into it if it's empty to keep the color of the bulb
        intentions.inspectionFixesToShow.add(new IntentionActionDescriptor(
            intention,
            null,
            LocalizeValue.empty(),
            PlatformIconGroup.actionsSuggestedrefactoringbulb()
        ));
    }

    @RequiredReadAction
    private static @Nullable MyIntention suggestRefactoringIntention(PsiFile hostFile, int offset) {
        Project project = hostFile.getProject();
        SuggestedRefactoringProviderImpl refactoringProvider = SuggestedRefactoringProviderImpl.getInstance(project);
        SuggestedRefactoringState state = refactoringProvider.getState();
        if (state == null) {
            return null;
        }

        PsiElement anchor = state.getAnchor();
        if (!anchor.isValid() || state.getErrorLevel() == ErrorLevel.INCONSISTENT) {
            return null;
        }
        if (!hostFile.equals(anchor.getContainingFile())) {
            return null;
        }

        SuggestedRefactoringSupport refactoringSupport = state.getRefactoringSupport();

        if (refactoringSupport.getAvailability().shouldSuppressRefactoringForDeclaration(state)) {
            // additional checks showed that the initial declaration was unsuitable for refactoring
            Application.get().invokeLater(refactoringProvider::suppressForCurrentDeclaration);
            return null;
        }

        if (state.getErrorLevel() != ErrorLevel.NO_ERRORS) {
            return null;
        }

        state = refactoringSupport.getAvailability().refineSignaturesWithResolve(state);

        if (state.getErrorLevel() == ErrorLevel.SYNTAX_ERROR || state.getOldSignature().equals(state.getNewSignature())) {
            Document document = Objects.requireNonNull(PsiDocumentManager.getInstance(project).getDocument(hostFile));
            long modificationStamp = document.getModificationStamp();
            Application.get().invokeLater(() -> {
                if (document.getModificationStamp() == modificationStamp) {
                    refactoringProvider.getAvailabilityIndicator().clear();
                }
            });
            return null;
        }

        SuggestedRefactoringData refactoringData = refactoringSupport.getAvailability().detectAvailableRefactoring(state);

        // update availability indicator with more precise state that takes into account resolve
        refactoringProvider.getAvailabilityIndicator().update(anchor, refactoringData, refactoringSupport);

        if (refactoringData != null) {
            hostFile.putUserData(REFACTORING_DATA_KEY, refactoringData);
        }

        TextRange range;
        if (refactoringData instanceof SuggestedRenameData renameData) {
            range = Objects.requireNonNull(refactoringSupport.nameRange(renameData.getDeclaration()));
        }
        else if (refactoringData instanceof SuggestedChangeSignatureData) {
            range = Objects.requireNonNull(SuggestedRefactoringAvailabilityIndicator.changeSignatureAvailabilityRange(refactoringSupport, anchor));
        }
        else {
            return null;
        }

        if (!range.containsOffset(offset)) {
            return null;
        }

        LocalizeValue text = refactoringData.getIntentionText();

        return new MyIntention(text, refactoringData instanceof SuggestedChangeSignatureData);
    }

    private static class MyIntention implements SyntheticIntentionAction, PriorityAction {
        private final LocalizeValue myText;
        private final boolean myShowReviewBalloon;

        MyIntention(LocalizeValue text, boolean showReviewBalloon) {
            myText = text;
            myShowReviewBalloon = showReviewBalloon;
        }

        @Override
        public Priority getPriority() {
            return Priority.HIGH;
        }

        @Override
        public LocalizeValue getText() {
            return myText;
        }

        @Override
        public boolean isAvailable(Project project, Editor editor, PsiFile psiFile) {
            return true;
        }

        @Override
        public boolean startInWriteAction() {
            return false;
        }

        @Override
        @RequiredUIAccess
        public void invoke(Project project, Editor editor, PsiFile psiFile) {
            PerformSuggestedRefactoring.performSuggestedRefactoring(project, editor, null, null, myShowReviewBalloon, ActionPlaces.INTENTION_MENU);
        }
    }
}
