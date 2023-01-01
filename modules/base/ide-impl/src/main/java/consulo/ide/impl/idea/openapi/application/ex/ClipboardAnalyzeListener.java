// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.application.ex;

import consulo.application.ApplicationManager;
import consulo.application.util.Patches;
import consulo.project.Project;
import consulo.project.ui.wm.IdeFrame;
import consulo.project.ui.wm.event.ApplicationActivationListener;
import consulo.ui.ex.awt.util.Alarm;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public abstract class ClipboardAnalyzeListener<T> implements ApplicationActivationListener {
  private static final int MAX_SIZE = 100 * 1024;
  @Nullable
  private String myCachedClipboardValue;

  @Override
  public void applicationActivated(@Nonnull final IdeFrame ideFrame) {
    final Runnable processClipboard = () -> {
      final String clipboard = ClipboardUtil.getTextInClipboard();
      if (clipboard != null && clipboard.length() < MAX_SIZE && !clipboard.equals(myCachedClipboardValue)) {
        myCachedClipboardValue = clipboard;
        final Project project = ideFrame.getProject();
        if (project != null && !project.isDefault()) {
          T handleValue = canHandle(clipboard);
          if (handleValue != null) {
            handle(project, myCachedClipboardValue, handleValue);
          }
        }
      }
    };

    if (Patches.SLOW_GETTING_CLIPBOARD_CONTENTS) {
      //IDEA's clipboard is synchronized with the system clipboard on frame activation so we need to postpone clipboard processing
      new Alarm().addRequest(processClipboard, 300);
    }
    else {
      processClipboard.run();
    }
  }

  protected abstract void handle(@Nonnull Project project, @Nonnull String value, @Nonnull T handleValue);

  @Override
  public void applicationDeactivated(@Nonnull IdeFrame ideFrame) {
    if (!ApplicationManager.getApplication().isDisposed()) {
      myCachedClipboardValue = ClipboardUtil.getTextInClipboard();
    }
  }

  /**
   * Return value of handling. If can handle value will not null
   */
  @Nullable
  public abstract T canHandle(@Nonnull String value);
}
