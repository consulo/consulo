// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything;

import consulo.annotation.component.ActionImpl;
import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.execution.executor.Executor;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.ide.actions.GotoActionBase;
import consulo.ide.impl.idea.openapi.keymap.impl.ModifierKeyDoubleClickHandler;
import consulo.ide.localize.IdeLocalize;
import consulo.ide.runAnything.RunAnythingProvider;
import consulo.localize.LocalizeValue;
import consulo.platform.Platform;
import consulo.platform.base.icon.PlatformIconGroup;
import consulo.platform.base.localize.ActionLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.AnAction;
import consulo.ui.ex.action.AnActionEvent;
import consulo.ui.ex.action.IdeActions;
import consulo.ui.ex.action.util.MacKeymapUtil;
import consulo.ui.ex.awt.FontUtil;
import consulo.ui.ex.awt.internal.IdeEventQueueProxy;
import consulo.ui.ex.internal.CustomShortcutBuilder;
import consulo.ui.ex.internal.KeyMapSetting;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;

import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import static consulo.ide.impl.idea.openapi.keymap.KeymapUtil.getActiveKeymapShortcuts;

@ActionImpl(id = IdeActions.ACTION_RUN_ANYTHING)
public class RunAnythingAction extends AnAction implements DumbAware {
    public static final String RUN_ANYTHING_ACTION_ID = "RunAnything";
    public static final Key<Executor> EXECUTOR_KEY = Key.create("EXECUTOR_KEY");
    public static final AtomicBoolean SHIFT_IS_PRESSED = new AtomicBoolean(false);
    public static final AtomicBoolean ALT_IS_PRESSED = new AtomicBoolean(false);

    private boolean myIsDoubleCtrlRegistered;
    private final Application myApplication;
    @Nonnull
    private final KeyMapSetting myKeyMapSetting;

    @Inject
    public RunAnythingAction(@Nonnull Application application,
                             @Nonnull IdeEventQueueProxy ideEventQueueProxy,
                             @Nonnull KeyMapSetting keyMapSetting) {
        super(
            ActionLocalize.actionRunanythingText(),
            ActionLocalize.actionRunanythingDescription(),
            PlatformIconGroup.actionsRun_anything()
        );
        myApplication = application;
        myKeyMapSetting = keyMapSetting;
        ideEventQueueProxy.addPostprocessor(
            event -> {
                if (event instanceof KeyEvent keyEvent) {
                    int keyCode = keyEvent.getKeyCode();
                    if (keyCode == KeyEvent.VK_SHIFT) {
                        SHIFT_IS_PRESSED.set(keyEvent.getID() == KeyEvent.KEY_PRESSED);
                    }
                    else if (keyCode == KeyEvent.VK_ALT) {
                        ALT_IS_PRESSED.set(keyEvent.getID() == KeyEvent.KEY_PRESSED);
                    }
                }
                return false;
            },
            null
        );
    }

    @Override
    @RequiredUIAccess
    public void actionPerformed(@Nonnull AnActionEvent e) {
        if (!myKeyMapSetting.isEnabledDoublePressShortcuts()
            && e.getInputEvent() instanceof KeyEvent keyEvent
            && keyEvent.getKeyCode() == KeyEvent.VK_CONTROL) {
            return;
        }

        Project project = e.getData(Project.KEY);
        if (project != null) {
            FeatureUsageTracker.getInstance().triggerFeatureUsed(IdeActions.ACTION_RUN_ANYTHING);

            RunAnythingManager runAnythingManager = RunAnythingManager.getInstance(project);
            String text = GotoActionBase.getInitialTextForNavigation(e);
            runAnythingManager.show(text, e);
        }
    }

    @Override
    @RequiredUIAccess
    public void update(@Nonnull AnActionEvent e) {
        e.getPresentation().putClientProperty(
            CustomShortcutBuilder.KEY,
            () -> {
                if (myIsDoubleCtrlRegistered) {
                    return IdeLocalize.runAnythingDoubleCtrlShortcut(
                        Platform.current().os().isMac() ? FontUtil.thinSpace() + MacKeymapUtil.CONTROL : "Ctrl"
                    );
                }
                //keymap shortcut is added automatically
                return LocalizeValue.of();
            }
        );

        if (getActiveKeymapShortcuts(RUN_ANYTHING_ACTION_ID).getShortcuts().length == 0) {
            if (!myIsDoubleCtrlRegistered) {
                ModifierKeyDoubleClickHandler.getInstance().registerAction(RUN_ANYTHING_ACTION_ID, KeyEvent.VK_CONTROL, -1, false);
                myIsDoubleCtrlRegistered = true;
            }
        }
        else {
            if (myIsDoubleCtrlRegistered) {
                ModifierKeyDoubleClickHandler.getInstance().unregisterAction(RUN_ANYTHING_ACTION_ID);
                myIsDoubleCtrlRegistered = false;
            }
        }

        e.getPresentation().setEnabledAndVisible(myApplication.getExtensionPoint(RunAnythingProvider.class).hasAnyExtensions());
    }
}