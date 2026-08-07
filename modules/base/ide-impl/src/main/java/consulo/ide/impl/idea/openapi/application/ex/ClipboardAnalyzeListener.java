// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.application.ex;

import consulo.application.ApplicationManager;
import consulo.application.util.Patches;
import consulo.project.Project;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.event.ApplicationActivationListener;
import consulo.ui.UIAccess;
import consulo.ui.annotation.RequiredUIAccess;
import consulo.ui.ex.awt.util.Alarm;

import org.jspecify.annotations.Nullable;

public abstract class ClipboardAnalyzeListener<T> implements ApplicationActivationListener {
  private static final int MAX_SIZE = 100 * 1024;
  private @Nullable String myCachedClipboardValue;

  @Override
  @RequiredUIAccess
  public void applicationActivated(IdeFrame ideFrame) {
    UIAccess uiAccess = UIAccess.current();

    Runnable processClipboard = () ->
      ClipboardUtil.getTextInClipboard()
        .whenCompleteAsync((clipboard, throwable) -> {
          if (throwable == null) {
            processClipboard(ideFrame, clipboard);
          }
        }, uiAccess);

    if (Patches.SLOW_GETTING_CLIPBOARD_CONTENTS) {
      //IDEA's clipboard is synchronized with the system clipboard on frame activation so we need to postpone clipboard processing
      new Alarm().addRequest(processClipboard, 300);
    }
    else {
      processClipboard.run();
    }
  }

  @RequiredUIAccess
  private void processClipboard(IdeFrame ideFrame, @Nullable String clipboard) {
    if (clipboard != null && clipboard.length() < MAX_SIZE && !clipboard.equals(myCachedClipboardValue)) {
      myCachedClipboardValue = clipboard;
      Project project = ideFrame.getProject();
      if (project != null && !project.isDefault()) {
        T handleValue = canHandle(clipboard);
        if (handleValue != null) {
          handle(project, myCachedClipboardValue, handleValue);
        }
      }
    }
  }

  protected abstract void handle(Project project, String value, T handleValue);

  @Override
  @RequiredUIAccess
  public void applicationDeactivated(IdeFrame ideFrame) {
    if (!ApplicationManager.getApplication().isDisposed()) {
      UIAccess uiAccess = UIAccess.current();

      ClipboardUtil.getTextInClipboard()
        .whenCompleteAsync((value, throwable) -> {
          if (throwable == null) {
            myCachedClipboardValue = value;
          }
        }, uiAccess);
    }
  }

  /**
   * Return value of handling. If can handle value will not null
   */
  public abstract @Nullable T canHandle(String value);
}
