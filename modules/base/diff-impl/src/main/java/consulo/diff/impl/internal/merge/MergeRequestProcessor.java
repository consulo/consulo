/*
 * Copyright 2000-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package consulo.diff.impl.internal.merge;

import consulo.application.Application;
import consulo.application.HelpManager;
import consulo.component.extension.ExtensionPoint;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.diff.DiffDataKeys;
import consulo.diff.DiffPlaces;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.PrevNextDifferenceIterable;
import consulo.diff.impl.internal.action.NextDifferenceAction;
import consulo.diff.impl.internal.action.PrevDifferenceAction;
import consulo.diff.impl.internal.util.UnifiedDiffUtil;
import consulo.diff.internal.DiffImplUtil;
import consulo.diff.internal.DiffManagerEx;
import consulo.diff.merge.MergeContext;
import consulo.diff.merge.MergeRequest;
import consulo.diff.merge.MergeResult;
import consulo.diff.merge.MergeTool;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.Button;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.WrappedLayout;
import consulo.util.dataholder.Key;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

// TODO: support merge request chains
// idea - to keep in memory all viewers that were modified (so binary conflict is not the case and OOM shouldn't be too often)
// suspend() / resume() methods for viewers? To not interfere with MergeRequest lifecycle: single request -> single viewer -> single applyResult()
public abstract class MergeRequestProcessor implements Disposable, UiDataProvider {
    private static final Logger LOG = Logger.getInstance(MergeRequestProcessor.class);

    private boolean myDisposed;

    private final @Nullable Project myProject;
    private final MergeContext myContext;

    private final List<MergeTool> myAvailableTools;

    private final DockLayout myPanel;
    private final WrappedLayout myContentPanel;
    private final WrappedLayout myToolbarPanel;
    private final WrappedLayout myToolbarStatusPanel;
    private final WrappedLayout myBottomPanel;

    private final MergeRequest myRequest;

    private MergeTool.MergeViewer myViewer;
    private @Nullable ActionToolbar myToolbar;
    private final List<AnAction> myRegisteredActions = new ArrayList<>();
    private @Nullable Supplier<CompletableFuture<Boolean>> myCloseHandler;
    private @Nullable BottomActions myBottomActions;
    private boolean myConflictResolved = false;

    @RequiredUIAccess
    public MergeRequestProcessor(@Nullable Project project, MergeRequest request) {
        myProject = project;
        myRequest = request;

        myContext = new MyDiffContext();
        myContext.putUserData(DiffUserDataKeys.PLACE, DiffPlaces.MERGE);

        myAvailableTools = DiffManagerEx.getInstanceEx().getMergeTools();

        myContentPanel = WrappedLayout.create();
        myToolbarPanel = WrappedLayout.create();
        myToolbarStatusPanel = WrappedLayout.create();
        myBottomPanel = WrappedLayout.create();

        DockLayout topPanel = DockLayout.create(Space.NONE);
        topPanel.center(myToolbarPanel);
        topPanel.right(myToolbarStatusPanel);

        myPanel = DockLayout.create(Space.NONE);
        myPanel.top(topPanel);
        myPanel.center(myContentPanel);
        myPanel.bottom(myBottomPanel);
        myPanel.putUserData(UiDataProvider.KEY, this);

        MergeTool.MergeViewer viewer;
        try {
            viewer = getFittedTool().createComponent(myContext, myRequest);
        }
        catch (Throwable e) {
            LOG.error(e);
            viewer = ErrorMergeTool.INSTANCE.createComponent(myContext, myRequest);
        }

        myViewer = viewer;
        updateBottomActions();
        rebuildBottomPanel();
    }

    //
    // Update
    //

    @RequiredUIAccess
    public void init() {
        setTitle(myRequest.getTitle());
        initViewer();
    }

    @RequiredUIAccess
    private void initViewer() {
        myContentPanel.set(myViewer.getComponent());

        MergeTool.ToolbarComponents toolbarComponents = myViewer.init();

        buildToolbar(toolbarComponents.toolbarActions);
        myToolbarStatusPanel.set(toolbarComponents.statusPanel);
        myCloseHandler = toolbarComponents.closeHandler;
    }

    @RequiredUIAccess
    private void destroyViewer() {
        Disposer.dispose(myViewer);

        for (AnAction action : myRegisteredActions) {
            action.unregisterCustomShortcutSet(myPanel);
        }
        myRegisteredActions.clear();

        myContentPanel.set((Component) null);
        myToolbarPanel.set((Component) null);
        myToolbarStatusPanel.set((Component) null);
        myToolbar = null;
        myCloseHandler = null;
        myBottomActions = null;
    }

    private void updateBottomActions() {
        myBottomActions = new BottomActions();
        myBottomActions.applyLeft = myViewer.getResolveAction(MergeResult.LEFT);
        myBottomActions.applyRight = myViewer.getResolveAction(MergeResult.RIGHT);
        myBottomActions.resolveAction = myViewer.getResolveAction(MergeResult.RESOLVED);
        myBottomActions.cancelAction = myViewer.getResolveAction(MergeResult.CANCEL);
    }

    @RequiredUIAccess
    protected void rebuildBottomPanel() {
        BottomActions bottomActions = getBottomActions();

        HorizontalLayout left = HorizontalLayout.create(Space.SMALL);
        addButton(left, bottomActions.applyLeft);
        addButton(left, bottomActions.applyRight);

        HorizontalLayout right = HorizontalLayout.create(Space.SMALL);
        addButton(right, bottomActions.resolveAction);
        addButton(right, bottomActions.cancelAction);

        DockLayout bottom = DockLayout.create(Space.NONE);
        bottom.left(left);
        bottom.right(right);
        bottom.paddingBuilder().allSet(Space.SMALL).apply();
        myBottomPanel.set(bottom);
    }

    @RequiredUIAccess
    private static void addButton(HorizontalLayout layout, MergeTool.@Nullable ActionRecord actionRecord) {
        if (actionRecord == null) {
            return;
        }
        Button button = Button.create(actionRecord.title(), event -> actionRecord.onActionPerformed().run());
        layout.add(button);
    }

    protected DefaultActionGroup collectToolbarActions(@Nullable List<AnAction> viewerActions) {
        DefaultActionGroup group = new DefaultActionGroup();

        List<AnAction> navigationActions = Arrays.asList(new MyPrevDifferenceAction(), new MyNextDifferenceAction());
        DiffImplUtil.addActionBlock(group, navigationActions);

        DiffImplUtil.addActionBlock(group, viewerActions);

        List<AnAction> requestContextActions = myRequest.getUserData(DiffUserDataKeys.CONTEXT_ACTIONS);
        DiffImplUtil.addActionBlock(group, requestContextActions);

        List<AnAction> contextActions = myContext.getUserData(DiffUserDataKeys.CONTEXT_ACTIONS);
        DiffImplUtil.addActionBlock(group, contextActions);

        return group;
    }

    @RequiredUIAccess
    protected void buildToolbar(@Nullable List<AnAction> viewerActions) {
        ActionGroup group = collectToolbarActions(viewerActions);
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.DIFF_TOOLBAR, group, true);

        toolbar.setTargetUIComponent(myViewer.getComponent());

        myToolbar = toolbar;
        myToolbarPanel.set(toolbar.getUIComponent());
        for (AnAction action : group.getChildren(null)) {
            action.registerCustomShortcutSet(action.getShortcutSet(), myPanel);
            myRegisteredActions.add(action);
        }
    }

    private MergeTool getFittedTool() {
        ExtensionPoint<MergeTool> point = Application.get().getExtensionPoint(MergeTool.class);
        return Objects.requireNonNull(point.findFirstSafe(t -> t.canShow(myContext, myRequest)), "Missed error impl");
    }

    private void setTitle(@Nullable String title) {
        setWindowTitle(LocalizeValue.of(title == null ? "Merge" : title));
    }

    @Override
    public void dispose() {
        if (myDisposed) {
            return;
        }
        Runnable disposeTask = () -> {
            if (myDisposed) {
                return;
            }
            myDisposed = true;

            onDispose();

            destroyViewer();
        };
        if (UIAccess.isUIThread()) {
            disposeTask.run();
        }
        else {
            Application.get().invokeLater(disposeTask);
        }
    }

    @RequiredUIAccess
    private void applyRequestResult(MergeResult result) {
        if (myConflictResolved) {
            return;
        }
        myConflictResolved = true;
        try {
            myRequest.applyResult(result);
        }
        catch (Exception e) {
            LOG.error(e);
        }
    }

    @RequiredUIAccess
    private void reopenWithTool(MergeTool tool) {
        if (myConflictResolved) {
            LOG.warn("Can't reopen with " + tool + " - conflict already resolved");
            return;
        }

        if (!tool.canShow(myContext, myRequest)) {
            LOG.warn("Can't reopen with " + tool + " - " + myRequest);
            return;
        }

        MergeTool.MergeViewer newViewer;
        try {
            newViewer = tool.createComponent(myContext, myRequest);
        }
        catch (Throwable e) {
            LOG.error(e);
            return;
        }

        boolean wasFocused = isFocused();

        destroyViewer();
        myViewer = newViewer;
        updateBottomActions();
        rebuildBottomPanel();
        initViewer();

        if (wasFocused) {
            requestFocusInternal();
        }
    }

    //
    // Abstract
    //

    @RequiredUIAccess
    protected void onDispose() {
        applyRequestResult(MergeResult.CANCEL);
    }

    protected abstract void setWindowTitle(LocalizeValue title);

    @RequiredUIAccess
    public abstract void closeDialog();

    public @Nullable <T> T getContextUserData(Key<T> key) {
        return myContext.getUserData(key);
    }

    public <T> void putContextUserData(Key<T> key, @Nullable T value) {
        myContext.putUserData(key, value);
    }

    //
    // Getters
    //

    public Component getComponent() {
        return myPanel;
    }

    public @Nullable Component getPreferredFocusedComponent() {
        Component component = myViewer.getPreferredFocusedComponent();
        if (component != null) {
            return component;
        }
        return myToolbar != null ? myToolbar.getUIComponent() : null;
    }

    public @Nullable Project getProject() {
        return myProject;
    }

    public MergeContext getContext() {
        return myContext;
    }

    @RequiredUIAccess
    public CompletableFuture<Boolean> checkCloseAction() {
        Supplier<CompletableFuture<Boolean>> closeHandler = myCloseHandler;
        if (myConflictResolved || closeHandler == null) {
            return CompletableFuture.completedFuture(Boolean.TRUE);
        }
        return closeHandler.get();
    }

    public BottomActions getBottomActions() {
        return myBottomActions != null ? myBottomActions : new BottomActions();
    }

    public @Nullable String getHelpId() {
        String helpId = myRequest.getUserData(DiffUserDataKeys.HELP_ID);
        return helpId != null ? helpId : "procedures.vcWithIDEA.commonVcsOps.integrateDiffs.resolveConflict";
    }

    //
    // Misc
    //

    @RequiredUIAccess
    public boolean isFocused() {
        return UnifiedDiffUtil.isFocusedComponent(myPanel);
    }

    @RequiredUIAccess
    public void requestFocus() {
        UnifiedDiffUtil.requestFocus(getPreferredFocusedComponent());
    }

    @RequiredUIAccess
    protected void requestFocusInternal() {
        UnifiedDiffUtil.requestFocus(getPreferredFocusedComponent());
    }

    //
    // Navigation
    //

    private static class MyNextDifferenceAction extends NextDifferenceAction {
        @Override
        public void update(AnActionEvent e) {
            if (!ActionPlaces.DIFF_TOOLBAR.equals(e.getPlace())) {
                e.getPresentation().setEnabled(true);
                return;
            }

            PrevNextDifferenceIterable iterable = e.getData(DiffDataKeys.PREV_NEXT_DIFFERENCE_ITERABLE);
            if (iterable != null && iterable.canGoNext()) {
                e.getPresentation().setEnabled(true);
                return;
            }

            e.getPresentation().setEnabled(false);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            PrevNextDifferenceIterable iterable = e.getData(DiffDataKeys.PREV_NEXT_DIFFERENCE_ITERABLE);
            if (iterable != null && iterable.canGoNext()) {
                iterable.goNext();
            }
        }
    }

    private static class MyPrevDifferenceAction extends PrevDifferenceAction {
        @Override
        public void update(AnActionEvent e) {
            if (!ActionPlaces.DIFF_TOOLBAR.equals(e.getPlace())) {
                e.getPresentation().setEnabled(true);
                return;
            }

            PrevNextDifferenceIterable iterable = e.getData(DiffDataKeys.PREV_NEXT_DIFFERENCE_ITERABLE);
            if (iterable != null && iterable.canGoPrev()) {
                e.getPresentation().setEnabled(true);
                return;
            }

            e.getPresentation().setEnabled(false);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            PrevNextDifferenceIterable iterable = e.getData(DiffDataKeys.PREV_NEXT_DIFFERENCE_ITERABLE);
            if (iterable != null && iterable.canGoPrev()) {
                iterable.goPrev();
            }
        }
    }

    //
    // Helpers
    //

    @Override
    public void uiDataSnapshot(DataSink sink) {
        sink.set(Project.KEY, myProject);
        sink.lazy(HelpManager.HELP_ID, () -> {
            if (myRequest.getUserData(DiffUserDataKeys.HELP_ID) != null) {
                return myRequest.getUserData(DiffUserDataKeys.HELP_ID);
            }
            else {
                return "procedures.vcWithIDEA.commonVcsOps.integrateDiffs.resolveConflict";
            }
        });

        UiDataProvider requestProvider = myRequest.getUserData(DiffUserDataKeys.DATA_PROVIDER);
        if (requestProvider != null) {
            sink.uiDataSnapshot(requestProvider);
        }

        UiDataProvider contextProvider = myContext.getUserData(DiffUserDataKeys.DATA_PROVIDER);
        if (contextProvider != null) {
            sink.uiDataSnapshot(contextProvider);
        }
    }

    private class MyDiffContext extends MergeContextEx {
        @Override
        public @Nullable Project getProject() {
            return MergeRequestProcessor.this.getProject();
        }

        @Override
        @RequiredUIAccess
        public boolean isFocused() {
            return MergeRequestProcessor.this.isFocused();
        }

        @Override
        @RequiredUIAccess
        public void requestFocus() {
            MergeRequestProcessor.this.requestFocusInternal();
        }

        @Override
        @RequiredUIAccess
        public void finishMerge(MergeResult result) {
            applyRequestResult(result);
            MergeRequestProcessor.this.closeDialog();
        }

        @Override
        @RequiredUIAccess
        public void reopenWithTool(MergeTool tool) {
            MergeRequestProcessor.this.reopenWithTool(tool);
        }
    }

    public static class BottomActions {
        public MergeTool.@Nullable ActionRecord applyLeft;
        public MergeTool.@Nullable ActionRecord applyRight;
        public MergeTool.@Nullable ActionRecord resolveAction;
        public MergeTool.@Nullable ActionRecord cancelAction;
    }
}
