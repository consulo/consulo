// Copyright 2000-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.diff.impl.internal.combined;

import consulo.diff.DiffContext;
import consulo.diff.DiffUserDataKeys;
import consulo.diff.impl.internal.DiffToolChooser;
import consulo.diff.impl.internal.action.OpenInEditorAction;
import consulo.diff.impl.internal.util.UnifiedDiffUtil;
import consulo.diff.internal.DiffImplUtil;
import consulo.disposer.Disposable;
import consulo.localize.LocalizeValue;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.ui.Component;
import consulo.ui.Label;
import consulo.ui.Space;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.ActionManager;
import consulo.ui.ex.action.ActionPlaces;
import consulo.ui.ex.action.ActionToolbar;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.DefaultActionGroup;
import consulo.ui.layout.DockLayout;
import consulo.ui.layout.HorizontalLayout;
import consulo.ui.layout.HorizontalLayoutStyle;
import consulo.ui.layout.WrappedLayout;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

final class CombinedDiffMainToolbar {
    private final CombinedDiffUIState myUiState;
    private final Component myTargetComponent;
    private final DiffToolChooser myDiffToolChooser;
    private final DiffContext myContext;
    private final AnAction myGoToChangeAction;

    private final DiffInfoComponent myDiffInfoPanel = new DiffInfoComponent();

    private final DefaultActionGroup myLeftToolbarGroup = new DefaultActionGroup();
    private final ActionToolbar myLeftToolbar;

    private final DefaultActionGroup myRightToolbarGroup = new DefaultActionGroup();
    private final ActionToolbar myRightToolbar;

    private final DockLayout myTopPanel;
    private final WrappedLayout myCenterPanel;
    private final WrappedLayout myRightPanel;

    private final AnAction myOpenInEditorAction = new OpenInEditorAction(null) {
        @Override
        public void updateInReadAction(AnActionEvent e) {
            super.updateInReadAction(e);
            e.getPresentation().setVisible(false);
        }
    };

    @RequiredUIAccess
    CombinedDiffMainToolbar(
        Disposable disposable,
        CombinedDiffUIState uiState,
        Component targetComponent,
        DiffToolChooser diffToolChooser,
        DiffContext context,
        AnAction goToChangeAction
    ) {
        myUiState = uiState;
        myTargetComponent = targetComponent;
        myDiffToolChooser = diffToolChooser;
        myContext = context;
        myGoToChangeAction = goToChangeAction;

        myLeftToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.DIFF_TOOLBAR, myLeftToolbarGroup, true);
        myLeftToolbar.setTargetUIComponent(targetComponent);

        myRightToolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.DIFF_RIGHT_TOOLBAR, myRightToolbarGroup, true);
        myRightToolbar.setTargetUIComponent(targetComponent);

        myCenterPanel = WrappedLayout.create(myDiffInfoPanel.getPanel());
        myRightPanel = WrappedLayout.create(myRightToolbar.getUIComponent());

        myTopPanel = DockLayout.create(Space.NONE);
        myTopPanel.left(myLeftToolbar.getUIComponent());
        myTopPanel.center(myCenterPanel);
        myTopPanel.right(myRightPanel);

        uiState.addDiffInfoListener(myDiffInfoPanel::updateForState, disposable);
        myDiffInfoPanel.updateForState(uiState.getDiffInfoState());
    }

    Component getComponent() {
        return myTopPanel;
    }

    @RequiredUIAccess
    void setSearchComponent(Component searchComponent) {
        myCenterPanel.set(searchComponent);
        myRightPanel.set((Component) null);
    }

    @RequiredUIAccess
    void hideSearch() {
        myCenterPanel.set(myDiffInfoPanel.getPanel());
        myRightPanel.set(myRightToolbar.getUIComponent());
    }

    boolean isFocusedInWindow() {
        return false;
    }

    @Nullable Component getPreferredFocusedComponent() {
        return myLeftToolbar.getUIComponent();
    }

    void clear() {
        myLeftToolbarGroup.removeAll();
        myRightToolbarGroup.removeAll();
    }

    @RequiredUIAccess
    void updateToolbar(@Nullable List<AnAction> toolbarActions) {
        collectToolbarActions(toolbarActions);
        myLeftToolbar.updateActionsImmediately();
        registerShortcuts(myLeftToolbarGroup);
        myRightToolbar.updateActionsImmediately();
        registerShortcuts(myRightToolbarGroup);
    }

    @RequiredUIAccess
    private void registerShortcuts(DefaultActionGroup group) {
        for (AnAction action : group.getChildren(null)) {
            UnifiedDiffUtil.registerAction(action, myTargetComponent);
        }
    }

    private void collectToolbarActions(@Nullable List<AnAction> viewerActions) {
        myLeftToolbarGroup.removeAll();
        myRightToolbarGroup.removeAll();

        myRightToolbarGroup.add(myDiffToolChooser);

        DiffImplUtil.addActionBlock(myLeftToolbarGroup, collectNavigationActions());

        DiffImplUtil.addActionBlock(myRightToolbarGroup, viewerActions);
        List<AnAction> contextActions = myContext.getUserData(DiffUserDataKeys.CONTEXT_ACTIONS);
        DiffImplUtil.addActionBlock(myLeftToolbarGroup, contextActions);
    }

    private List<AnAction> collectNavigationActions() {
        List<AnAction> actions = new ArrayList<>();
        actions.add(new CombinedPrevBlockAction(myContext));
        actions.add(new CombinedPrevDifferenceAction(myContext));
        actions.add(myGoToChangeAction);
        actions.add(new CombinedNextDifferenceAction(myContext));
        actions.add(new CombinedNextBlockAction(myContext));
        actions.add(myOpenInEditorAction);
        return actions;
    }

    private static final class DiffInfoComponent {
        private final Label myLeftTitle = Label.create(LocalizeValue.empty());
        private final Label myRightLabel = Label.create(LocalizeValue.empty());
        private final Label myArrows = Label.create(LocalizeValue.empty());
        private final HorizontalLayout myPanel = HorizontalLayout.create(Space.LARGE);

        @RequiredUIAccess
        private DiffInfoComponent() {
            myArrows.setImage(PlatformIconGroup.diffArrow());
            myPanel.addStyle(HorizontalLayoutStyle.TRANSPARENT_BACKGROUND);
            myPanel.add(myLeftTitle);
            myPanel.add(myArrows);
            myPanel.add(myRightLabel);
        }

        Component getPanel() {
            return myPanel;
        }

        @RequiredUIAccess
        void updateForState(DiffInfoState diffInfo) {
            switch (diffInfo) {
                case EmptyDiffInfoState empty -> {
                    myLeftTitle.setVisible(false);
                    myArrows.setVisible(false);
                    myRightLabel.setVisible(false);
                }
                case SingleTitleDiffInfoState single -> {
                    myLeftTitle.setVisible(true);
                    myArrows.setVisible(false);
                    myRightLabel.setVisible(false);
                    myLeftTitle.setText(LocalizeValue.of(single.title()));
                }
                case TwoTitlesDiffInfoState two -> {
                    myLeftTitle.setVisible(true);
                    myArrows.setVisible(true);
                    myRightLabel.setVisible(true);
                    myLeftTitle.setText(LocalizeValue.of(two.leftTitle()));
                    myRightLabel.setText(LocalizeValue.of(two.rightTitle()));
                }
            }
        }
    }
}
