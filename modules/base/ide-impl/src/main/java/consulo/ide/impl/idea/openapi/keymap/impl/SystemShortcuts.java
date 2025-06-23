// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.keymap.impl;

import consulo.application.Application;
import consulo.application.util.SystemInfo;
import consulo.application.util.registry.Registry;
import consulo.ide.impl.idea.ide.util.PropertiesComponent;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.ActionsTreeUtil;
import consulo.ide.impl.idea.openapi.keymap.impl.ui.KeymapPanel;
import consulo.ide.impl.idea.util.ArrayUtilRt;
import consulo.ide.impl.idea.util.ReflectionUtil;
import consulo.localize.LocalizeValue;
import consulo.logging.Logger;
import consulo.platform.Platform;
import consulo.process.ExecutionException;
import consulo.process.cmd.GeneralCommandLine;
import consulo.process.util.CapturingProcessUtil;
import consulo.project.ui.notification.Notification;
import consulo.project.ui.notification.NotificationDisplayType;
import consulo.project.ui.notification.NotificationGroup;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.awt.UIExAWTDataKey;
import consulo.ui.ex.keymap.Keymap;
import consulo.util.lang.ref.SimpleReference;
import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.reflect.Method;
import java.util.List;
import java.util.*;
import java.util.function.Predicate;
import java.util.function.Supplier;

public final class SystemShortcuts {
    public static final NotificationGroup SYSTEM_SHORTCUTS_CONFLICTS_GROUP = new NotificationGroup(
        "System shortcuts conflicts",
        NotificationDisplayType.STICKY_BALLOON,
        true
    );

    private static final Logger LOG = Logger.getInstance(SystemShortcuts.class);

    @Nonnull
    private static final String ourUnknownSysAction = "Unknown action";

    @Nonnull
    private final Map<KeyStroke, AWTKeyStroke> myKeyStroke2SysShortcut = new HashMap<>();
    @Nonnull
    private final MuteConflictsSettings myMutedConflicts = new MuteConflictsSettings();
    @Nonnull
    private final Set<String> myNotifiedActions = new HashSet<>();

    private
    @Nullable
    Keymap myKeymap;

    @Nonnull
    private final Map<AWTKeyStroke, ConflictItem> myKeymapConflicts = new HashMap<>();

    public SystemShortcuts() {
        readSystem();
    }

    public static final class ConflictItem {
        final
        @Nonnull
        String mySysActionDesc;
        final
        @Nonnull
        KeyStroke mySysKeyStroke;
        final
        @Nonnull
        String[] myActionIds;

        public ConflictItem(@Nonnull KeyStroke sysKeyStroke, @Nonnull String sysActionDesc, @Nonnull String[] actionIds) {
            mySysKeyStroke = sysKeyStroke;
            mySysActionDesc = sysActionDesc;
            myActionIds = actionIds;
        }

        @Nonnull
        public String getSysActionDesc() {
            return mySysActionDesc;
        }

        @Nonnull
        public KeyStroke getSysKeyStroke() {
            return mySysKeyStroke;
        }

        @Nonnull
        public String[] getActionIds() {
            return myActionIds;
        }

        @Nullable
        String getUnmutedActionId(@Nonnull MuteConflictsSettings settings) {
            for (String actId : myActionIds) {
                if (!settings.isMutedAction(actId)) {
                    return actId;
                }
            }
            return null;
        }
    }

    @RequiredUIAccess
    public void updateKeymapConflicts(@Nullable Keymap keymap) {
        myKeymap = keymap;
        myKeymapConflicts.clear();

        if (myKeyStroke2SysShortcut.isEmpty()) {
            return;
        }

        for (@Nonnull KeyStroke sysKS : myKeyStroke2SysShortcut.keySet()) {
            String[] actIds = computeOnEdt(() -> keymap.getActionIds(sysKS));
            if (actIds == null || actIds.length == 0) {
                continue;
            }

            @Nonnull AWTKeyStroke shk = myKeyStroke2SysShortcut.get(sysKS);
            myKeymapConflicts.put(shk, new ConflictItem(sysKS, getDescription(shk), actIds));
        }
    }

