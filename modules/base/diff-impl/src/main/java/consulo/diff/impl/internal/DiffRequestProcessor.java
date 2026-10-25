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
package consulo.diff.impl.internal;

import consulo.application.Application;
import consulo.application.HelpManager;
import consulo.application.ui.wm.ApplicationIdeFocusManager;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.dataContext.DataContext;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.diff.*;
import consulo.diff.DiffEditorViewer;
import consulo.diff.FrameDiffTool.DiffViewer;
import consulo.diff.impl.internal.action.NextChangeAction;
import consulo.diff.impl.internal.action.NextDifferenceAction;
import consulo.diff.impl.internal.action.OpenInEditorAction;
import consulo.diff.impl.internal.action.PrevChangeAction;
import consulo.diff.impl.internal.action.PrevDifferenceAction;
import consulo.diff.impl.internal.external.ExternalDiffTool;
import consulo.diff.impl.internal.util.UnifiedDiffUtil;
import consulo.diff.internal.DiffImplUtil;
import consulo.diff.internal.DiffManagerEx;
import consulo.diff.internal.DiffSettingsHolder;
import consulo.diff.internal.DiffSettingsHolder.DiffSettings;
import consulo.diff.internal.DiffUserDataKeysEx;
import consulo.diff.internal.DiffUserDataKeysEx.ScrollToPolicy;
import consulo.diff.request.*;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.language.editor.hint.HintManager;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.MessageBoxes;
import consulo.ui.ProgressBar;
import consulo.ui.Space;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.WrappedLayout;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.UserDataHolderBase;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("InnerClassMayBeStatic")
public abstract class DiffRequestProcessor implements DiffEditorViewer, UiDataProvider {
    private static final Logger LOG = Logger.getInstance(DiffRequestProcessor.class);

    private boolean myDisposed;

    private final @Nullable Project myProject;
    private final DiffContext myContext;

    private final DiffSettings mySettings;
    private final List<DiffTool> myAvailableTools;
    private final LinkedList<DiffTool> myToolOrder;

    private final OpenInEditorAction myOpenInEditorAction;
    private final DiffToolChooser myToolChooser;
    private @Nullable DefaultActionGroup myPopupActionGroup;

    private final DockLayout myPanel;
    private final WrappedLayout myContentPanel;
    private final WrappedLayout myToolbarPanel;
    private final WrappedLayout myToolbarStatusPanel;
    private final ProgressBar myProgressBar;
    private int myProgressCount;

    private @Nullable Component myContent;
    private @Nullable ActionToolbar myToolbar;
    private final List<AnAction> myRegisteredActions = new ArrayList<>();

    private DiffRequest myActiveRequest;

    private ViewerState myState;

    @RequiredUIAccess
    public DiffRequestProcessor(@Nullable Project project) {
        this(project, new UserDataHolderBase());
    }

    @RequiredUIAccess
    public DiffRequestProcessor(@Nullable Project project, String place) {
        this(project, DiffImplUtil.createUserDataHolder(DiffUserDataKeys.PLACE, place));
    }

