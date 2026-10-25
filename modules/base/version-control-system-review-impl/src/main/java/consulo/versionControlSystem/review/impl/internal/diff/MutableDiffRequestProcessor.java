// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.versionControlSystem.review.impl.internal.diff;

import consulo.application.util.ListSelection;
import consulo.diff.impl.internal.DiffRequestProcessor;
import consulo.diff.internal.DiffUserDataKeysEx.ScrollToPolicy;
import consulo.diff.request.DiffRequest;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.versionControlSystem.change.PresentableChange;
import consulo.versionControlSystem.impl.internal.diff.MutableDiffRequestProcessorNavigator;
import consulo.versionControlSystem.impl.internal.diff.PresentableGoToChangePopupAction;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A {@link DiffRequestProcessor} whose state is controlled externally
 */
public class MutableDiffRequestProcessor extends DiffRequestProcessor {
    private final List<MutableDiffRequestProcessorUpdateListener> myUpdateSignalListeners = new CopyOnWriteArrayList<>();

    // after setting the navigator, the viewer should be re-created
    private MutableDiffRequestProcessorNavigator<?> myNavigator = MutableDiffRequestProcessorNavigator.empty();

    public MutableDiffRequestProcessor(@Nullable Project project) {
        super(project);
    }

    public MutableDiffRequestProcessorNavigator<?> getNavigator() {
        return myNavigator;
    }

    public void setNavigator(MutableDiffRequestProcessorNavigator<?> navigator) {
        myNavigator = navigator;
    }

    @RequiredUIAccess
    public void applyRequest(DiffRequest request) {
        applyRequest(request, false, null);
    }

    @RequiredUIAccess
    public void applyRequest(DiffRequest request, boolean force) {
        applyRequest(request, force, null);
    }

    @Override
    @RequiredUIAccess
    protected void reloadRequest() {
        for (MutableDiffRequestProcessorUpdateListener listener : myUpdateSignalListeners) {
            listener.onReloadRequested();
        }
    }

    @Override
    @RequiredUIAccess
    public void updateRequest(boolean force, @Nullable ScrollToPolicy scrollToChangePolicy) {
        for (MutableDiffRequestProcessorUpdateListener listener : myUpdateSignalListeners) {
            listener.onUpdateRequested(force, scrollToChangePolicy);
        }
    }

    public void addUpdateSignalListener(Disposable parentDisposable, MutableDiffRequestProcessorUpdateListener listener) {
        myUpdateSignalListeners.add(listener);
        Disposer.register(parentDisposable, () -> myUpdateSignalListeners.remove(listener));
    }

    @Override
    protected boolean isNavigationEnabled() {
        return !myNavigator.getCurrentList().getList().isEmpty();
    }

    @Override
    protected boolean hasPrevChange() {
        return myNavigator.getCurrentList().getSelectedIndex() > 0;
    }

    @Override
    protected boolean hasNextChange() {
        return myNavigator.getCurrentList().getSelectedIndex() < myNavigator.getCurrentList().getList().size() - 1;
    }

    @Override
    protected void goToNextChange(boolean fromDifferences) {
        myNavigator.selectNext(fromDifferences);
    }

    @Override
    protected void goToPrevChange(boolean fromDifferences) {
        myNavigator.selectPrev(fromDifferences);
    }

    @Override
    protected List<AnAction> getNavigationActions() {
        List<AnAction> actions = new ArrayList<>(super.getNavigationActions());
        actions.add(createGoToChangeAction());
        return actions;
    }

    private AnAction createGoToChangeAction() {
        return new PresentableGoToChangePopupAction<>(new MutableDiffRequestProcessorNavigator<Object>() {
            @SuppressWarnings("unchecked")
            private MutableDiffRequestProcessorNavigator<Object> current() {
                return (MutableDiffRequestProcessorNavigator<Object>) myNavigator;
            }

            @Override
            public ListSelection<Object> getCurrentList() {
                return current().getCurrentList();
            }

            @Override
            public void selectNext(boolean fromDifferences) {
                current().selectNext(fromDifferences);
            }

            @Override
            public void selectPrev(boolean fromDifferences) {
                current().selectPrev(fromDifferences);
            }

            @Override
            public void select(Object change) {
                current().select(change);
            }

            @Override
            public @Nullable PresentableChange getChangePresentation(Object change) {
                return current().getChangePresentation(change);
            }
        });
    }
}