    @Nonnull
    public Collection<ConflictItem> getUnmutedKeymapConflicts() {
        List<ConflictItem> result = new ArrayList<>();
        myKeymapConflicts.forEach((ks, ci) -> {
            if (ci.getUnmutedActionId(myMutedConflicts) != null) {
                result.add(ci);
            }
        });
        return result;
    }

    @Nullable
    public Predicate<AnAction> createKeymapConflictsActionFilter() {
        if (myKeyStroke2SysShortcut.isEmpty() || myKeymap == null) {
            return null;
        }

        Predicate<Shortcut> predicate = sc -> {
            if (sc == null) {
                return false;
            }
            for (KeyStroke ks : myKeyStroke2SysShortcut.keySet()) {
                if (sc.startsWith(new KeyboardShortcut(ks, null))) {
                    ConflictItem ci = myKeymapConflicts.get(myKeyStroke2SysShortcut.get(ks));
                    if (ci != null && ci.getUnmutedActionId(myMutedConflicts) != null) {
                        return true;
                    }
                }
            }
            return false;
        };
        return ActionsTreeUtil.isActionFiltered(ActionManager.getInstance(), myKeymap, predicate);
    }

    @Nullable
    @RequiredUIAccess
    public Map<KeyboardShortcut, String> calculateConflicts(@Nonnull Keymap keymap, @Nonnull String actionId) {
        if (myKeyStroke2SysShortcut.isEmpty()) {
            return null;
        }

        Map<KeyboardShortcut, String> result = null;
        Shortcut[] actionShortcuts = computeOnEdt(() -> keymap.getShortcuts(actionId));
        for (Shortcut sc : actionShortcuts) {
            if (!(sc instanceof KeyboardShortcut ksc)) {
                continue;
            }
            for (@Nonnull KeyStroke sks : myKeyStroke2SysShortcut.keySet()) {
                if (ksc.getFirstKeyStroke().equals(sks) || sks.equals(ksc.getSecondKeyStroke())) {
                    if (result == null) {
                        result = new HashMap<>();
                    }
                    result.put(ksc, getDescription(myKeyStroke2SysShortcut.get(sks)));
                }
            }
        }
        return result;
    }

    @RequiredUIAccess
    private static <T> T computeOnEdt(Supplier<T> supplier) {
        Application app = Application.get();
        if (app.isDispatchThread()) {
            return supplier.get();
        }

        SimpleReference<T> result = SimpleReference.create();
        app.invokeAndWait(() -> result.set(supplier.get()));
        return result.get();
    }

    @Nullable
    public Map<KeyStroke, String> createKeystroke2SysShortcutMap() {
        if (myKeyStroke2SysShortcut.isEmpty()) {
            return null;
        }

        Map<KeyStroke, String> result = new HashMap<>();
        myKeyStroke2SysShortcut.forEach((ks, sysks) -> result.put(ks, getDescription(sysks)));
        return result;
    }

    private int getUnmutedConflictsCount() {
        if (myKeymapConflicts.isEmpty()) {
            return 0;
        }
        int result = 0;
        for (ConflictItem ci : myKeymapConflicts.values()) {
            if (ci.getUnmutedActionId(myMutedConflicts) != null) {
                result++;
            }
        }
        return result;
    }

    public void onUserPressedShortcut(@Nonnull Keymap keymap, @Nonnull String[] actionIds, @Nonnull KeyboardShortcut ksc) {
        if (actionIds.length == 0) {
            return;
        }

        KeyStroke ks = ksc.getFirstKeyStroke();
        AWTKeyStroke sysKs = myKeyStroke2SysShortcut.get(ks);
        if (sysKs == null && ksc.getSecondKeyStroke() != null) {
            sysKs = myKeyStroke2SysShortcut.get(ks = ksc.getSecondKeyStroke());
        }
        if (sysKs == null) {
            return;
        }

        String unmutedActId = null;
        for (String actId : actionIds) {
            if (myNotifiedActions.contains(actId)) {
                continue;
            }
            if (!myMutedConflicts.isMutedAction(actId)) {
                unmutedActId = actId;
                break;
            }
        }
        if (unmutedActId == null) {
            return;
        }

        @Nullable String macOsShortcutAction = getDescription(sysKs);
        //System.out.println(actionId + " shortcut '" + sysKS + "' "
        //                   + Arrays.toString(actionShortcuts) + " conflicts with macOS shortcut"
        //                   + (macOsShortcutAction == null ? "." : " '" + macOsShortcutAction + "'."));
        doNotify(keymap, unmutedActId, ks, macOsShortcutAction, ksc);
    }

