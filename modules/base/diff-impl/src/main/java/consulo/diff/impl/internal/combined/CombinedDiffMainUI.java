// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.diff.impl.internal.combined.search.CombinedDiffSearchController;
import consulo.diff.impl.internal.combined.search.CombinedDiffSearchContext;
import consulo.dataContext.DataSink;
import consulo.dataContext.UiDataProvider;
import consulo.diff.DiffContext;
import consulo.diff.DiffDataKeys;
import consulo.diff.DiffTool;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.FrameDiffTool;
import consulo.diff.impl.internal.DiffRequestProcessor;
import consulo.diff.impl.internal.DiffToolChooser;
import consulo.diff.impl.internal.ShowActionGroupPopupAction;
import consulo.diff.impl.internal.util.UnifiedDiffUtil;
import consulo.diff.internal.DiffImplUtil;
import consulo.diff.internal.DiffManagerEx;
import consulo.diff.internal.DiffSettingsHolder;
import consulo.diff.internal.DiffUserDataKeysEx;
import consulo.diff.request.DiffRequest;
import consulo.disposer.Disposable;
import consulo.disposer.Disposer;
import consulo.localize.LocalizeValue;
import consulo.platform.base.localize.CommonLocalize;
import consulo.project.Project;
import consulo.ui.Component;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.WrappedLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CombinedDiffMainUI implements Disposable {
    private final CombinedDiffModel myModel;
    private final DiffContext myContext;
    private final DiffSettingsHolder.DiffSettings mySettings;

    private final DefaultActionGroup myPopupActionGroup = new DefaultActionGroup();
    private final List<AnAction> myRegisteredActions = new ArrayList<>();
    private final DockLayout myMainPanel;

    private final WrappedLayout myContentPanel;

    private final MyDiffToolChooser myDiffToolChooser;

    private final CombinedDiffUIState myCombinedDiffUIState = new CombinedDiffUIState();

    private final CombinedDiffMainToolbar myMainToolbar;

    private boolean myDisposed;

    private @Nullable CombinedDiffSearchController mySearchController;

    @RequiredUIAccess
    public CombinedDiffMainUI(CombinedDiffModel model, AnAction goToChangeAction) {
        myModel = model;
        myContext = model.getContext();
        mySettings = DiffSettingsHolder.DiffSettings.getSettings(myContext.getUserData(DiffUserDataKeys.PLACE));

        myMainPanel = DockLayout.create(Space.NONE);
        myContentPanel = WrappedLayout.create();
        myDiffToolChooser = new MyDiffToolChooser(myContext, model, mySettings);

        myMainToolbar = new CombinedDiffMainToolbar(this, myCombinedDiffUIState, myMainPanel, myDiffToolChooser, myContext, goToChangeAction);

        myMainPanel.top(myMainToolbar.getComponent());
        myMainPanel.center(myContentPanel);
        myMainPanel.putUserData(UiDataProvider.KEY, this::uiDataSnapshot);

        Component bottomPanel = myContext.getUserData(DiffUserDataKeysEx.BOTTOM_PANEL);
        if (bottomPanel != null) {
            myMainPanel.bottom(bottomPanel);
        }
        if (bottomPanel instanceof Disposable disposable) {
            Disposer.register(this, disposable);
        }

        myContentPanel.set(UnifiedDiffUtil.createMessagePanel(CommonLocalize.treeNodeLoading()));
    }

    @RequiredUIAccess
    void setContent(CombinedDiffViewer viewer) {
        clear();
        myContentPanel.set(viewer.getComponent());
        FrameDiffTool.ToolbarComponents toolbarComponents = viewer.init();
        myMainToolbar.updateToolbar(toolbarComponents.toolbarActions);
        buildActionPopup(toolbarComponents.popupActions);
    }

    @RequiredUIAccess
    public void setSearchController(CombinedDiffSearchController searchController) {
        mySearchController = searchController;
        myCombinedDiffUIState.setSearchMode(true);
        myMainToolbar.setSearchComponent(searchController.getSearchComponent());
    }

    @RequiredUIAccess
    public void updateSearch(CombinedDiffSearchContext context) {
        CombinedDiffSearchController controller = mySearchController;
        if (controller != null) {
            controller.update(context);
        }
    }

    @RequiredUIAccess
    public void closeSearch() {
        mySearchController = null;
        myMainToolbar.hideSearch();
        myCombinedDiffUIState.setSearchMode(false);

        CombinedDiffViewer combinedViewer = myContext.getUserData(CombinedDiffViewerKeys.COMBINED_DIFF_VIEWER_KEY);
        if (combinedViewer != null) {
            UnifiedDiffUtil.requestFocus(combinedViewer.getPreferredFocusedComponent());
        }
    }

    public @Nullable Component getPreferredFocusedComponent() {
        return myMainToolbar.getPreferredFocusedComponent();
    }

    public Component getComponent() {
        return myMainPanel;
    }

    public boolean isUnified() {
        return myDiffToolChooser.getActiveTool() instanceof CombinedUnifiedDiffTool;
    }

    public boolean isFocusedInWindow() {
        return myMainToolbar.isFocusedInWindow();
    }

    public boolean isWindowFocused() {
        return true;
    }

    @RequiredUIAccess
    public void requestFocusInWindow() {
        UnifiedDiffUtil.requestFocus(getPreferredFocusedComponent());
    }

    @RequiredUIAccess
    private void buildActionPopup(@Nullable List<AnAction> popupActions) {
        myPopupActionGroup.removeAll();
        DiffImplUtil.addActionBlock(myPopupActionGroup, myDiffToolChooser);
        DiffImplUtil.addActionBlock(myPopupActionGroup, popupActions);
        DiffImplUtil.addActionBlock(myPopupActionGroup, ActionManager.getInstance().getAction("Vcs.Diff.EnableDisableCombinedDiff"));
        ShowActionGroupPopupAction action = new ShowActionGroupPopupAction(() -> myPopupActionGroup);
        UnifiedDiffUtil.registerAction(action, myMainPanel);
        myRegisteredActions.add(action);
    }

    @RequiredUIAccess
    private void clear() {
        myCombinedDiffUIState.reset();
        myContentPanel.set((Component) null);
        myMainToolbar.clear();
        myPopupActionGroup.removeAll();
        for (AnAction action : myRegisteredActions) {
            action.unregisterCustomShortcutSet(myMainPanel);
        }
        myRegisteredActions.clear();
    }

    @Override
    public void dispose() {
        myDisposed = true;
    }

    private void uiDataSnapshot(DataSink sink) {
        sink.set(DiffDataKeys.DIFF_REQUEST, getCurrentRequest());
        sink.set(Project.KEY, myContext.getProject());
        sink.set(DiffDataKeys.DIFF_CONTEXT, myContext);
        UiDataProvider contextProvider = myContext.getUserData(DiffUserDataKeys.DATA_PROVIDER);
        if (contextProvider != null) {
            contextProvider.uiDataSnapshot(sink);
        }
        DiffRequest request = getCurrentRequest();
        UiDataProvider requestProvider = request != null ? request.getUserData(DiffUserDataKeys.DATA_PROVIDER) : null;
        if (requestProvider != null) {
            requestProvider.uiDataSnapshot(sink);
        }
        CombinedDiffViewer viewer = myContext.getUserData(CombinedDiffViewerKeys.COMBINED_DIFF_VIEWER_KEY);
        if (viewer != null) {
            viewer.uiDataSnapshot(sink);
        }
    }

    public @Nullable DiffRequest getCurrentRequest() {
        CombinedDiffViewer combinedViewer = myContext.getUserData(CombinedDiffViewerKeys.COMBINED_DIFF_VIEWER_KEY);
        if (combinedViewer == null) {
            return null;
        }
        return myModel.getLoadedRequest(combinedViewer.getCurrentBlockId());
    }

    public CombinedDiffUIState getUiState() {
        return myCombinedDiffUIState;
    }

    static final class MyDiffToolChooser extends DiffToolChooser {
        private final DiffContext myContext;
        private final CombinedDiffModel myModel;
        private final DiffSettingsHolder.DiffSettings mySettings;

        private final List<CombinedDiffTool> myAvailableTools = new ArrayList<>();
        private final List<CombinedDiffTool> myCombinedToolOrder = new ArrayList<>();

        private CombinedDiffTool myActiveTool;

        MyDiffToolChooser(DiffContext context, CombinedDiffModel model, DiffSettingsHolder.DiffSettings settings) {
            super(context.getProject());
            myContext = context;
            myModel = model;
            mySettings = settings;

            for (DiffTool tool : DiffManagerEx.getInstanceEx().getDiffTools()) {
                if (tool instanceof CombinedDiffTool combinedDiffTool) {
                    myAvailableTools.add(combinedDiffTool);
                }
            }

            updateAvailableDiffTools();
            myActiveTool = !myCombinedToolOrder.isEmpty() ? myCombinedToolOrder.get(0) : myAvailableTools.get(0);
        }

        private void updateAvailableDiffTools() {
            myCombinedToolOrder.clear();
            myCombinedToolOrder.addAll(DiffRequestProcessor.getToolOrderFromSettings(mySettings, myAvailableTools));
        }

        private void moveToolOnTop(CombinedDiffTool tool) {
            if (myCombinedToolOrder.remove(tool)) {
                myCombinedToolOrder.add(0, tool);
                updateToolOrderSettings(myCombinedToolOrder);
            }
        }

        private void updateToolOrderSettings(List<? extends DiffTool> toolOrder) {
            List<String> savedOrder = new ArrayList<>();
            for (DiffTool tool : toolOrder) {
                savedOrder.add(tool.getClass().getCanonicalName());
            }
            mySettings.setDiffToolsOrder(savedOrder);
        }

        @RequiredUIAccess
        @Override
        public void onSelected(@Nullable Project project, DiffTool diffTool) {
            if (!(diffTool instanceof CombinedDiffTool combinedDiffTool)) {
                return;
            }

            myActiveTool = combinedDiffTool;

            moveToolOnTop(combinedDiffTool);
            myModel.reload();
        }

        @Override
        public List<CombinedDiffTool> getTools() {
            return List.copyOf(myAvailableTools);
        }

        @Override
        public DiffTool getActiveTool() {
            return myActiveTool;
        }

        @Override
        public @Nullable DiffTool getForcedDiffTool() {
            return null;
        }
    }
}
