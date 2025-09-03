// Copyright 2000-2022 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package consulo.desktop.awt.os.mac.internal.touchBar;

import consulo.ide.impl.idea.ide.ui.customization.CustomisedActionGroup;
import consulo.ide.impl.idea.openapi.wm.impl.welcomeScreen.WelcomePopupAction;
import consulo.ide.localize.IdeLocalize;
import consulo.logging.Logger;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.project.ui.wm.ToolWindowId;
import consulo.ui.ex.action.*;
import consulo.util.lang.Pair;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import java.awt.event.InputEvent;
import java.util.HashMap;
import java.util.Map;

final class ActionsLoader {
    private static final Logger LOG = Logger.getInstance(ActionsLoader.class);
    private static final boolean ENABLE_FN_MODE = Boolean.getBoolean("touchbar.fn.mode.enable");
    private static int FN_WIDTH = Integer.getInteger("touchbar.fn.width", 68);

    private static final boolean TOOLWINDOW_CROSS_ESC = !Boolean.getBoolean("touchbar.toolwindow.esc");
    private static final boolean TOOLWINDOW_EMULATE_ESC = Boolean.getBoolean("touchbar.toolwindow.emulateesc");
    private static final boolean TOOLWINDOW_PERSISTENT = !Boolean.getBoolean("touchbar.toolwindow.nonpersistent");

    private static final String SETTINS_AUTOCLOSE_KEY = "touchbar.toolwindow.autoclose";
    private static final String DEFAULT_ACTION_GROUP = "TouchBarDefault";

    static {
        FN_WIDTH = Math.max(FN_WIDTH, 50);
        FN_WIDTH = Math.min(FN_WIDTH, 100);
    }

    private static String[] getAutoCloseActions(@Nonnull String toolWindowId) {
        if (toolWindowId.isEmpty()) {
            return null;
        }

        // TODO: read setting from proper place (think where)
        String propVal = System.getProperty(SETTINS_AUTOCLOSE_KEY + '.' + toolWindowId);
        if (propVal == null || propVal.isEmpty()) {
            return getAutoCloseActionsDefault(toolWindowId);
        }

        String[] split = propVal.split(",");
        if (split.length == 0) {
            return null;
        }

        for (int c = 0; c < split.length; ++c) {
            split[c] = split[c].trim();
        }
        return split;
    }

    private static final int RUN_CONFIG_POPOVER_WIDTH = 143;

    static @Nullable Pair<Map<Long, ActionGroup>, Customizer> getProjectDefaultActionGroup() {
        if (ENABLE_FN_MODE) {
            LOG.debug("use FN-actions group for default actions");
            return getFnActionGroup();
        }

        @Nullable Map<Long, ActionGroup> defaultGroup = getActionGroup(DEFAULT_ACTION_GROUP);
        if (defaultGroup == null) {
            return null;
        }

        // Create hardcoded customizer for project-default touchbar
        // TODO: load customizer from xml or settings
        Customizer customizer = new Customizer(
            null /*project-default touchbar never replaces esc-button*/,
            null /*project-default touchbar mustn't be closed because of auto-close actions*/,
            (parentInfo, butt, presentation) -> {
                String actId = ActionManager.getInstance().getId(butt.getAnAction());

                boolean isRunConfigPopover = "RunConfiguration".equals(actId);
                boolean isOpenInTerminalAction = "Terminal.OpenInTerminal".equals(actId) || "Terminal.OpenInReworkedTerminal".equals(actId);
                if (isRunConfigPopover || isOpenInTerminalAction) {
                    butt.setText(presentation.getText());
                    butt.setIconFromPresentation(presentation);
                }
                else {
                    TouchbarActionCustomizations customizations = parentInfo == null ? null : parentInfo.getCustomizations();
                    butt.setIconAndTextFromPresentation(presentation, customizations);
                }

                if (isRunConfigPopover) {
                    if (presentation.getIcon() != PlatformIconGroup.generalAdd()) {
                        butt.setHasArrowIcon(true);
                        butt.setLayout(RUN_CONFIG_POPOVER_WIDTH, 0, 5, 8);
                    }
                    else {
                        butt.setHasArrowIcon(false);
                        butt.setLayout(0, 0, 5, 8);
                    }
                }
                else if (butt.getAnAction() instanceof WelcomePopupAction) {
                    butt.setHasArrowIcon(true);
                }
            });
        return Pair.create(defaultGroup, customizer);
    }

    static @Nullable Pair<Map<Long, ActionGroup>, Customizer> getToolWindowActionGroup(@Nonnull String toolWindowId) {
        if ("Services".equals(toolWindowId)) {
            LOG.debug("Services tool-window will use action-group from debug tool window");
            toolWindowId = "Debug";
        }
        @Nullable Map<Long, ActionGroup> actions = getActionGroup(IdeActions.GROUP_TOUCHBAR + toolWindowId);
        if (actions == null || actions.get(0L) == null) {
            LOG.debug("null action group (or it doesn't contain main-layout) for tool window: %s", toolWindowId);
            return null;
        }

        Customizer customizer = new Customizer(
            TOOLWINDOW_CROSS_ESC ? new TBPanel.CrossEscInfo(TOOLWINDOW_EMULATE_ESC, TOOLWINDOW_PERSISTENT) : null,
            getAutoCloseActions(toolWindowId)
        );
        return Pair.create(actions, customizer);
    }

