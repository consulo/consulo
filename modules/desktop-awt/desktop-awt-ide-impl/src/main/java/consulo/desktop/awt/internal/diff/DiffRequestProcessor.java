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
package consulo.desktop.awt.internal.diff;

import consulo.application.Application;
import consulo.application.HelpManager;
import consulo.application.dumb.DumbAware;
import consulo.codeEditor.Editor;
import consulo.codeEditor.LogicalPosition;
import consulo.dataContext.DataManager;
import consulo.dataContext.DataProvider;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.desktop.awt.internal.diff.action.OpenInEditorAction;
import consulo.desktop.awt.internal.diff.external.ExternalDiffTool;
import consulo.desktop.awt.internal.diff.util.AWTDiffUtil;
import consulo.diff.*;
import consulo.diff.FrameDiffTool.DiffViewer;
import consulo.diff.internal.DiffSettingsHolder;
import consulo.diff.internal.DiffSettingsHolder.DiffSettings;
import consulo.diff.impl.internal.action.NextChangeAction;
import consulo.diff.impl.internal.action.NextDifferenceAction;
import consulo.diff.impl.internal.action.PrevChangeAction;
import consulo.diff.impl.internal.action.PrevDifferenceAction;
import consulo.diff.internal.DiffImplUtil;
import consulo.diff.internal.DiffManagerEx;
import consulo.diff.internal.DiffUserDataKeysEx;
import consulo.diff.internal.DiffUserDataKeysEx.ScrollToPolicy;
import consulo.diff.request.*;
import consulo.diff.util.LineRange;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.ide.impl.dataContext.BaseDataManager;
import consulo.ide.impl.idea.codeInsight.hint.HintManagerImpl;
import consulo.ide.impl.idea.ui.LightweightHintImpl;
import consulo.language.editor.hint.HintManager;
import consulo.language.editor.ui.awt.HintUtil;
import consulo.logging.Logger;
import consulo.project.Project;
import consulo.project.ui.internal.ProjectIdeFocusManager;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.util.ActionUtil;
import consulo.ui.ex.awt.*;
import consulo.ui.ex.awt.action.ComboBoxAction;
import consulo.ui.ex.awt.hint.HintHint;
import consulo.ui.ex.popup.Balloon;
import consulo.ui.ex.popup.JBPopupFactory;
import consulo.ui.ex.popup.ListPopup;
import consulo.util.collection.ContainerUtil;
import consulo.util.dataholder.Key;
import consulo.util.dataholder.UserDataHolder;
import consulo.util.dataholder.UserDataHolderBase;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@SuppressWarnings("InnerClassMayBeStatic")
public abstract class DiffRequestProcessor implements Disposable {
    private static final Logger LOG = Logger.getInstance(DiffRequestProcessor.class);

    private boolean myDisposed;

    private final @Nullable Project myProject;
    private final DiffContext myContext;

    private final DiffSettings mySettings;
    private final List<DiffTool> myAvailableTools;
    private final LinkedList<DiffTool> myToolOrder;

    private final OpenInEditorAction myOpenInEditorAction;
    private @Nullable DefaultActionGroup myPopupActionGroup;

    private final JPanel myPanel;
    private final MyPanel myMainPanel;
    private final Wrapper myContentPanel;
    private final Wrapper myToolbarPanel; // TODO: allow to call 'updateToolbar' from Viewer ?
    private final Wrapper myToolbarStatusPanel;
    private final MyProgressBar myProgressBar;

    private DiffRequest myActiveRequest;

    private ViewerState myState;

    public DiffRequestProcessor(@Nullable Project project) {
        this(project, new UserDataHolderBase());
    }

    public DiffRequestProcessor(@Nullable Project project, String place) {
        this(project, DiffImplUtil.createUserDataHolder(DiffUserDataKeys.PLACE, place));
    }