    @RequiredUIAccess
    public DiffRequestProcessor(@Nullable Project project, UserDataHolder context) {
        myProject = project;
        myToolChooser = new MyDiffToolChooser();

        myContext = new MyDiffContext(context);
        myActiveRequest = new LoadingDiffRequest();

        mySettings = DiffSettingsHolder.getInstance().getSettings(myContext.getUserData(DiffUserDataKeys.PLACE));

        myAvailableTools = DiffManagerEx.getInstanceEx().getDiffTools();
        myToolOrder = new LinkedList<>(getToolOrderFromSettings(myAvailableTools));

        // UI

        myContentPanel = WrappedLayout.create();
        myToolbarPanel = WrappedLayout.create();
        myToolbarStatusPanel = WrappedLayout.create();
        myProgressBar = ProgressBar.create();
        myProgressBar.setIndeterminate(true);
        myProgressBar.setVisible(false);

        HorizontalLayout statusPanel = HorizontalLayout.create(Space.X_SMALL);
        statusPanel.add(myProgressBar);
        statusPanel.add(myToolbarStatusPanel);

        DockLayout topPanel = DockLayout.create(Space.NONE);
        topPanel.center(myToolbarPanel);
        topPanel.right(statusPanel);

        myPanel = DockLayout.create(Space.NONE);
        myPanel.top(topPanel);
        myPanel.center(myContentPanel);
        myPanel.putUserData(UiDataProvider.KEY, this);

        Component bottomPanel = myContext.getUserData(DiffUserDataKeysEx.BOTTOM_PANEL);
        if (bottomPanel != null) {
            myPanel.bottom(bottomPanel);
        }
        if (bottomPanel instanceof Disposable disposable) {
            Disposer.register(this, disposable);
        }

        myState = EmptyState.INSTANCE;
        setContent(UnifiedDiffUtil.createMessagePanel(LocalizeValue.of(((LoadingDiffRequest) myActiveRequest).getMessage())));

        myOpenInEditorAction = new OpenInEditorAction(this::onAfterNavigate);
    }

    //
    // Update
    //

    @RequiredUIAccess
    protected void reloadRequest() {
        updateRequest(true);
    }

    @RequiredUIAccess
    public void updateRequest() {
        updateRequest(false);
    }

    @RequiredUIAccess
    public void updateRequest(boolean force) {
        updateRequest(force, null);
    }

    @RequiredUIAccess
    public abstract void updateRequest(boolean force, @Nullable ScrollToPolicy scrollToChangePolicy);

