// Copyright 2000-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.refactoring.suggested;

import consulo.annotation.access.RequiredReadAction;
import consulo.application.Application;
import consulo.application.ReadAction;
import consulo.document.Document;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringData;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringProvider;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringState;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringState.ErrorLevel;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringSupport;
import consulo.language.psi.PsiDocumentManager;
import consulo.language.psi.PsiElement;
import consulo.language.psi.PsiFile;
import consulo.project.Project;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.undoRedo.BasicUndoableAction;
import consulo.undoRedo.ProjectUndoManager;
import org.jspecify.annotations.Nullable;

import java.util.Iterator;
import java.util.Objects;

public class SuggestedRefactoringChangeCollector implements SuggestedRefactoringSignatureWatcher {
    private final SuggestedRefactoringAvailabilityIndicator myAvailabilityIndicator;

    private volatile @Nullable SuggestedRefactoringState myState;

    private boolean myAmendStateInBackgroundEnabled = !Application.get().isUnitTestMode();

    public SuggestedRefactoringChangeCollector(SuggestedRefactoringAvailabilityIndicator availabilityIndicator) {
        myAvailabilityIndicator = availabilityIndicator;
    }

    public @Nullable SuggestedRefactoringState getState() {
        return myState;
    }

    @Override
    @RequiredUIAccess
    @RequiredReadAction
    public void editingStarted(PsiElement declaration, SuggestedRefactoringSupport refactoringSupport) {
        UIAccess.assertIsUIThread();
        if (!declaration.isValid()) {
            throw new IllegalArgumentException("Declaration is not valid");
        }
        myState = refactoringSupport.getStateChanges().createInitialState(declaration);
        updateAvailabilityIndicator();
        amendStateInBackground();

        Project project = declaration.getProject();
        Document document = Objects.requireNonNull(PsiDocumentManager.getInstance(project).getDocument(declaration.getContainingFile()));
        // add UndoableAction which will reset signature tracking state to initial
        ProjectUndoManager.getInstance(project).undoableActionPerformed(new EditingStartedUndoableAction(document, project));
    }

    @Override
    @RequiredUIAccess
    @RequiredReadAction
    public void nextSignature(PsiElement anchor, SuggestedRefactoringSupport refactoringSupport) {
        UIAccess.assertIsUIThread();
        if (!anchor.isValid()) {
            throw new IllegalArgumentException("Anchor is not valid");
        }
        SuggestedRefactoringState state = myState;
        myState = state != null ? refactoringSupport.getStateChanges().updateState(state, anchor) : null;
        updateAvailabilityIndicator();
        amendStateInBackground();
    }

    @Override
    @RequiredUIAccess
    public void inconsistentState() {
        UIAccess.assertIsUIThread();
        SuggestedRefactoringState state = myState;
        myState = state != null ? state.withErrorLevel(ErrorLevel.INCONSISTENT) : null;
        updateAvailabilityIndicator();
    }

    @Override
    @RequiredUIAccess
    public void reset() {
        UIAccess.assertIsUIThread();
        myState = null;
        updateAvailabilityIndicator();
    }

    @RequiredUIAccess
    public void undoToState(SuggestedRefactoringState state) {
        UIAccess.assertIsUIThread();
        myState = state;
        updateAvailabilityIndicator();
        amendStateInBackground();
    }

    private void amendStateInBackground() {
        if (!myAmendStateInBackgroundEnabled) {
            return;
        }
        SuggestedRefactoringState state = myState;
        if (state == null) {
            return;
        }
        Object stateLock = new Object();
        if (state.getErrorLevel() == ErrorLevel.INCONSISTENT) {
            throw new IllegalArgumentException("State must not be inconsistent");
        }
        PsiElement anchor = state.getAnchor();
        if (!anchor.isValid()) {
            return;
        }
        PsiFile psiFile = anchor.getContainingFile();
        Project project = psiFile.getProject();
        PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
        Document document = Objects.requireNonNull(psiDocumentManager.getDocument(psiFile));

        SuggestedRefactoringState[] initialState = {state};

        ReadAction.nonBlocking(() -> {
                Iterator<SuggestedRefactoringState> states =
                    initialState[0].getRefactoringSupport().getAvailability().amendStateInBackground(initialState[0]);
                while (states.hasNext()) {
                    SuggestedRefactoringState newState = states.next();
                    Application.get().invokeLater(() -> {
                        synchronized (stateLock) {
                            if (myState != initialState[0]) {
                                return;
                            }
                            myState = newState;
                            initialState[0] = newState; // update initialState so that condition of expireWhen remains false
                        }
                        if (psiDocumentManager.isCommitted(document)) { // we can't update availability indicator if document is not committed
                            updateAvailabilityIndicator();
                        }
                    });
                }
                return null;
            })
            .coalesceBy(this)
            .inSmartMode(project)
            .expireWhen(() -> {
                synchronized (stateLock) {
                    return myState != initialState[0];
                }
            })
            .expireWith(project)
            .submitDefault();
    }

    @RequiredUIAccess
    @RequiredReadAction
    private void updateAvailabilityIndicator() {
        SuggestedRefactoringState state = myState;
        SuggestedRefactoringAvailabilityListener publisher =
            myAvailabilityIndicator.getProject().getMessageBus().syncPublisher(SuggestedRefactoringAvailabilityListener.class);
        if (state == null || state.getOldSignature().equals(state.getNewSignature()) && state.getErrorLevel() == ErrorLevel.NO_ERRORS) {
            myAvailabilityIndicator.clear();
            publisher.cleared();
            return;
        }

        SuggestedRefactoringSupport refactoringSupport = state.getRefactoringSupport();
        if (!state.getAnchor().isValid() || refactoringSupport.nameRange(state.getAnchor()) == null) {
            myAvailabilityIndicator.disable();
            publisher.disabled();
            return;
        }

        SuggestedRefactoringData refactoringData = state.getErrorLevel() == ErrorLevel.NO_ERRORS
            ? refactoringSupport.getAvailability().detectAvailableRefactoring(state)
            : null;
        myAvailabilityIndicator.update(state.getAnchor(), refactoringData, refactoringSupport);
        publisher.updated(state);
    }

    public boolean isAmendStateInBackgroundEnabled() {
        return myAmendStateInBackgroundEnabled;
    }

    public void setAmendStateInBackgroundEnabled(boolean value) {
        if (value != myAmendStateInBackgroundEnabled) {
            myAmendStateInBackgroundEnabled = value;
            SuggestedRefactoringState state = myState;
            if (value && (state == null || state.getErrorLevel() != ErrorLevel.INCONSISTENT)) {
                amendStateInBackground();
            }
        }
    }

    private static class EditingStartedUndoableAction extends BasicUndoableAction {
        private final Project myProject;

        EditingStartedUndoableAction(Document document, Project project) {
            super(document);
            myProject = project;
        }

        @Override
        public void undo() {
            SuggestedRefactoringProvider.getInstance(myProject).reset();
        }

        @Override
        public void redo() {
        }
    }
}
