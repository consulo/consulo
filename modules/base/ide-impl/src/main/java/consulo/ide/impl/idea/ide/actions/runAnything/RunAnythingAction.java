// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything;

import consulo.application.dumb.DumbAware;
import consulo.application.util.registry.Registry;
import consulo.execution.executor.Executor;
import consulo.externalService.statistic.FeatureUsageTracker;
import consulo.ide.impl.idea.ide.actions.GotoActionBase;
import consulo.ide.impl.idea.ide.actions.runAnything.activity.RunAnythingProvider;
import consulo.ide.impl.idea.openapi.keymap.impl.ModifierKeyDoubleClickHandler;
import consulo.ide.impl.ui.IdeEventQueueProxy;
import consulo.platform.Platform;
import consulo.platform.base.localize.IdeLocalize;
import consulo.project.Project;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.util.MacKeymapUtil;
import consulo.ui.ex.awt.FontUtil;
import consulo.ui.ex.awt.action.CustomComponentAction;
import consulo.util.dataholder.Key;
import jakarta.annotation.Nonnull;

import javax.swing.*;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import static consulo.ide.impl.idea.openapi.keymap.KeymapUtil.getActiveKeymapShortcuts;

public class RunAnythingAction extends AnAction implements CustomComponentAction, DumbAware {
  public static final String RUN_ANYTHING_ACTION_ID = "RunAnything";
  public static final Key<Executor> EXECUTOR_KEY = Key.create("EXECUTOR_KEY");
  public static final AtomicBoolean SHIFT_IS_PRESSED = new AtomicBoolean(false);
  public static final AtomicBoolean ALT_IS_PRESSED = new AtomicBoolean(false);

  private boolean myIsDoubleCtrlRegistered;

  public RunAnythingAction(@Nonnull IdeEventQueueProxy ideEventQueueProxy) {
    ideEventQueueProxy.addPostprocessor(event -> {
      if (event instanceof KeyEvent) {
        final int keyCode = ((KeyEvent)event).getKeyCode();
        if (keyCode == KeyEvent.VK_SHIFT) {
          SHIFT_IS_PRESSED.set(event.getID() == KeyEvent.KEY_PRESSED);
        }
        else if (keyCode == KeyEvent.VK_ALT) {
          ALT_IS_PRESSED.set(event.getID() == KeyEvent.KEY_PRESSED);
        }
      }
      return false;
    }, null);
  }

  @RequiredUIAccess
  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    if (Registry.is("ide.suppress.double.click.handler") && e.getInputEvent() instanceof KeyEvent) {
      if (((KeyEvent)e.getInputEvent()).getKeyCode() == KeyEvent.VK_CONTROL) {
        return;
      }
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
  public void update(@Nonnull AnActionEvent e) {
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

    e.getPresentation().setEnabledAndVisible(RunAnythingProvider.EP_NAME.hasAnyExtensions());
  }

  @Nonnull
  @Override
  public JComponent createCustomComponent(@Nonnull Presentation presentation, @Nonnull String place) {
    ActionButtonFactory factory = ActionButtonFactory.getInstance();

    ActionButton button = factory.create(this, presentation, place, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE);
    button.setCustomShortcutBuilder(() -> {
      if (myIsDoubleCtrlRegistered) {
        return IdeLocalize.runAnythingDoubleCtrlShortcut(
          Platform.current().os().isMac() ? FontUtil.thinSpace() + MacKeymapUtil.CONTROL : "Ctrl"
        ).get();
      }
      //keymap shortcut is added automatically
      return null;
    });
    return button.getComponent();
  }
}