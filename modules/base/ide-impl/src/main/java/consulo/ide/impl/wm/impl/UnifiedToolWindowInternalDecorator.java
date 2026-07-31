/*
 * Copyright 2013-2026 consulo.io
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
package consulo.ide.impl.wm.impl;

import consulo.application.dumb.DumbAware;
import consulo.dataContext.UiDataProvider;
import consulo.disposer.Disposer;
import consulo.ide.impl.idea.ide.actions.ToggleToolbarAction;
import consulo.ide.impl.idea.util.EventDispatcher;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.project.ui.impl.internal.wm.ToolWindowBase;
import consulo.project.ui.internal.WindowInfoImpl;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.Component;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.localize.UILocalize;
import consulo.ui.ex.toolWindow.*;
import consulo.ui.layout.DockLayout;
import consulo.util.lang.StringUtil;
import org.jspecify.annotations.Nullable;

/**
 * Internal decorator of a tool window for the frontends which render the unified {@link consulo.ui} components
 * rather than swing. Analog of {@code consulo.desktop.awt.wm.impl.DesktopInternalDecorator} - it owns the header
 * and builds the popup group the header and its context menu are made of.
 *
 * @author VISTALL
 * @since 2026-07-31
 */
public class UnifiedToolWindowInternalDecorator implements ToolWindowInternalDecorator {
    private final Project myProject;
    private final ToolWindowBase myToolWindow;
    private final EventDispatcher<InternalDecoratorListener> myDispatcher = EventDispatcher.create(InternalDecoratorListener.class);

    private final TogglePinnedModeAction myToggleAutoHideModeAction = new TogglePinnedModeAction();
    private final ToggleDockModeAction myToggleDockModeAction = new ToggleDockModeAction();
    private final ToggleFloatingModeAction myToggleFloatingModeAction = new ToggleFloatingModeAction();
    private final ToggleWindowedModeAction myToggleWindowedModeAction = new ToggleWindowedModeAction();
    private final ToggleSideModeAction myToggleSideModeAction = new ToggleSideModeAction();
    private final ToggleContentUiTypeAction myToggleContentUiTypeAction = new ToggleContentUiTypeAction();
    private final RemoveStripeButtonAction myHideStripeButtonAction = new RemoveStripeButtonAction();
    private final ActionGroup myToggleToolbarGroup;

    private final DockLayout myLayout = DockLayout.create(0);
    private final UnifiedToolWindowHeader myHeader;

    private WindowInfoImpl myInfo;
    private @Nullable ActionGroup myAdditionalGearActions;

    @RequiredUIAccess
    public UnifiedToolWindowInternalDecorator(Project project, WindowInfoImpl info, ToolWindowBase toolWindow) {
        myProject = project;
        myInfo = info;
        myToolWindow = toolWindow;
        myToolWindow.setDecorator(this);

        myToggleToolbarGroup = ToggleToolbarAction.createToggleToolbarGroup(project, toolWindow);

        myHeader = new UnifiedToolWindowHeader(toolWindow, this, () -> createPopupGroup(true));

        myLayout.putUserData(UiDataProvider.KEY, sink -> sink.set(ToolWindow.KEY, myToolWindow));

        myLayout.top(myHeader.getComponent());
        myLayout.center(toolWindow.getUIComponent());
    }

    public Component getComponent() {
        return myLayout;
    }

    public UnifiedToolWindowHeader getHeader() {
        return myHeader;
    }

    @Override
    public WindowInfo getWindowInfo() {
        return myInfo;
    }

    @Override
    @RequiredUIAccess
    public void apply(WindowInfo windowInfo) {
        if (myProject.isDisposed()) {
            return;
        }

        myInfo = (WindowInfoImpl) windowInfo;

        myHeader.updateActionsAsync();
    }

    @Override
    public ToolWindow getToolWindow() {
        return myToolWindow;
    }

    @Override
    public void addInternalDecoratorListener(InternalDecoratorListener l) {
        myDispatcher.addListener(l);
    }

    @Override
    public void removeInternalDecoratorListener(InternalDecoratorListener l) {
        myDispatcher.removeListener(l);
    }

    @Override
    public void fireActivated() {
        myDispatcher.getMulticaster().activated(this);
    }

    @Override
    public void fireHidden() {
        myDispatcher.getMulticaster().hidden(this);
    }

    @Override
    public void fireHiddenSide() {
        myDispatcher.getMulticaster().hiddenSide(this);
    }

    private void fireAnchorChanged(ToolWindowAnchor anchor) {
        myDispatcher.getMulticaster().anchorChanged(this, anchor);
    }

    private void fireAutoHideChanged(boolean autoHide) {
        myDispatcher.getMulticaster().autoHideChanged(this, autoHide);
    }

    private void fireTypeChanged(ToolWindowType type) {
        myDispatcher.getMulticaster().typeChanged(this, type);
    }

    private void fireSideStatusChanged(boolean isSide) {
        myDispatcher.getMulticaster().sideStatusChanged(this, isSide);
    }

    private void fireContentUiTypeChanges(ToolWindowContentUiType type) {
        myDispatcher.getMulticaster().contentUiTypeChanges(this, type);
    }

