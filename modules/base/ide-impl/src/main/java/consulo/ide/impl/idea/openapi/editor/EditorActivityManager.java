// Copyright 2000-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.ide.impl.idea.openapi.editor;

import consulo.application.ApplicationManager;
import consulo.codeEditor.Editor;

import jakarta.annotation.Nonnull;

/**
 * A way to abstract various properties of an editor (as being visible to a user) away from Swing.
 * Useful in scenarios where an application is headless
 * or the editor is not physically visible but we want it to treated as if it's (a guest session during collaborative development)
 */
public class EditorActivityManager {
  private static final EditorActivityManager ourInstance = new EditorActivityManager();

  public static EditorActivityManager getInstance() {
    return ourInstance;
  }

  /**
   * Determines whether an editor is visible to a user
   */
  public boolean isVisible(@Nonnull Editor editor) {
    return ApplicationManager.getApplication().isHeadlessEnvironment() || editor.getContentComponent().isShowing();
  }

  /**
   * Determines whether an editor has focus
   */
  public boolean isFocused(@Nonnull Editor editor) {
    return ApplicationManager.getApplication().isHeadlessEnvironment() || editor.getContentComponent().hasFocus();
  }
}
