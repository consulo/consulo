// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.ide.actions.runAnything;

import consulo.application.Application;
import consulo.application.dumb.DumbAware;
import consulo.application.statistic.FeatureUsageTracker;
import consulo.application.util.SystemInfo;
import consulo.application.util.registry.Registry;
import consulo.execution.executor.Executor;
import consulo.ide.IdeBundle;
import consulo.ide.impl.idea.ide.IdeEventQueue;
import consulo.ide.impl.idea.ide.actions.GotoActionBase;
import consulo.ide.impl.idea.ide.actions.runAnything.activity.RunAnythingProvider;
import consulo.ui.ex.awt.action.CustomComponentAction;
import consulo.ide.impl.idea.openapi.actionSystem.impl.ActionButton;
import consulo.ide.impl.idea.openapi.keymap.impl.ModifierKeyDoubleClickHandler;
import consulo.ide.impl.idea.openapi.util.text.StringUtil;
import consulo.language.editor.CommonDataKeys;
import consulo.ui.ex.action.*;
import consulo.ui.ex.action.util.MacKeymapUtil;
import consulo.ui.ex.awt.FontUtil;
import consulo.util.dataholder.Key;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

  public RunAnythingAction(@Nonnull Application application) {
    if(application.isSwingApplication()) {
      IdeEventQueue.getInstance().addPostprocessor(event -> {
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
  }

  @Override
  public void actionPerformed(@Nonnull AnActionEvent e) {
    if (Registry.is("ide.suppress.double.click.handler") && e.getInputEvent() instanceof KeyEvent) {
      if (((KeyEvent)e.getInputEvent()).getKeyCode() == KeyEvent.VK_CONTROL) {
        return;
      }
    }

    if (e.getData(CommonDataKeys.PROJECT) != null) {
      FeatureUsageTracker.getInstance().triggerFeatureUsed(IdeActions.ACTION_RUN_ANYTHING);

      RunAnythingManager runAnythingManager = RunAnythingManager.getInstance(e.getData(CommonDataKeys.PROJECT));
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
    return new ActionButton(this, presentation, place, ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE) {

      @Nullable
      @Override
      protected String getShortcutText() {
        if (myIsDoubleCtrlRegistered) {
          return IdeBundle.message("run.anything.double.ctrl.shortcut", SystemInfo.isMac ? FontUtil.thinSpace() + MacKeymapUtil.CONTROL : "Ctrl");
        }
        //keymap shortcut is added automatically
        return null;
      }

      @Override
      public void setToolTipText(String s) {
        String shortcutText = getShortcutText();
        super.setToolTipText(StringUtil.isNotEmpty(shortcutText) ? (s + " (" + shortcutText + ")") : s);
      }
    };
  }
}