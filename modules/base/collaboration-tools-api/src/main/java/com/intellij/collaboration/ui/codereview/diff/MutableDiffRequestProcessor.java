// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.collaboration.ui.codereview.diff;

import consulo.application.util.ListSelection;
import consulo.diff.request.DiffRequest;
import consulo.project.Project;
import consulo.proxy.EventDispatcher;
import consulo.ui.ex.action.AnAction;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.util.EventListener;

/**
 * A {@link DiffRequestProcessor} whose state is controlled externally.
 */
final class MutableDiffRequestProcessor extends DiffRequestProcessor {
    private final @Nonnull EventDispatcher<UpdateSignalListener> updateSignalDispatcher =
        EventDispatcher.create(UpdateSignalListener.class);

    // after setting the navigator, the viewer should be re-created
    private @Nonnull Navigator<?> navigator = Navigator.empty();

    MutableDiffRequestProcessor(@Nonnull Project project) {
        super(project);
    }

    @Nonnull
    Navigator<?> getNavigator() {
        return navigator;
    }

    void setNavigator(@Nonnull Navigator<?> navigator) {
        this.navigator = navigator;
    }

    void applyRequest(@Nonnull DiffRequest request) {
        super.doApplyRequest(request);
    }

    @Override
    protected void reloadRequest() {
        updateSignalDispatcher.getMulticaster().onReloadRequested();
    }

    @Override
    protected void updateRequest(boolean force, @Nullable ScrollToPolicy scrollToChangePolicy) {
        updateSignalDispatcher.getMulticaster().onUpdateRequested(force, scrollToChangePolicy);
    }

    void addUpdateSignalListener(@Nonnull UpdateSignalListener listener) {
        updateSignalDispatcher.addListener(listener);
    }

    void removeUpdateSignalListener(@Nonnull UpdateSignalListener listener) {
        updateSignalDispatcher.removeListener(listener);
    }

    @SuppressWarnings("rawtypes")
    private @Nonnull ListSelection getCurrentList() {
        return navigator.getCurrentList();
    }

    @Override
    protected boolean isNavigationEnabled() {
        return !getCurrentList().getList().isEmpty();
    }

    @Override
    protected boolean hasPrevChange(boolean fromUpdate) {
        return getCurrentList().getSelectedIndex() > 0;
    }

    @Override
    protected boolean hasNextChange(boolean fromUpdate) {
        ListSelection<?> list = getCurrentList();
        return list.getSelectedIndex() < list.getList().size() - 1;
    }

    @Override
    protected void goToNextChange(boolean fromDifferences) {
        navigator.selectNext(fromDifferences);
    }

    @Override
    protected void goToPrevChange(boolean fromDifferences) {
        navigator.selectPrev(fromDifferences);
    }

    @Override
    protected @Nonnull AnAction createGoToChangeAction() {
        return getPopupAction(navigator);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <C> @Nonnull AnAction getPopupAction(@Nonnull Navigator<C> nav) {
        return new PresentableGoToChangePopupAction<C>() {
            @Override
            protected @Nonnull ListSelection<? extends C> getChanges() {
                ListSelection<C> list = nav.getCurrentList();
                return ListSelection.createAt(list.getList(), list.getSelectedIndex());
            }

            @Override
            protected void onSelected(@Nonnull C change) {
                nav.select(change);
            }

            @Override
            protected @Nullable PresentableChange getPresentation(@Nonnull C change) {
                return nav.getChangePresentation(change);
            }
        };
    }

    interface UpdateSignalListener extends EventListener {
        default void onReloadRequested() {
        }

        default void onUpdateRequested(boolean force, @Nullable ScrollToPolicy scrollToChangePolicy) {
        }
    }

    interface Navigator<C> {
        @Nonnull
        ListSelection<C> getCurrentList();

        void selectNext(boolean fromDifferences);

        void selectPrev(boolean fromDifferences);

        void select(@Nonnull C change);

        @Nullable
        PresentableChange getChangePresentation(@Nonnull C change);

        @SuppressWarnings("unchecked")
        static <C> @Nonnull Navigator<C> empty() {
            return (Navigator<C>) EmptyNavigator.INSTANCE;
        }
    }

    private enum EmptyNavigator implements Navigator<Object> {
        INSTANCE;

        @Override
        public @Nonnull ListSelection<Object> getCurrentList() {
            return ListSelection.empty();
        }

        @Override
        public void selectNext(boolean fromDifferences) {
        }

        @Override
        public void selectPrev(boolean fromDifferences) {
        }

        @Override
        public void select(@Nonnull Object change) {
        }

        @Override
        public @Nullable PresentableChange getChangePresentation(@Nonnull Object change) {
            return null;
        }
    }
}
