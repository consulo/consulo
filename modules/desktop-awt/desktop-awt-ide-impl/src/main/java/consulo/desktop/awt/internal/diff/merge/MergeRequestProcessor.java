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
package consulo.desktop.awt.internal.diff.merge;

import consulo.annotation.DeprecationInfo;
import consulo.application.Application;
import consulo.application.HelpManager;
import consulo.component.extension.ExtensionPoint;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.desktop.awt.internal.diff.util.AWTDiffUtil;
import consulo.diff.DiffDataKeys;
import consulo.diff.DiffPlaces;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.PrevNextDifferenceIterable;
import consulo.diff.impl.internal.action.NextDifferenceAction;
import consulo.diff.impl.internal.action.PrevDifferenceAction;
import consulo.diff.impl.internal.util.DiffImplUtil;
import consulo.diff.internal.DiffManagerEx;
import consulo.diff.merge.MergeContext;
import consulo.diff.merge.MergeRequest;
import consulo.diff.merge.MergeResult;
import consulo.diff.merge.MergeTool;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.dataContext.BaseDataManager;
import consulo.ide.impl.idea.openapi.actionSystem.ex.ActionImplUtil;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.IdeFocusTraversalPolicy;
import consulo.ui.ex.awt.JBUI;
import consulo.ui.ex.awt.UIUtil;
import consulo.ui.ex.awt.Wrapper;
import consulo.ui.ex.awt.LocalizeAction;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.BooleanSupplier;

// TODO: support merge request chains
// idea - to keep in memory all viewers that were modified (so binary conflict is not the case and OOM shouldn't be too often)
// suspend() / resume() methods for viewers? To not interfere with MergeRequest lifecycle: single request -> single viewer -> single applyResult()
public abstract class MergeRequestProcessor implements Disposable {
    private static final Logger LOG = Logger.getInstance(MergeRequestProcessor.class);

    private boolean myDisposed;

    @Nullable
    private final Project myProject;
    @Nonnull
    private final MergeContext myContext;

    @Nonnull
    private final List<MergeTool> myAvailableTools;

    @Nonnull
    private final JPanel myPanel;
    @Nonnull
    private final MyPanel myMainPanel;
    @Nonnull
    private final Wrapper myContentPanel;
    @Nonnull
    private final Wrapper myToolbarPanel;
    @Nonnull
    private final Wrapper myToolbarStatusPanel;

    @Nonnull
    private final MergeRequest myRequest;

    @Nonnull
    private MergeTool.MergeViewer myViewer;
    @Nullable
    private BooleanSupplier myCloseHandler;
    @Nullable
    private BottomActions myBottomActions;
    private boolean myConflictResolved = false;

    @RequiredUIAccess
    public MergeRequestProcessor(@Nullable Project project, @Nonnull MergeRequest request) {
        myProject = project;
        myRequest = request;

        myContext = new MyDiffContext();
        myContext.putUserData(DiffUserDataKeys.PLACE, DiffPlaces.MERGE);

        myAvailableTools = DiffManagerEx.getInstanceEx().getMergeTools();

        myMainPanel = new MyPanel();
        myContentPanel = new Wrapper();
        myToolbarPanel = new Wrapper();
        myToolbarPanel.setFocusable(true);
        myToolbarStatusPanel = new Wrapper();

        myPanel = JBUI.Panels.simplePanel(myMainPanel);

        JPanel topPanel = JBUI.Panels.simplePanel(myToolbarPanel).addToRight(myToolbarStatusPanel);

        myMainPanel.add(topPanel, BorderLayout.NORTH);
        myMainPanel.add(myContentPanel, BorderLayout.CENTER);

        myMainPanel.setFocusTraversalPolicyProvider(true);
        myMainPanel.setFocusTraversalPolicy(new MyFocusTraversalPolicy());

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
        myContentPanel.setContent(myViewer.getComponent());

        MergeTool.ToolbarComponents toolbarComponents = myViewer.init();

        buildToolbar(toolbarComponents.toolbarActions);
        myToolbarStatusPanel.setContent(toolbarComponents.statusPanel);
        myCloseHandler = toolbarComponents.closeHandler;
    }