    static @Nullable Map<Long, ActionGroup> getActionGroup(@Nonnull String groupId) {
        // 1. build full name of group
        String fullGroupId = groupId.startsWith(IdeActions.GROUP_TOUCHBAR) ? groupId : IdeActions.GROUP_TOUCHBAR + groupId;

        // 2. read touchbar-actions from CustomActionsSchema and select proper child
        ActionGroup allTouchbarActions = (ActionGroup) CustomActionsSchema.getInstance().getCorrectedAction(IdeActions.GROUP_TOUCHBAR);
        if (allTouchbarActions == null) {
            LOG.debug("can't create touchbar because ActionGroup isn't defined: %s", IdeActions.GROUP_TOUCHBAR);
            return null;
        }

        ActionManager actionManager = ActionManager.getInstance();
        ActionGroup actionGroup = null;
        for (AnAction act : allTouchbarActions.getChildren(null)) {
            if (!(act instanceof ActionGroup)) {
                continue;
            }
            String actId = actionManager.getId(act instanceof CustomisedActionGroup o ? o.getDelegate() : act);
            if (actId == null || actId.isEmpty()) {
                continue;
            }

            if (actId.equals(fullGroupId)) {
                actionGroup = (ActionGroup) act;
                break;
            }
        }

        // 3. when group wasn't found in CustomActionsSchema just read group from ActionManager
        if (actionGroup == null) {
            LOG.debug("group %s wasn't found in CustomActionsSchema, will obtain it directly from ActionManager", groupId);
            AnAction act = actionManager.getAction(fullGroupId);
            if (!(act instanceof ActionGroup)) {
                LOG.debug("can't create touchbar because corresponding ActionGroup isn't defined: %s", groupId);
                return null;
            }
            actionGroup = (ActionGroup) act;
        }

        // 4. extract main layout and alternative layouts with modifers
        Map<Long, ActionGroup> result = new HashMap<>();
        DefaultActionGroup mainLayout = new DefaultActionGroup();
        mainLayout.getTemplatePresentation().copyFrom(actionGroup.getTemplatePresentation()); // just for convenience debug
        result.put(0L, mainLayout);
        for (AnAction act : actionGroup.getChildren(null)) {
            if (!(act instanceof ActionGroup)) {
                mainLayout.addAction(act);
                continue;
            }
            String gid = actionManager.getId(act instanceof CustomisedActionGroup o ? o.getDelegate() : act);
            if (gid.startsWith(fullGroupId + "_")) {
                long mask = _str2mask(gid.substring(fullGroupId.length() + 1));
                if (mask != 0) {
                    result.put(mask, (ActionGroup) act);
                }
                else {
                    LOG.debug("zero mask for group: %s", fullGroupId);
                }
            }
            else {
                mainLayout.addAction(act);
            }
        }

        return result;
    }

    static @Nonnull Pair<Map<Long, ActionGroup>, Customizer> getFnActionGroup() {
        DefaultActionGroup result = new DefaultActionGroup(IdeLocalize.actionFnKeysText(), false);
        for (int c = 1; c <= 12; ++c) {
            result.add(new FNKeyAction(c));
        }
        Map<Long, ActionGroup> ret = new HashMap<>();
        ret.put(0L, result);

        Customizer customizer = new Customizer(null, null, (parentInfo, butt, presentation) -> {
            if (butt.getAnAction() instanceof FNKeyAction act) {
                butt.setWidth(FN_WIDTH);
                butt.setIcon(null);
                String hint = presentation.getText() == null || presentation.getText().isEmpty() ? " " : presentation.getText();
                butt.setText(String.format("F%d", act.getFN()), hint, act.isActionDisabled());
            }
        });
        return Pair.create(ret, customizer);
    }

    private static long _str2mask(@Nonnull String modifierId) {
        if (!modifierId.contains(".")) {
            if (modifierId.equalsIgnoreCase("alt")) {
                return InputEvent.ALT_DOWN_MASK;
            }
            if (modifierId.equalsIgnoreCase("cmd")) {
                return InputEvent.META_DOWN_MASK;
            }
            if (modifierId.equalsIgnoreCase("ctrl")) {
                return InputEvent.CTRL_DOWN_MASK;
            }
            if (modifierId.equalsIgnoreCase("shift")) {
                return InputEvent.SHIFT_DOWN_MASK;
            }
            return 0;
        }

        String[] spl = modifierId.split("\\.");
        long mask = 0;
        for (String sub : spl) {
            mask |= _str2mask(sub);
        }
        return mask;
    }

    // hardcoded default auto-close actions
    private static String[] getAutoCloseActionsDefault(@Nonnull String toolWindowId) {
        if (
            toolWindowId.equals(ToolWindowId.DEBUG) ||
                toolWindowId.equals(ToolWindowId.RUN) ||
                toolWindowId.equals(ToolWindowId.SERVICES)
        ) {
            return new String[]{"Stop"};
        }
        return null;
    }
}
