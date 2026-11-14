// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.ide.impl.idea.refactoring.suggested;

import consulo.annotation.access.RequiredReadAction;
import consulo.annotation.component.ServiceImpl;
import consulo.disposer.Disposable;
import consulo.document.util.TextRange;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringProvider;
import consulo.language.editor.refactoring.suggested.SuggestedRefactoringState;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.jspecify.annotations.Nullable;

@Singleton
@ServiceImpl
public class SuggestedRefactoringProviderImpl implements SuggestedRefactoringProvider, Disposable {
    public static SuggestedRefactoringProviderImpl getInstance(Project project) {
        return (SuggestedRefactoringProviderImpl) SuggestedRefactoringProvider.getInstance(project);
    }

    private final SuggestedRefactoringAvailabilityIndicator myAvailabilityIndicator;
    private final SuggestedRefactoringChangeCollector myChangeCollector;
    private final SuggestedRefactoringChangeListener myListener;

    @Inject
    public SuggestedRefactoringProviderImpl(Project project) {
        myAvailabilityIndicator = new SuggestedRefactoringAvailabilityIndicator(project);
        myChangeCollector = new SuggestedRefactoringChangeCollector(myAvailabilityIndicator);
        myListener = new SuggestedRefactoringChangeListener(project, myChangeCollector, this);
    }

    public SuggestedRefactoringAvailabilityIndicator getAvailabilityIndicator() {
        return myAvailabilityIndicator;
    }

    public @Nullable SuggestedRefactoringState getState() {
        return myChangeCollector.getState();
    }

    @Override
    public void reset() {
        // we must also reset new identifiers otherwise declaration is considered new after inplace-rename
        // see https://youtrack.jetbrains.com/issue/IDEA-233185
        myListener.reset(true);
    }

    @RequiredUIAccess
    @RequiredReadAction
    public void undoToState(SuggestedRefactoringState state, TextRange signatureRange) {
        myListener.undoToState(state, signatureRange);
        myChangeCollector.undoToState(state);
    }

    public void suppressForCurrentDeclaration() {
        myListener.suppressForCurrentDeclaration();
    }

    public boolean isAmendStateInBackgroundEnabled() {
        return myChangeCollector.isAmendStateInBackgroundEnabled();
    }

    public void setAmendStateInBackgroundEnabled(boolean value) {
        myChangeCollector.setAmendStateInBackgroundEnabled(value);
    }

    @Override
    public void dispose() {
    }
}