    @RequiredUIAccess
    private void destroyViewer() {
        Disposer.dispose(myViewer);

        ActionImplUtil.clearActions(myMainPanel);

        myContentPanel.setContent(null);
        myToolbarPanel.setContent(null);
        myToolbarStatusPanel.setContent(null);
        myCloseHandler = null;
        myBottomActions = null;
    }

    private void updateBottomActions() {
        myBottomActions = new BottomActions();
        myBottomActions.applyLeft = createAction(myViewer.getResolveAction(MergeResult.LEFT));
        myBottomActions.applyRight = createAction(myViewer.getResolveAction(MergeResult.RIGHT));
        myBottomActions.resolveAction = createAction(myViewer.getResolveAction(MergeResult.RESOLVED));
        myBottomActions.cancelAction = createAction(myViewer.getResolveAction(MergeResult.CANCEL));
    }

    private LocalizeAction createAction(MergeTool.ActionRecord actionRecord) {
        if (actionRecord == null) {
            return null;
        }

        return new LocalizeAction(actionRecord.title()) {
            @Override
            public void actionPerformed(ActionEvent e) {
                actionRecord.onActionPerformed().run();
            }
        };
    }

    @Nonnull
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

    protected void buildToolbar(@Nullable List<AnAction> viewerActions) {
        ActionGroup group = collectToolbarActions(viewerActions);
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.DIFF_TOOLBAR, group, true);

        DataManager.registerDataProvider(toolbar.getComponent(), myMainPanel);
        toolbar.setTargetComponent(toolbar.getComponent());

