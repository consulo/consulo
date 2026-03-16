// Copyright 2000-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package consulo.codeEditor.event;

import consulo.codeEditor.Editor;

import java.awt.event.FocusEvent;
import java.util.EventListener;

/**
 * @author max
 */
public interface FocusChangeListener extends EventListener {
  void focusGained(Editor editor);

  default void focusLost(Editor editor) {
  }

  default void focusLost(Editor editor, @SuppressWarnings("unused") FocusEvent event) {
    focusLost(editor);
  }

  default void focusGained(Editor editor, @SuppressWarnings("unused") FocusEvent event) {
    focusGained(editor);
  }
}