    private void doNotify(
        @Nonnull Keymap keymap,
        @Nonnull String actionId,
        @Nonnull KeyStroke sysKS,
        @Nullable String macOsShortcutAction,
        @Nonnull KeyboardShortcut conflicted
    ) {
        AnAction act = ActionManager.getInstance().getAction(actionId);
        String actText = act == null ? actionId : act.getTemplateText();

        Notification.Builder notificationBuilder = SYSTEM_SHORTCUTS_CONFLICTS_GROUP.newWarning()
            .title(LocalizeValue.localizeTODO("Shortcuts conflicts"))
            .content(LocalizeValue.localizeTODO(
                "The " + actText +
                    " shortcut conflicts with macOS shortcut" +
                    (macOsShortcutAction == null ? "" : " '" + macOsShortcutAction + "'") +
                    ". Modify this shortcut or change macOS system settings."
            ))
            .addClosingAction(
                LocalizeValue.localizeTODO("Modify shortcut"),
                e -> {
                    Component component = e.getDataContext().getData(UIExAWTDataKey.CONTEXT_COMPONENT);
                    if (component == null) {
                        Window[] frames = Window.getWindows();
                        component = frames == null || frames.length == 0 ? null : frames[0];
                        if (component == null) {
                            LOG.error("can't show KeyboardShortcutDialog (parent component wasn't found)");
                            return;
                        }
                    }

                    KeymapPanel.addKeyboardShortcut(
                        actionId,
                        ActionShortcutRestrictions.getInstance().getForActionId(actionId),
                        keymap,
                        component,
                        conflicted,
                        SystemShortcuts.this
                    );
                }
            )
            .addAction(LocalizeValue.localizeTODO("Don't show again"), () -> myMutedConflicts.addMutedAction(actionId));

        if (Platform.current().os().isMac()) {
            notificationBuilder.addAction(
                LocalizeValue.localizeTODO("Change system settings"),
                () -> Application.get().executeOnPooledThread(() -> {
                    GeneralCommandLine cmdLine = new GeneralCommandLine(
                        "osascript",
                        "-e",
                        "tell application \"System Preferences\"",
                        "-e",
                        "set the current pane to pane id \"com.apple.preference.keyboard\"",
                        "-e",
                        "reveal anchor \"shortcutsTab\" of pane id \"com.apple.preference.keyboard\"",
                        "-e",
                        "activate",
                        "-e",
                        "end tell"
                    );
                    try {
                        CapturingProcessUtil.execAndGetOutput(cmdLine);
                        // NOTE: we can't detect OS-settings changes
                        // but we can try to schedule check conflicts (and expire notification if necessary)
                    }
                    catch (ExecutionException ex) {
                        LOG.error(ex);
                    }
                })
            );
        }

        myNotifiedActions.add(actionId);
        notificationBuilder.notify(null);
    }

    private static Class ourShkClass;
    private static Method ourMethodGetDescription;
    private static Method ourMethodReadSystemHotkeys;

    private static
    @Nonnull
    String getDescription(@Nonnull AWTKeyStroke systemHotkey) {
        if (ourShkClass == null) {
            ourShkClass = ReflectionUtil.forName("java.awt.desktop.SystemHotkey");
        }
        if (ourShkClass == null) {
            return ourUnknownSysAction;
        }

        if (ourMethodGetDescription == null) {
            ourMethodGetDescription = ReflectionUtil.getMethod(ourShkClass, "getDescription");
        }
        String result = null;
        try {
            result = (String)ourMethodGetDescription.invoke(systemHotkey);
        }
        catch (Throwable e) {
            Logger.getInstance(SystemShortcuts.class).error(e);
        }
        return result == null ? ourUnknownSysAction : result;
    }