        myToolbarPanel.setContent(toolbar.getComponent());
        for (AnAction action : group.getChildren(null)) {
            AWTDiffUtil.registerAction(action, myMainPanel);
        }
    }

    @Nonnull
    private MergeTool getFittedTool() {
        ExtensionPoint<MergeTool> point = Application.get().getExtensionPoint(MergeTool.class);
        return Objects.requireNonNull(point.findFirstSafe(t -> t.canShow(myContext, myRequest)), "Missed error impl");
    }

    private void setTitle(@Nullable String title) {
        setWindowTitle(title == null ? "Merge" : title);
    }

    @Override
    public void dispose() {
        if (myDisposed) {
            return;
        }
        UIUtil.invokeLaterIfNeeded(() -> {
            if (myDisposed) {
                return;
            }
            myDisposed = true;

            onDispose();

            destroyViewer();
        });
    }

    @RequiredUIAccess
    private void applyRequestResult(@Nonnull MergeResult result) {
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
    private void reopenWithTool(@Nonnull MergeTool tool) {
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
        rebuildSouthPanel();
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

    protected void setWindowTitle(@Nonnull LocalizeValue title) {
        setWindowTitle(title.get());
    }

    @Deprecated
    @DeprecationInfo("Use variant with LocalizeValue")
    protected void setWindowTitle(@Nonnull String title) {
        setWindowTitle(LocalizeValue.of(title));
    }

    protected abstract void rebuildSouthPanel();

    public abstract void closeDialog();

    @Nullable
    public <T> T getContextUserData(@Nonnull Key<T> key) {
        return myContext.getUserData(key);
    }

    public <T> void putContextUserData(@Nonnull Key<T> key, @Nullable T value) {
        myContext.putUserData(key, value);
    }

    //
    // Getters
    //

    @Nonnull
    public JComponent getComponent() {
        return myPanel;
    }

    @Nullable
    public JComponent getPreferredFocusedComponent() {
        JComponent component = myViewer.getPreferredFocusedComponent();
        return component != null ? component : myToolbarPanel.getTargetComponent();
    }

    @Nullable
    public Project getProject() {
        return myProject;
    }

    @Nonnull
    public MergeContext getContext() {
        return myContext;
    }

    @RequiredUIAccess
    public boolean checkCloseAction() {
        return myConflictResolved || myCloseHandler == null || myCloseHandler.getAsBoolean();
    }

    @Nonnull
    public BottomActions getBottomActions() {
        return myBottomActions != null ? myBottomActions : new BottomActions();
    }

    @Nullable
    public String getHelpId() {
        return (String)myMainPanel.getData(HelpManager.HELP_ID);
    }

    //
    // Misc
    //

    public boolean isFocused() {
        return AWTDiffUtil.isFocusedComponent(myProject, myPanel);
    }

    public void requestFocus() {
        AWTDiffUtil.requestFocus(myProject, getPreferredFocusedComponent());
    }

    protected void requestFocusInternal() {
        JComponent component = getPreferredFocusedComponent();
        if (component != null) {
            component.requestFocusInWindow();
        }
    }

    //
    // Navigation
    //

    private static class MyNextDifferenceAction extends NextDifferenceAction {
        @Override
        public void update(@Nonnull AnActionEvent e) {
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
        public void actionPerformed(@Nonnull AnActionEvent e) {
            PrevNextDifferenceIterable iterable = e.getData(DiffDataKeys.PREV_NEXT_DIFFERENCE_ITERABLE);
            if (iterable != null && iterable.canGoNext()) {
                iterable.goNext();
            }
        }
    }

    private static class MyPrevDifferenceAction extends PrevDifferenceAction {
        @Override
        public void update(@Nonnull AnActionEvent e) {
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
        public void actionPerformed(@Nonnull AnActionEvent e) {
            PrevNextDifferenceIterable iterable = e.getData(DiffDataKeys.PREV_NEXT_DIFFERENCE_ITERABLE);
            if (iterable != null && iterable.canGoPrev()) {
                iterable.goPrev();
            }
        }
    }

    //
    // Helpers
    //

    private class MyPanel extends JPanel implements DataProvider {
        public MyPanel() {
            super(new BorderLayout());
        }

        @Nullable
        @Override
        public Object getData(@Nonnull Key<?> dataId) {
            Object data;

            DataProvider contentProvider =
                ((BaseDataManager)DataManager.getInstance()).getDataProviderEx(myContentPanel.getTargetComponent());
            if (contentProvider != null) {
                data = contentProvider.getData(dataId);
                if (data != null) {
                    return data;
                }
            }

            if (Project.KEY == dataId) {
                return myProject;
            }
            else if (HelpManager.HELP_ID == dataId) {
                if (myRequest.getUserData(DiffUserDataKeys.HELP_ID) != null) {
                    return myRequest.getUserData(DiffUserDataKeys.HELP_ID);
                }
                else {
                    return "procedures.vcWithIDEA.commonVcsOps.integrateDiffs.resolveConflict";
                }
            }

            DataProvider requestProvider = myRequest.getUserData(DiffUserDataKeys.DATA_PROVIDER);
            if (requestProvider != null) {
                data = requestProvider.getData(dataId);
                if (data != null) {
                    return data;
                }
            }

            DataProvider contextProvider = myContext.getUserData(DiffUserDataKeys.DATA_PROVIDER);
            if (contextProvider != null) {
                data = contextProvider.getData(dataId);
                if (data != null) {
                    return data;
                }
            }
            return null;
        }
    }

    private class MyFocusTraversalPolicy extends IdeFocusTraversalPolicy {
        @Override
        public final Component getDefaultComponent(final Container focusCycleRoot) {
            JComponent component = MergeRequestProcessor.this.getPreferredFocusedComponent();
            if (component == null) {
                return null;
            }
            return IdeFocusTraversalPolicy.getPreferredFocusedComponent(component, this);
        }
    }

    private class MyDiffContext extends MergeContextEx {
        @Nullable
        @Override
        public Project getProject() {
            return MergeRequestProcessor.this.getProject();
        }

        @Override
        public boolean isFocused() {
            return MergeRequestProcessor.this.isFocused();
        }

        @Override
        public void requestFocus() {
            MergeRequestProcessor.this.requestFocusInternal();
        }

        @Override
        @RequiredUIAccess
        public void finishMerge(@Nonnull MergeResult result) {
            applyRequestResult(result);
            MergeRequestProcessor.this.closeDialog();
        }

        @Override
        @RequiredUIAccess
        public void reopenWithTool(@Nonnull MergeTool tool) {
            MergeRequestProcessor.this.reopenWithTool(tool);
        }
    }

    public static class BottomActions {
        @Nullable
        public LocalizeAction applyLeft;
        @Nullable
        public LocalizeAction applyRight;
        @Nullable
        public LocalizeAction resolveAction;
        @Nullable
        public LocalizeAction cancelAction;
    }
}