    public DiffRequestProcessor(@Nullable Project project, UserDataHolder context) {
        myProject = project;

        myContext = new MyDiffContext(context);
        myActiveRequest = new LoadingDiffRequest();

        mySettings = DiffSettingsHolder.getInstance().getSettings(myContext.getUserData(DiffUserDataKeys.PLACE));

        myAvailableTools = DiffManagerEx.getInstanceEx().getDiffTools();
        myToolOrder = new LinkedList<>(getToolOrderFromSettings(myAvailableTools));

        // UI

        myMainPanel = new MyPanel();
        myContentPanel = new Wrapper();
        myToolbarPanel = new Wrapper();
        myToolbarPanel.setFocusable(true);
        myToolbarStatusPanel = new Wrapper();
        myProgressBar = new MyProgressBar();

        myPanel = JBUI.Panels.simplePanel(myMainPanel);

        JPanel statusPanel = JBUI.Panels.simplePanel(myToolbarStatusPanel).addToLeft(myProgressBar);
        JPanel topPanel = JBUI.Panels.simplePanel(myToolbarPanel).addToRight(statusPanel);

        myMainPanel.add(topPanel, BorderLayout.NORTH);
        myMainPanel.add(myContentPanel, BorderLayout.CENTER);

        myMainPanel.setFocusTraversalPolicyProvider(true);
        myMainPanel.setFocusTraversalPolicy(new MyFocusTraversalPolicy());

        JComponent bottomPanel = myContext.getUserData(DiffUserDataKeysEx.BOTTOM_PANEL);
        if (bottomPanel != null) {
            myMainPanel.add(bottomPanel, BorderLayout.SOUTH);
        }
        if (bottomPanel instanceof Disposable disposable) {
            Disposer.register(this, disposable);
        }

        myState = EmptyState.INSTANCE;
        myContentPanel.setContent(AWTDiffUtil.createMessagePanel(((LoadingDiffRequest)myActiveRequest).getMessage()));

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

        force = force || (myQueuedApplyRequest != null && myQueuedApplyRequest.force);
        myQueuedApplyRequest = new ApplyData(request, force, scrollToChangePolicy);
        @RequiredUIAccess
        Runnable task = () -> {
            if (myQueuedApplyRequest == null || myDisposed) {
                return;
            }
            doApplyRequest(myQueuedApplyRequest.request, myQueuedApplyRequest.force, myQueuedApplyRequest.scrollToChangePolicy);
            myQueuedApplyRequest = null;
        };

        ProjectIdeFocusManager.getInstance(myProject).doWhenFocusSettlesDown(task);
    }

    @RequiredUIAccess
    private void doApplyRequest(DiffRequest request, boolean force, @Nullable ScrollToPolicy scrollToChangePolicy) {
        if (!force && request == myActiveRequest) {
            return;
        }

        request.putUserData(DiffUserDataKeysEx.SCROLL_TO_CHANGE, scrollToChangePolicy);

        boolean hadFocus = isFocused();

        myState.destroy();
        myToolbarStatusPanel.setContent(null);
        myToolbarPanel.setContent(null);
        myContentPanel.setContent(null);
        ActionUtil.clearActions(myMainPanel);

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

    public boolean isWindowFocused() {
        Window window = SwingUtilities.getWindowAncestor(myPanel);
        return window != null && window.isFocused();
    }

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

    protected List<DiffTool> getToolOrderFromSettings(List<DiffTool> availableTools) {
        List<DiffTool> result = new ArrayList<>();
        List<String> savedOrder = getSettings().getDiffToolsOrder();

        for (String clazz : savedOrder) {
            DiffTool tool = ContainerUtil.find(availableTools, tool1 -> tool1.getClass().getCanonicalName().equals(clazz));
            if (tool != null) {
                result.add(tool);
            }
        }

        for (DiffTool tool : availableTools) {
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
        UIUtil.invokeLaterIfNeeded(() -> {
            if (myDisposed) {
                return;
            }
            myDisposed = true;

            onDispose();

            myState.destroy();
            myToolbarStatusPanel.setContent(null);
            myToolbarPanel.setContent(null);
            myContentPanel.setContent(null);

            myActiveRequest.onAssigned(false);

            myState = EmptyState.INSTANCE;
            myActiveRequest = NoDiffRequest.INSTANCE;
        });
    }

    protected DefaultActionGroup collectToolbarActions(@Nullable List<AnAction> viewerActions) {
        DefaultActionGroup group = new DefaultActionGroup();

        List<AnAction> navigationActions = new ArrayList<>();
        navigationActions.addAll(getNavigationActions());
        navigationActions.add(myOpenInEditorAction);
        navigationActions.add(new MyChangeDiffToolAction());
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
            selectToolActions.add(new DiffToolToggleAction(tool));
        }
        DiffImplUtil.addActionBlock(group, selectToolActions);

        DiffImplUtil.addActionBlock(group, viewerActions);

        return group;
    }

    protected void buildToolbar(@Nullable List<AnAction> viewerActions) {
        ActionGroup group = collectToolbarActions(viewerActions);
        ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.DIFF_TOOLBAR, group, true);

        DataManager.registerUiDataProvider(toolbar.getComponent(), myMainPanel);
        toolbar.setTargetComponent(toolbar.getComponent());

        myToolbarPanel.setContent(toolbar.getComponent());
        for (AnAction action : group.getChildren(null)) {
            AWTDiffUtil.registerAction(action, myMainPanel);
        }
    }