    private static final boolean DEBUG_SYSTEM_SHORTCUTS = Boolean.getBoolean("debug.system.shortcuts");

    private void readSystem() {
        myKeyStroke2SysShortcut.clear();

        if (!Platform.current().os().isMac() || !SystemInfo.isJetBrainsJvm) {
            return;
        }

        try {
            if (!Registry.is("read.system.shortcuts")) {
                return;
            }

            if (ourShkClass == null) {
                ourShkClass = ReflectionUtil.forName("java.awt.desktop.SystemHotkey");
            }
            if (ourShkClass == null) {
                return;
            }

            if (ourMethodReadSystemHotkeys == null) {
                ourMethodReadSystemHotkeys = ReflectionUtil.getMethod(ourShkClass, "readSystemHotkeys");
            }
            if (ourMethodReadSystemHotkeys == null) {
                return;
            }

            List<AWTKeyStroke> all = (List<AWTKeyStroke>)ourMethodReadSystemHotkeys.invoke(ourShkClass);
            if (all == null || all.isEmpty()) {
                return;
            }

            String debugInfo = "";
            for (AWTKeyStroke shk : all) {
                if (shk.getModifiers() == 0) {
                    //System.out.println("Skip system shortcut [without modifiers]: " + shk);
                    continue;
                }
                if (shk.getKeyChar() == KeyEvent.CHAR_UNDEFINED && shk.getKeyCode() == KeyEvent.VK_UNDEFINED) {
                    //System.out.println("Skip system shortcut [undefined key]: " + shk);
                    continue;
                }
                if ("Move focus to the next window in application".equals(getDescription(shk))) {
                    // Skip this shortcut because it handled in IDE-side
                    // see: JBR-1515 Regression test jb/sun/awt/macos/MoveFocusShortcutTest.java fails on macOS  (Now we prevent Mac OS from handling the shortcut. We can enumerate windows on IDE level.)
                    continue;
                }

                KeyStroke sysKS;
                if (shk.getKeyChar() != KeyEvent.CHAR_UNDEFINED) {
                    int keyCode = KeyEvent.getExtendedKeyCodeForChar(shk.getKeyChar());
                    if (keyCode == KeyEvent.VK_UNDEFINED) {
                        //System.out.println("Skip system shortcut [undefined key]: " + shk);
                        continue;
                    }
                    sysKS = KeyStroke.getKeyStroke(keyCode, shk.getModifiers());
                }
                else {
                    sysKS = KeyStroke.getKeyStroke(shk.getKeyCode(), shk.getModifiers());
                }

                myKeyStroke2SysShortcut.put(sysKS, shk);

                if (DEBUG_SYSTEM_SHORTCUTS) {
                    debugInfo += shk.toString() + ";\n";
                }
            }
            if (DEBUG_SYSTEM_SHORTCUTS) {
                Logger.getInstance(SystemShortcuts.class).info("system shortcuts:\n" + debugInfo);
            }
        }
        catch (Throwable e) {
            Logger.getInstance(SystemShortcuts.class).debug(e);
        }
    }

    private static class MuteConflictsSettings {
        private static final String MUTED_ACTIONS_KEY = "muted.system.shortcut.conflicts.actions";
        private
        @Nonnull
        Set<String> myMutedActions;

        void init() {
            if (myMutedActions != null) {
                return;
            }
            myMutedActions = new HashSet<>();
            String[] muted = PropertiesComponent.getInstance().getValues(MUTED_ACTIONS_KEY);
            if (muted != null) {
                Collections.addAll(myMutedActions, muted);
            }
        }

        void addMutedAction(@Nonnull String actId) {
            init();
            myMutedActions.add(actId);
            PropertiesComponent.getInstance().setValues(MUTED_ACTIONS_KEY, ArrayUtilRt.toStringArray(myMutedActions));
        }

        void removeMutedAction(@Nonnull String actId) {
            init();
            myMutedActions.remove(actId);
            PropertiesComponent.getInstance().setValues(MUTED_ACTIONS_KEY, ArrayUtilRt.toStringArray(myMutedActions));
        }

        public boolean isMutedAction(@Nonnull String actionId) {
            init();
            return myMutedActions.contains(actionId);
        }
    }
}