    private void fireVisibleOnPanelChanged(boolean visibleOnPanel) {
        myDispatcher.getMulticaster().visibleStripeButtonChanged(this, visibleOnPanel);
    }

    @Override
    @RequiredUIAccess
    public void setTitleActions(AnAction... actions) {
        myHeader.setAdditionalTitleActions(actions);
    }

    @Override
    @RequiredUIAccess
    public void setTabActions(AnAction... actions) {
        myHeader.setTabActions(actions);
    }

    @Override
    public void setAdditionalGearActions(@Nullable ActionGroup additionalGearActions) {
        myAdditionalGearActions = additionalGearActions;
    }

    @Override
    public final ActionGroup createPopupGroup() {
        return createPopupGroup(false);
    }

    public final ActionGroup createPopupGroup(boolean skipHideAction) {
        DefaultActionGroup group = createGearPopupGroup();

        if (!ToolWindowId.PREVIEW.equals(myInfo.getId())) {
            group.add(myToggleContentUiTypeAction);
        }

        DefaultActionGroup moveGroup = new DefaultActionGroup(UILocalize.toolWindowMoveToActionGroupName(), true);
        ToolWindowAnchor anchor = myInfo.getAnchor();
        if (anchor != ToolWindowAnchor.TOP) {
            moveGroup.add(new ChangeAnchorAction(UILocalize.toolWindowMoveToTopActionName(), ToolWindowAnchor.TOP));
        }
        if (anchor != ToolWindowAnchor.LEFT) {
            moveGroup.add(new ChangeAnchorAction(UILocalize.toolWindowMoveToLeftActionName(), ToolWindowAnchor.LEFT));
        }
        if (anchor != ToolWindowAnchor.BOTTOM) {
            moveGroup.add(new ChangeAnchorAction(UILocalize.toolWindowMoveToBottomActionName(), ToolWindowAnchor.BOTTOM));
        }
        if (anchor != ToolWindowAnchor.RIGHT) {
            moveGroup.add(new ChangeAnchorAction(UILocalize.toolWindowMoveToRightActionName(), ToolWindowAnchor.RIGHT));
        }
        group.add(moveGroup);

        if (!skipHideAction) {
            group.addSeparator();
            group.add(new HideAction());
        }

        return group;
    }

    private DefaultActionGroup createGearPopupGroup() {
        DefaultActionGroup group = new DefaultActionGroup();

        ActionGroup additionalGearActions = myAdditionalGearActions;
        if (additionalGearActions != null) {
            if (additionalGearActions.isPopup() && !StringUtil.isEmpty(additionalGearActions.getTemplatePresentation().getText())) {
                group.add(additionalGearActions);
            }
            else {
                addSorted(group, additionalGearActions);
            }
            group.addSeparator();
        }

        group.addAction(myToggleToolbarGroup);

        if (myInfo.isDocked()) {
            group.add(myToggleAutoHideModeAction);
            group.add(myToggleDockModeAction);
            group.add(myToggleFloatingModeAction);
            group.add(myToggleWindowedModeAction);
            group.add(myToggleSideModeAction);
        }
        else if (myInfo.isFloating()) {
            group.add(myToggleAutoHideModeAction);
            group.add(myToggleFloatingModeAction);
            group.add(myToggleWindowedModeAction);
        }
        else if (myInfo.isWindowed()) {
            group.add(myToggleFloatingModeAction);
            group.add(myToggleWindowedModeAction);
        }
        else if (myInfo.isSliding()) {
            if (!ToolWindowId.PREVIEW.equals(myInfo.getId())) {
                group.add(myToggleDockModeAction);
            }
            group.add(myToggleFloatingModeAction);
            group.add(myToggleWindowedModeAction);
            group.add(myToggleSideModeAction);
        }

        group.add(myHideStripeButtonAction);

        return group;
    }

    private static void addSorted(DefaultActionGroup main, ActionGroup group) {
        AnAction[] children = group.getChildren(null);
        main.addAll(children);

        String separatorText = group.getTemplatePresentation().getText();
        if (children.length > 0 && !StringUtil.isEmpty(separatorText)) {
            main.addAction(AnSeparator.create(separatorText), Constraints.FIRST);
        }
    }

    @Override
    public boolean isFocused() {
        return false;
    }

    @Override
    public boolean hasFocus() {
        return false;
    }

    @Override
    public int getHeaderHeight() {
        return UnifiedToolWindowHeader.HEIGHT;
    }

    @Override
    public void dispose() {
        Disposer.dispose(myHeader);
    }

    private final class ChangeAnchorAction extends AnAction implements DumbAware {
        private final ToolWindowAnchor myAnchor;

        private ChangeAnchorAction(LocalizeValue title, ToolWindowAnchor anchor) {
            super(title);
            myAnchor = anchor;
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            fireAnchorChanged(myAnchor);
        }
    }

    private final class TogglePinnedModeAction extends ToggleAction implements DumbAware {
        private TogglePinnedModeAction() {
            copyFrom(ActionManager.getInstance().getAction(TOGGLE_PINNED_MODE_ACTION_ID));
        }