    protected void buildActionPopup(@Nullable List<AnAction> viewerActions) {
        ShowActionGroupPopupAction action = new ShowActionGroupPopupAction();
        AWTDiffUtil.registerAction(action, myMainPanel);

        myPopupActionGroup = collectPopupActions(viewerActions);
    }

    private void setTitle(@Nullable String title) {
        if (getContextUserData(DiffUserDataKeys.DO_NOT_CHANGE_WINDOW_TITLE) == Boolean.TRUE) {
            return;
        }
        if (title == null) {
            title = "Diff";
        }
        setWindowTitle(title);
    }

    //
    // Getters
    //

    public JComponent getComponent() {
        return myPanel;
    }

    public @Nullable JComponent getPreferredFocusedComponent() {
        JComponent component = myState.getPreferredFocusedComponent();
        return component != null ? component : myToolbarPanel.getTargetComponent();
    }

    public @Nullable DiffRequest getActiveRequest() {
        return myActiveRequest;
    }

    public @Nullable Project getProject() {
        return myProject;
    }

    public DiffContext getContext() {
        return myContext;
    }

    protected DiffSettings getSettings() {
        return mySettings;
    }

    public boolean isDisposed() {
        return myDisposed;
    }

    //
    // Actions
    //

    private class ShowInExternalToolAction extends DumbAwareAction {
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
                Messages.showErrorDialog(e.getData(Project.KEY), ex.getMessage(), "Can't Show Diff In External Tool");
            }
        }
    }

    private class MyChangeDiffToolAction extends ComboBoxAction implements DumbAware {
        public MyChangeDiffToolAction() {
            // TODO: add icons for diff tools, show only icon in toolbar - to reduce jumping on change ?
            setEnabledInModalContext(true);
        }

        @Override
        public void update(AnActionEvent e) {
            Presentation presentation = e.getPresentation();

            DiffTool activeTool = myState.getActiveTool();
            presentation.setText(activeTool.getName());

            if (activeTool == ErrorDiffTool.INSTANCE) {
                presentation.setEnabledAndVisible(false);
            }

            for (DiffTool tool : getAvailableFittedTools()) {
                if (tool != activeTool) {
                    presentation.setEnabledAndVisible(true);
                    return;
                }
            }

            presentation.setEnabledAndVisible(false);
        }

        @Override
        public DefaultActionGroup createPopupActionGroup(JComponent button) {
            DefaultActionGroup group = new DefaultActionGroup();
            for (DiffTool tool : getAvailableFittedTools()) {
                group.add(new DiffToolToggleAction(tool));
            }

            return group;
        }
    }

    private class DiffToolToggleAction extends AnAction implements DumbAware {
        private final DiffTool myDiffTool;

        private DiffToolToggleAction(DiffTool tool) {
            super(tool.getName());
            setEnabledInModalContext(true);
            myDiffTool = tool;
        }

        @RequiredUIAccess
        @Override
        public void actionPerformed(AnActionEvent e) {
            if (myState.getActiveTool() == myDiffTool) {
                return;
            }

            moveToolOnTop(myDiffTool);

            updateRequest(true);
        }
    }

    private class ShowActionGroupPopupAction extends DumbAwareAction {
        public ShowActionGroupPopupAction() {
            EmptyAction.setupAction(this, "Diff.ShowSettingsPopup", null);
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabled(myPopupActionGroup != null && myPopupActionGroup.getChildrenCount() > 0);
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            assert myPopupActionGroup != null;
            ListPopup popup = JBPopupFactory.getInstance().createActionGroupPopup(
                "Diff Actions",
                myPopupActionGroup,
                e.getDataContext(),
                JBPopupFactory.ActionSelectionAid.SPEEDSEARCH,
                false
            );
            popup.showInCenterOf(myPanel);
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

            if (myIterationState != IterationState.NEXT) {
                notifyMessage(e, true);
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

            if (myIterationState != IterationState.PREV) {
                notifyMessage(e, false);
                myIterationState = IterationState.PREV;
                return;
            }

            goToPrevChange(true);
        }
    }

    @RequiredUIAccess
    private void notifyMessage(AnActionEvent e, boolean next) {
        Editor editor = e.getData(DiffDataKeys.CURRENT_EDITOR);

        // TODO: provide "change" word in chain UserData - for tests/etc
        String message = DiffImplUtil.createNotificationText(
            next ? "Press again to go to the next file" : "Press again to go to the previous file",
            "You can disable this feature in " + DiffImplUtil.getSettingsConfigurablePath()
        );

        LightweightHintImpl hint = new LightweightHintImpl(HintUtil.createInformationLabel(message));
        Point point = new Point(myContentPanel.getWidth() / 2, next ? myContentPanel.getHeight() - JBUI.scale(40) : JBUI.scale(40));

        if (editor == null) {
            Component owner = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
            HintHint hintHint = createNotifyHint(myContentPanel, point, next);
            hint.show(myContentPanel, point.x, point.y, owner instanceof JComponent component ? component : null, hintHint);
        }
        else {
            int x = SwingUtilities.convertPoint(myContentPanel, point, editor.getComponent()).x;

            JComponent header = editor.getHeaderComponent();
            int shift = editor.getScrollingModel().getVerticalScrollOffset() - (header != null ? header.getHeight() : 0);

            LogicalPosition position;
            LineRange changeRange = e.getData(DiffDataKeys.CURRENT_CHANGE_RANGE);
            if (changeRange == null) {
                position = new LogicalPosition(editor.getCaretModel().getLogicalPosition().line + (next ? 1 : 0), 0);
            }
            else {
                position = new LogicalPosition(next ? changeRange.end : changeRange.start, 0);
            }
            int y = editor.logicalPositionToXY(position).y - shift;

            Point editorPoint = new Point(x, y);
            HintHint hintHint = createNotifyHint(editor.getComponent(), editorPoint, !next);
            HintManagerImpl.getInstanceImpl().showEditorHint(
                hint,
                editor,
                editorPoint,
                HintManager.HIDE_BY_ANY_KEY | HintManager.HIDE_BY_TEXT_CHANGE | HintManager.HIDE_BY_SCROLLING,
                0,
                false,
                hintHint
            );
        }
    }

    private static HintHint createNotifyHint(JComponent component, Point point, boolean above) {
        return new HintHint(component, point)
            .setPreferredPosition(above ? Balloon.Position.above : Balloon.Position.below)
            .setAwtTooltip(true)
            .setFont(UIUtil.getLabelFont().deriveFont(Font.BOLD))
            .setTextBg(HintUtil.INFORMATION_COLOR)
            .setShowImmediately(true);
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

    private class MyPanel extends JPanel implements UiDataProvider {
        public MyPanel() {
            super(new BorderLayout());
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension windowSize = AWTDiffUtil.getDefaultDiffPanelSize();
            Dimension size = super.getPreferredSize();
            return new Dimension(Math.max(windowSize.width, size.width), Math.max(windowSize.height, size.height));
        }

        @Override
        public void uiDataSnapshot(DataSink sink) {
            DataProvider contentProvider =
                ((BaseDataManager)DataManager.getInstance()).getDataProviderEx(myContentPanel.getTargetComponent());
            if (contentProvider instanceof UiDataProvider uiDataProvider) {
                sink.uiDataSnapshot(uiDataProvider);
            }

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

            DataProvider requestProvider = myActiveRequest.getUserData(DiffUserDataKeys.DATA_PROVIDER);
            if (requestProvider instanceof UiDataProvider uiDataProvider) {
                sink.uiDataSnapshot(uiDataProvider);
            }

            DataProvider contextProvider = myContext.getUserData(DiffUserDataKeys.DATA_PROVIDER);
            if (contextProvider instanceof UiDataProvider uiDataProvider) {
                sink.uiDataSnapshot(uiDataProvider);
            }
        }
    }

    private static class MyProgressBar extends JProgressBar {
        private int myProgressCount = 0;

        public MyProgressBar() {
            setIndeterminate(true);
            setVisible(false);
        }

        public void startProgress() {
            myProgressCount++;
            setVisible(true);
        }

        public void stopProgress() {
            myProgressCount--;
            LOG.assertTrue(myProgressCount >= 0);
            if (myProgressCount == 0) {
                setVisible(false);
            }
        }
    }

    private class MyFocusTraversalPolicy extends IdeFocusTraversalPolicy {
        @Override
        public final Component getDefaultComponent(Container focusCycleRoot) {
            JComponent component = DiffRequestProcessor.this.getPreferredFocusedComponent();
            if (component == null) {
                return null;
            }
            return IdeFocusTraversalPolicy.getPreferredFocusedComponent(component, this);
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
                myProgressBar.startProgress();
            }
            else {
                myProgressBar.stopProgress();
            }
        }

        @Nullable
        @Override
        public Project getProject() {
            return DiffRequestProcessor.this.getProject();
        }

        @Override
        public boolean isFocused() {
            return DiffRequestProcessor.this.isFocused();
        }

        @Override
        public boolean isWindowFocused() {
            return DiffRequestProcessor.this.isWindowFocused();
        }

        @Override
        public void requestFocus() {
            DiffRequestProcessor.this.requestFocusInternal();
        }

        @Nullable
        @Override
        public <T> T getUserData(Key<T> key) {
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

        @Nullable
        JComponent getPreferredFocusedComponent();

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

        @Nullable
        @Override
        public JComponent getPreferredFocusedComponent() {
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
            myContentPanel.setContent(myViewer.getComponent());

            FrameDiffTool.ToolbarComponents init = myViewer.init();
            buildToolbar(init.toolbarActions);
        }

        @Override
        @RequiredUIAccess
        public void destroy() {
            Disposer.dispose(myViewer);
        }

        @Nullable
        @Override
        public JComponent getPreferredFocusedComponent() {
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
            myContentPanel.setContent(myViewer.getComponent());
            setTitle(myActiveRequest.getTitle());

            FrameDiffTool.ToolbarComponents toolbarComponents = myViewer.init();

            buildToolbar(toolbarComponents.toolbarActions);
            buildActionPopup(toolbarComponents.popupActions);

            myToolbarStatusPanel.setContent(toolbarComponents.statusPanel);
        }

        @Override
        @RequiredUIAccess
        public void destroy() {
            Disposer.dispose(myViewer);
        }

        @Nullable
        @Override
        public JComponent getPreferredFocusedComponent() {
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
            myContentPanel.setContent(myWrapperViewer.getComponent());
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


            myToolbarStatusPanel.setContent(toolbarComponents1.statusPanel); // TODO: combine both panels ?
        }

        @Override
        @RequiredUIAccess
        public void destroy() {
            Disposer.dispose(myViewer);
            Disposer.dispose(myWrapperViewer);
        }

        @Nullable
        @Override
        public JComponent getPreferredFocusedComponent() {
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