    private FrameDiffTool getFittedTool() {
        List<FrameDiffTool> tools = new ArrayList<>();
        for (DiffTool tool : myToolOrder) {
            try {
                if (tool instanceof FrameDiffTool frameDiffTool && tool.canShow(myContext, myActiveRequest)) {
                    tools.add(frameDiffTool);
                }
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }

        tools = DiffImplUtil.filterSuppressedTools(tools);

        return tools.isEmpty() ? ErrorDiffTool.INSTANCE : tools.get(0);
    }

    private List<FrameDiffTool> getAvailableFittedTools() {
        List<FrameDiffTool> tools = new ArrayList<>();
        for (DiffTool tool : myAvailableTools) {
            try {
                if (tool instanceof FrameDiffTool frameDiffTool && tool.canShow(myContext, myActiveRequest)) {
                    tools.add(frameDiffTool);
                }
            }
            catch (Throwable e) {
                LOG.error(e);
            }
        }

        return DiffImplUtil.filterSuppressedTools(tools);
    }

    private void moveToolOnTop(DiffTool tool) {
        myToolOrder.remove(tool);

        FrameDiffTool toolToReplace = getFittedTool();

        int index;
        for (index = 0; index < myToolOrder.size(); index++) {
            if (myToolOrder.get(index) == toolToReplace) {
                break;
            }
        }
        myToolOrder.add(index, tool);

        updateToolOrderSettings(myToolOrder);
    }

    @RequiredUIAccess
    private ViewerState createState() {
        FrameDiffTool frameTool = getFittedTool();

        DiffViewer viewer = frameTool.createComponent(myContext, myActiveRequest);

        Application.get().getExtensionPoint(DiffExtension.class)
            .forEach(extension -> extension.onViewerCreated(viewer, myContext, myActiveRequest));

        DiffViewerWrapper wrapper = myActiveRequest.getUserData(DiffViewerWrapper.KEY);
        if (wrapper == null) {
            return new DefaultState(viewer, frameTool);
        }
        else {
            return new WrapperState(viewer, frameTool, wrapper);
        }
    }

    //
    // Abstract
    //

    private @Nullable ApplyData myQueuedApplyRequest;

    @RequiredUIAccess
    protected void applyRequest(DiffRequest request, boolean force, @Nullable ScrollToPolicy scrollToChangePolicy) {
        myIterationState = IterationState.NONE;

        boolean forceApply = force || (myQueuedApplyRequest != null && myQueuedApplyRequest.force);
        myQueuedApplyRequest = new ApplyData(request, forceApply, scrollToChangePolicy);
        @RequiredUIAccess
        Runnable task = () -> {
            if (myQueuedApplyRequest == null || myDisposed) {
                return;
            }
            doApplyRequest(myQueuedApplyRequest.request, myQueuedApplyRequest.force, myQueuedApplyRequest.scrollToChangePolicy);
            myQueuedApplyRequest = null;
        };

        ApplicationIdeFocusManager.getInstance().doWhenFocusSettlesDown(task);
    }

    @RequiredUIAccess
    private void doApplyRequest(DiffRequest request, boolean force, @Nullable ScrollToPolicy scrollToChangePolicy) {
        if (!force && request == myActiveRequest) {
            return;
        }

        request.putUserData(DiffUserDataKeysEx.SCROLL_TO_CHANGE, scrollToChangePolicy);

        boolean hadFocus = isFocused();

        myState.destroy();
        myToolbarStatusPanel.set((Component) null);
        myToolbarPanel.set((Component) null);
        myToolbar = null;
        setContent(null);
        clearRegisteredActions();

        myActiveRequest.onAssigned(false);
        myActiveRequest = request;
        myActiveRequest.onAssigned(true);

        try {
            myState = createState();
            myState.init();
        }
        catch (Throwable e) {
            LOG.error(e);
            myState = new ErrorState(new ErrorDiffRequest("Error: can't show diff"), getFittedTool());
            myState.init();
        }

        if (hadFocus) {
            requestFocusInternal();
        }
    }

    protected void setWindowTitle(String title) {
    }

    protected void onAfterNavigate() {
    }

    @RequiredUIAccess
    protected void onDispose() {
    }

    public @Nullable <T> T getContextUserData(Key<T> key) {
        return myContext.getUserData(key);
    }

    public <T> void putContextUserData(Key<T> key, @Nullable T value) {
        myContext.putUserData(key, value);
    }

    protected List<AnAction> getNavigationActions() {
        return ContainerUtil.<AnAction>list(
            new MyPrevDifferenceAction(),
            new MyNextDifferenceAction(),
            new MyPrevChangeAction(),
            new MyNextChangeAction()
        );
    }

    //
    // Misc
    //

    @RequiredUIAccess
    public boolean isWindowFocused() {
        return UnifiedDiffUtil.isWindowFocused(myPanel);
    }

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

    protected List<DiffTool> getToolOrderFromSettings(List<DiffTool> availableTools) {
        return getToolOrderFromSettings(getSettings(), availableTools);
    }

    public static <T extends DiffTool> List<T> getToolOrderFromSettings(DiffSettings diffSettings, List<T> availableTools) {
        List<T> result = new ArrayList<>();
        List<String> savedOrder = diffSettings.getDiffToolsOrder();

        for (String clazz : savedOrder) {
            T tool = ContainerUtil.find(availableTools, tool1 -> tool1.getClass().getCanonicalName().equals(clazz));
            if (tool != null) {
                result.add(tool);
            }
        }

        for (T tool : availableTools) {
            if (!result.contains(tool)) {
                result.add(tool);
            }
        }

        return result;
    }

    protected void updateToolOrderSettings(List<DiffTool> toolOrder) {
        List<String> savedOrder = new ArrayList<>();
        for (DiffTool tool : toolOrder) {
            savedOrder.add(tool.getClass().getCanonicalName());
        }
        getSettings().setDiffToolsOrder(savedOrder);
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

            myState.destroy();
            myToolbarStatusPanel.set((Component) null);
            myToolbarPanel.set((Component) null);
            myToolbar = null;
            setContent(null);
            clearRegisteredActions();

            myActiveRequest.onAssigned(false);

            myState = EmptyState.INSTANCE;
            myActiveRequest = NoDiffRequest.INSTANCE;
        };
        if (UIAccess.isUIThread()) {
            disposeTask.run();
        }
        else {
            Application.get().invokeLater(disposeTask);
        }
    }