        @Override
        public boolean isSelected(AnActionEvent event) {
            return !myInfo.isAutoHide();
        }

        @Override
        public void setSelected(AnActionEvent event, boolean flag) {
            fireAutoHideChanged(!myInfo.isAutoHide());
        }

        @Override
        public void update(AnActionEvent e) {
            super.update(e);
            e.getPresentation().setVisible(myInfo.getType() != ToolWindowType.FLOATING && myInfo.getType() != ToolWindowType.WINDOWED);
        }
    }

    private final class ToggleDockModeAction extends ToggleAction implements DumbAware {
        private ToggleDockModeAction() {
            copyFrom(ActionManager.getInstance().getAction(TOGGLE_DOCK_MODE_ACTION_ID));
        }

        @Override
        public boolean isSelected(AnActionEvent event) {
            return myInfo.isDocked();
        }

        @Override
        public void setSelected(AnActionEvent event, boolean flag) {
            if (myInfo.isDocked()) {
                fireTypeChanged(ToolWindowType.SLIDING);
            }
            else if (myInfo.isSliding()) {
                fireTypeChanged(ToolWindowType.DOCKED);
            }
        }
    }

    private final class ToggleFloatingModeAction extends ToggleAction implements DumbAware {
        private ToggleFloatingModeAction() {
            copyFrom(ActionManager.getInstance().getAction(TOGGLE_FLOATING_MODE_ACTION_ID));
        }

        @Override
        public boolean isSelected(AnActionEvent event) {
            return myInfo.isFloating();
        }

        @Override
        public void setSelected(AnActionEvent event, boolean flag) {
            fireTypeChanged(myInfo.isFloating() ? myInfo.getInternalType() : ToolWindowType.FLOATING);
        }
    }

    private final class ToggleWindowedModeAction extends ToggleAction implements DumbAware {
        private ToggleWindowedModeAction() {
            copyFrom(ActionManager.getInstance().getAction(TOGGLE_WINDOWED_MODE_ACTION_ID));
        }

        @Override
        public boolean isSelected(AnActionEvent event) {
            return myInfo.isWindowed();
        }

        @Override
        public void setSelected(AnActionEvent event, boolean flag) {
            fireTypeChanged(myInfo.isWindowed() ? myInfo.getInternalType() : ToolWindowType.WINDOWED);
        }

        @Override
        public void update(AnActionEvent e) {
            super.update(e);
            if (Platform.current().os().isMac()) {
                e.getPresentation().setEnabledAndVisible(false);
            }
        }
    }

    private final class ToggleSideModeAction extends ToggleAction implements DumbAware {
        private ToggleSideModeAction() {
            copyFrom(ActionManager.getInstance().getAction(TOGGLE_SIDE_MODE_ACTION_ID));
        }

        @Override
        public boolean isSelected(AnActionEvent event) {
            return myInfo.isSplit();
        }

        @Override
        public void setSelected(AnActionEvent event, boolean flag) {
            fireSideStatusChanged(flag);
        }
    }

    private final class ToggleContentUiTypeAction extends ToggleAction implements DumbAware {
        private boolean myHadSeveralContents;

        private ToggleContentUiTypeAction() {
            copyFrom(ActionManager.getInstance().getAction(TOGGLE_CONTENT_UI_TYPE_ACTION_ID));
        }

        @Override
        public void update(AnActionEvent e) {
            myHadSeveralContents = myHadSeveralContents || myToolWindow.getContentManager().getContentCount() > 1;
            super.update(e);
            e.getPresentation().setVisible(myHadSeveralContents);
        }

        @Override
        public boolean isSelected(AnActionEvent e) {
            return myInfo.getContentUiType() == ToolWindowContentUiType.COMBO;
        }

        @Override
        public void setSelected(AnActionEvent e, boolean state) {
            fireContentUiTypeChanges(state ? ToolWindowContentUiType.COMBO : ToolWindowContentUiType.TABBED);
        }
    }

    private final class RemoveStripeButtonAction extends LegacyDumbAwareAction {
        private RemoveStripeButtonAction() {
            Presentation presentation = getTemplatePresentation();
            presentation.setText(ActionLocalize.actionRemovestripebuttonText());
            presentation.setDescription(ActionLocalize.actionRemovestripebuttonDescription());
        }

        @Override
        public void update(AnActionEvent e) {
            e.getPresentation().setEnabledAndVisible(myInfo.isShowStripeButton());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            fireVisibleOnPanelChanged(false);
            if (myToolWindow.isActive()) {
                fireHidden();
            }
        }
    }

    private final class HideAction extends LegacyDumbAwareAction {
        private HideAction() {
            copyFrom(ActionManager.getInstance().getAction(HIDE_ACTIVE_WINDOW_ACTION_ID));
            getTemplatePresentation().setText(UILocalize.toolWindowHideActionName());
        }

        @Override
        @RequiredUIAccess
        public void actionPerformed(AnActionEvent e) {
            fireHidden();
        }

        @Override
        public void update(AnActionEvent event) {
            event.getPresentation().setEnabled(myInfo.isVisible());
        }
    }
}