    protected DefaultActionGroup collectToolbarActions(@Nullable List<AnAction> viewerActions) {
        DefaultActionGroup group = new DefaultActionGroup();

        List<AnAction> navigationActions = new ArrayList<>();
        navigationActions.addAll(getNavigationActions());
        navigationActions.add(myOpenInEditorAction);
        navigationActions.add(myToolChooser);
        DiffImplUtil.addActionBlock(
            group,
            navigationActions
        );

        DiffImplUtil.addActionBlock(group, viewerActions);

        List<AnAction> requestContextActions = myActiveRequest.getUserData(DiffUserDataKeys.CONTEXT_ACTIONS);
        DiffImplUtil.addActionBlock(group, requestContextActions);

        List<AnAction> contextActions = myContext.getUserData(DiffUserDataKeys.CONTEXT_ACTIONS);
        DiffImplUtil.addActionBlock(group, contextActions);

        DiffImplUtil.addActionBlock(
            group,
            new ShowInExternalToolAction(),
            ActionManager.getInstance().getAction(IdeActions.ACTION_CONTEXT_HELP)
        );

        return group;
    }

    protected DefaultActionGroup collectPopupActions(@Nullable List<AnAction> viewerActions) {
        DefaultActionGroup group = new DefaultActionGroup();

        List<AnAction> selectToolActions = new ArrayList<>();
        for (DiffTool tool : getAvailableFittedTools()) {
            if (tool == myState.getActiveTool()) {
                continue;
            }
            selectToolActions.add(myToolChooser.createSelectAction(tool));
        }
        DiffImplUtil.addActionBlock(group, selectToolActions);

        DiffImplUtil.addActionBlock(group, viewerActions);

        DiffImplUtil.addActionBlock(
            group,
            ActionManager.getInstance().getAction("Vcs.Diff.ToggleDiffAligningMode"),
            ActionManager.getInstance().getAction("Vcs.Diff.EnableDisableCombinedDiff")
        );

        return group;
    }

    @RequiredUIAccess
    protected void buildToolbar(@Nullable List<AnAction> viewerActions) {
        ActionGroup group = collectToolbarActions(viewerActions);

        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.DIFF_TOOLBAR, group, true);

        toolbar.setTargetUIComponent(myContent != null ? myContent : myContentPanel);

        myToolbar = toolbar;
        myToolbarPanel.set(toolbar.getUIComponent());

        for (AnAction action : group.getChildren(null)) {
            registerAction(action);
        }
    }

    @RequiredUIAccess
    protected void buildActionPopup(@Nullable List<AnAction> viewerActions) {
        ShowActionGroupPopupAction action = new ShowActionGroupPopupAction(() -> myPopupActionGroup);
        registerAction(action);

        myPopupActionGroup = collectPopupActions(viewerActions);
    }

    @RequiredUIAccess
    private void registerAction(AnAction action) {
        UnifiedDiffUtil.registerAction(action, myPanel);
        myRegisteredActions.add(action);
    }

    @RequiredUIAccess
    private void clearRegisteredActions() {
        for (AnAction action : myRegisteredActions) {
            action.unregisterCustomShortcutSet(myPanel);
        }
        myRegisteredActions.clear();
    }

    @RequiredUIAccess
    private void setContent(@Nullable Component content) {
        myContent = content;
        myContentPanel.set(content);
    }

    private void setTitle(@Nullable String title) {
        if (getContextUserData(DiffUserDataKeys.DO_NOT_CHANGE_WINDOW_TITLE) == Boolean.TRUE) {
            return;
        }
        setWindowTitle(title == null ? "Diff" : title);
    }

    //
    // Getters
    //

    public Component getComponent() {
        return myPanel;
    }

    public @Nullable Component getPreferredFocusedComponent() {
        Component component = myState.getPreferredFocusedComponent();
        if (component != null) {
            return component;
        }
        return myToolbar != null ? myToolbar.getUIComponent() : null;
    }

    public @Nullable DiffRequest getActiveRequest() {
        return myActiveRequest;
    }

    public @Nullable Project getProject() {
        return myProject;
    }

    @Override
    public DiffContext getContext() {
        return myContext;
    }

    protected DiffSettings getSettings() {
        return mySettings;
    }

    @Override
    public boolean isDisposed() {
        return myDisposed;
    }

    @Override
    @RequiredUIAccess
    public void fireProcessorActivated() {
        updateRequest();
    }

    public @Nullable DiffViewer getActiveViewer() {
        if (myState instanceof DefaultState defaultState) {
            return defaultState.myViewer;
        }
        if (myState instanceof WrapperState wrapperState) {
            return wrapperState.myViewer;
        }
        return null;
    }

    //
    // Actions
    //

    private class ShowInExternalToolAction extends DumbAwareAction implements AnActionWithSyncUpdate {
        public ShowInExternalToolAction() {
            EmptyAction.setupAction(this, "Diff.ShowInExternalTool", null);
        }

        @Override
        public void update(AnActionEvent e) {
            if (!ExternalDiffTool.isEnabled()) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }
            e.getPresentation().setEnabled(ExternalDiffTool.canShow(myActiveRequest));
            e.getPresentation().setVisible(true);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            try {
                ExternalDiffTool.showRequest(e.getData(Project.KEY), myActiveRequest);
            }
            catch (Throwable ex) {
                MessageBoxes.okError(LocalizeValue.ofNullable(ex.getMessage()))
                    .title(LocalizeValue.localizeTODO("Can't Show Diff In External Tool"))
                    .showAsync();
            }
        }
    }

    private class MyDiffToolChooser extends DiffToolChooser {
        private MyDiffToolChooser() {
            super(myProject);
        }

        @RequiredUIAccess
        @Override
        public void onSelected(@Nullable Project project, DiffTool diffTool) {
            if (myState.getActiveTool() == diffTool) {
                return;
            }

            moveToolOnTop(diffTool);

            updateRequest(true);
        }

        @Override
        public List<? extends DiffTool> getTools() {
            return getAvailableFittedTools();
        }

        @Override
        public DiffTool getActiveTool() {
            return myState.getActiveTool();
        }

        @Override
        public @Nullable DiffTool getForcedDiffTool() {
            DiffTool activeTool = myState.getActiveTool();
            return activeTool == ErrorDiffTool.INSTANCE ? activeTool : null;
        }
    }

    //
    // Navigation
    //

    private enum IterationState {
        NEXT,
        PREV,
        NONE
    }

    private IterationState myIterationState = IterationState.NONE;

    protected boolean hasNextChange() {
        return false;
    }

    protected boolean hasPrevChange() {
        return false;
    }

    @RequiredUIAccess
    protected void goToNextChange(boolean fromDifferences) {
    }

    @RequiredUIAccess
    protected void goToPrevChange(boolean fromDifferences) {
    }

    protected boolean isNavigationEnabled() {
        return false;
    }

    protected class MyNextDifferenceAction extends NextDifferenceAction {
        @Override
        public void update(AnActionEvent e) {
            if (!ActionPlaces.DIFF_TOOLBAR.equals(e.getPlace())) {
                e.getPresentation().setEnabledAndVisible(true);
                return;
            }

            PrevNextDifferenceIterable iterable = e.getData(DiffDataKeys.PREV_NEXT_DIFFERENCE_ITERABLE);
            if (iterable != null && iterable.canGoNext()) {
                e.getPresentation().setEnabled(true);
                return;
            }

            if (getSettings().isGoToNextFileOnNextDifference() && isNavigationEnabled() && hasNextChange()) {
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
                myIterationState = IterationState.NONE;
                return;
            }

            if (!isNavigationEnabled() || !hasNextChange() || !getSettings().isGoToNextFileOnNextDifference()) {
                return;
            }

            if (myIterationState != IterationState.NEXT && notifyMessage(e, true)) {
                myIterationState = IterationState.NEXT;
                return;
            }

            goToNextChange(true);
        }
    }

    protected class MyPrevDifferenceAction extends PrevDifferenceAction {
        @Override
        public void update(AnActionEvent e) {
            if (!ActionPlaces.DIFF_TOOLBAR.equals(e.getPlace())) {
                e.getPresentation().setEnabledAndVisible(true);
                return;
            }

            PrevNextDifferenceIterable iterable = e.getData(DiffDataKeys.PREV_NEXT_DIFFERENCE_ITERABLE);
            if (iterable != null && iterable.canGoPrev()) {
                e.getPresentation().setEnabled(true);
                return;
            }

            if (getSettings().isGoToNextFileOnNextDifference() && isNavigationEnabled() && hasPrevChange()) {
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
                myIterationState = IterationState.NONE;
                return;
            }

            if (!isNavigationEnabled() || !hasPrevChange() || !getSettings().isGoToNextFileOnNextDifference()) {
                return;
            }

            if (myIterationState != IterationState.PREV && notifyMessage(e, false)) {
                myIterationState = IterationState.PREV;
                return;
            }

            goToPrevChange(true);
        }
    }

    @RequiredUIAccess
    private boolean notifyMessage(AnActionEvent e, boolean next) {
        Editor editor = e.getData(DiffDataKeys.CURRENT_EDITOR);
        if (editor == null) {
            return false;
        }

        // TODO: provide "change" word in chain UserData - for tests/etc
        String message = DiffImplUtil.createNotificationText(
            next ? "Press again to go to the next file" : "Press again to go to the previous file",
            "You can disable this feature in " + DiffImplUtil.getSettingsConfigurablePath()
        );

        HintManager.getInstance().showInformationHint(editor, LocalizeValue.of(message), next ? HintManager.UNDER : HintManager.ABOVE);
        return true;
    }

    // Iterate requests

    protected class MyNextChangeAction extends NextChangeAction {
        @Override
        public void update(AnActionEvent e) {
            if (!ActionPlaces.DIFF_TOOLBAR.equals(e.getPlace())) {
                e.getPresentation().setEnabledAndVisible(true);
                return;
            }

            if (!isNavigationEnabled()) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }

            e.getPresentation().setVisible(true);
            e.getPresentation().setEnabled(hasNextChange());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            if (!isNavigationEnabled() || !hasNextChange()) {
                return;
            }

            goToNextChange(false);
        }
    }

    protected class MyPrevChangeAction extends PrevChangeAction {
        @Override
        public void update(AnActionEvent e) {
            if (!ActionPlaces.DIFF_TOOLBAR.equals(e.getPlace())) {
                e.getPresentation().setEnabledAndVisible(true);
                return;
            }

            if (!isNavigationEnabled()) {
                e.getPresentation().setEnabledAndVisible(false);
                return;
            }

            e.getPresentation().setVisible(true);
            e.getPresentation().setEnabled(hasPrevChange());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            if (!isNavigationEnabled() || !hasPrevChange()) {
                return;
            }

            goToPrevChange(false);
        }
    }

    //
    // Helpers
    //

    @Override
    public void uiDataSnapshot(DataSink sink) {
        sink.set(OpenInEditorAction.KEY, myOpenInEditorAction);
        sink.set(DiffDataKeys.DIFF_REQUEST, myActiveRequest);
        sink.set(Project.KEY, myProject);
        sink.lazy(HelpManager.HELP_ID, () -> {
            if (myActiveRequest.getUserData(DiffUserDataKeys.HELP_ID) != null) {
                return myActiveRequest.getUserData(DiffUserDataKeys.HELP_ID);
            }
            else {
                return "reference.dialogs.diff.file";
            }
        });
        sink.set(DiffDataKeys.DIFF_CONTEXT, myContext);

        myState.uiDataSnapshot(sink);

        UiDataProvider requestProvider = myActiveRequest.getUserData(DiffUserDataKeys.DATA_PROVIDER);
        if (requestProvider != null) {
            sink.uiDataSnapshot(requestProvider);
        }

        UiDataProvider contextProvider = myContext.getUserData(DiffUserDataKeys.DATA_PROVIDER);
        if (contextProvider != null) {
            sink.uiDataSnapshot(contextProvider);
        }
    }

    @RequiredUIAccess
    private void startProgress() {
        myProgressCount++;
        myProgressBar.setVisible(true);
    }

    @RequiredUIAccess
    private void stopProgress() {
        myProgressCount--;
        LOG.assertTrue(myProgressCount >= 0);
        if (myProgressCount == 0) {
            myProgressBar.setVisible(false);
        }
    }

    private class MyDiffContext extends DiffContextEx {
        private final UserDataHolder myContext;

        public MyDiffContext(UserDataHolder context) {
            myContext = context;
        }

        @RequiredUIAccess
        @Override
        public void reopenDiffRequest() {
            updateRequest(true);
        }

        @RequiredUIAccess
        @Override
        public void reloadDiffRequest() {
            reloadRequest();
        }

        @RequiredUIAccess
        @Override
        public void showProgressBar(boolean enabled) {
            if (enabled) {
                startProgress();
            }
            else {
                stopProgress();
            }
        }

        @Override
        public @Nullable Project getProject() {
            return DiffRequestProcessor.this.getProject();
        }

        @Override
        @RequiredUIAccess
        public boolean isFocused() {
            return DiffRequestProcessor.this.isFocused();
        }

        @Override
        @RequiredUIAccess
        public boolean isWindowFocused() {
            return DiffRequestProcessor.this.isWindowFocused();
        }

        @Override
        @RequiredUIAccess
        public void requestFocus() {
            DiffRequestProcessor.this.requestFocusInternal();
        }

        @Override
        public <T> @Nullable T getUserData(Key<T> key) {
            return myContext.getUserData(key);
        }

        @Override
        public <T> void putUserData(Key<T> key, @Nullable T value) {
            myContext.putUserData(key, value);
        }
    }

    private static class ApplyData {
        private final DiffRequest request;
        private final boolean force;
        private final @Nullable ScrollToPolicy scrollToChangePolicy;

        public ApplyData(DiffRequest request, boolean force, @Nullable ScrollToPolicy scrollToChangePolicy) {
            this.request = request;
            this.force = force;
            this.scrollToChangePolicy = scrollToChangePolicy;
        }
    }

    //
    // States
    //

    private interface ViewerState {
        @RequiredUIAccess
        void init();

        @RequiredUIAccess
        void destroy();

        @Nullable Component getPreferredFocusedComponent();

        void uiDataSnapshot(DataSink sink);

        DiffTool getActiveTool();
    }

    private static class EmptyState implements ViewerState {
        private static final EmptyState INSTANCE = new EmptyState();

        @RequiredUIAccess
        @Override
        public void init() {
        }

        @RequiredUIAccess
        @Override
        public void destroy() {
        }

        @Override
        public @Nullable Component getPreferredFocusedComponent() {
            return null;
        }

        @Override
        public void uiDataSnapshot(DataSink sink) {
        }

        @Override
        public DiffTool getActiveTool() {
            return ErrorDiffTool.INSTANCE;
        }
    }

    private class ErrorState implements ViewerState {
        private final @Nullable DiffTool myDiffTool;
        private final MessageDiffRequest myRequest;

        private final DiffViewer myViewer;

        @RequiredUIAccess
        public ErrorState(MessageDiffRequest request) {
            this(request, null);
        }

        @RequiredUIAccess
        public ErrorState(MessageDiffRequest request, @Nullable DiffTool diffTool) {
            myDiffTool = diffTool;
            myRequest = request;

            myViewer = ErrorDiffTool.INSTANCE.createComponent(myContext, myRequest);
        }

        @Override
        @RequiredUIAccess
        public void init() {
            setContent(myViewer.getComponent());

            FrameDiffTool.ToolbarComponents init = myViewer.init();
            buildToolbar(init.toolbarActions);
        }

        @Override
        @RequiredUIAccess
        public void destroy() {
            Disposer.dispose(myViewer);
        }

        @Override
        public @Nullable Component getPreferredFocusedComponent() {
            return null;
        }

        @Override
        public void uiDataSnapshot(DataSink sink) {
        }

        @Override
        public DiffTool getActiveTool() {
            return myDiffTool != null ? myDiffTool : ErrorDiffTool.INSTANCE;
        }
    }

    private class DefaultState implements ViewerState {
        private final DiffViewer myViewer;
        private final FrameDiffTool myTool;

        public DefaultState(DiffViewer viewer, FrameDiffTool tool) {
            myViewer = viewer;
            myTool = tool;
        }

        @Override
        @RequiredUIAccess
        public void init() {
            setContent(myViewer.getComponent());
            setTitle(myActiveRequest.getTitle());

            FrameDiffTool.ToolbarComponents toolbarComponents = myViewer.init();

            buildToolbar(toolbarComponents.toolbarActions);
            buildActionPopup(toolbarComponents.popupActions);

            myToolbarStatusPanel.set(toolbarComponents.statusPanel);
        }

        @Override
        @RequiredUIAccess
        public void destroy() {
            Disposer.dispose(myViewer);
        }

        @Override
        public @Nullable Component getPreferredFocusedComponent() {
            return myViewer.getPreferredFocusedComponent();
        }

        @Override
        public DiffTool getActiveTool() {
            return myTool;
        }

        @Override
        public void uiDataSnapshot(DataSink sink) {
            sink.set(DiffDataKeys.DIFF_VIEWER, myViewer);
        }
    }

    private class WrapperState implements ViewerState {
        private final DiffViewer myViewer;
        private final FrameDiffTool myTool;

        private DiffViewer myWrapperViewer;

        public WrapperState(DiffViewer viewer, FrameDiffTool tool, DiffViewerWrapper wrapper) {
            myViewer = viewer;
            myTool = tool;
            myWrapperViewer = wrapper.createComponent(myContext, myActiveRequest, myViewer);
        }

        @Override
        @RequiredUIAccess
        public void init() {
            setContent(myWrapperViewer.getComponent());
            setTitle(myActiveRequest.getTitle());


            FrameDiffTool.ToolbarComponents toolbarComponents1 = myViewer.init();
            FrameDiffTool.ToolbarComponents toolbarComponents2 = myWrapperViewer.init();

            List<AnAction> toolbarActions = new ArrayList<>();
            if (toolbarComponents1.toolbarActions != null) {
                toolbarActions.addAll(toolbarComponents1.toolbarActions);
            }
            if (toolbarComponents2.toolbarActions != null) {
                if (!toolbarActions.isEmpty() && !toolbarComponents2.toolbarActions.isEmpty()) {
                    toolbarActions.add(AnSeparator.getInstance());
                }
                toolbarActions.addAll(toolbarComponents2.toolbarActions);
            }
            buildToolbar(toolbarActions);

            List<AnAction> popupActions = new ArrayList<>();
            if (toolbarComponents1.popupActions != null) {
                popupActions.addAll(toolbarComponents1.popupActions);
            }
            if (toolbarComponents2.popupActions != null) {
                if (!popupActions.isEmpty() && !toolbarComponents2.popupActions.isEmpty()) {
                    popupActions.add(AnSeparator.getInstance());
                }
                popupActions.addAll(toolbarComponents2.popupActions);
            }
            buildActionPopup(popupActions);


            myToolbarStatusPanel.set(toolbarComponents1.statusPanel); // TODO: combine both panels ?
        }

        @Override
        @RequiredUIAccess
        public void destroy() {
            Disposer.dispose(myViewer);
            Disposer.dispose(myWrapperViewer);
        }

        @Override
        public @Nullable Component getPreferredFocusedComponent() {
            return myWrapperViewer.getPreferredFocusedComponent();
        }

        @Override
        public DiffTool getActiveTool() {
            return myTool;
        }

        @Override
        public void uiDataSnapshot(DataSink sink) {
            sink.set(DiffDataKeys.WRAPPING_DIFF_VIEWER, myWrapperViewer);
            sink.set(DiffDataKeys.DIFF_VIEWER, myViewer);
        }
    }
}
